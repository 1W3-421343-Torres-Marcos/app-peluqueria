package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.CashPaymentSupport;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;
import com.metanoia.domain.rules.StatusTransitions;
import com.metanoia.domain.usecase.ChargeAppointmentUseCase.ChargeResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ejecuta "Completar y cobrar" sobre turnos {@code PENDING} o {@code CONFIRMED} en una
 * única transacción atómica (congela comisiones + registra movimientos de caja).
 */
public final class CompleteAndChargeUseCase {

    public static final class Request {
        private final String appointmentId;
        private final Money adjustment;
        private final String adjustmentReason;
        private final List<PaymentSplit> splits;

        public Request(
                String appointmentId,
                Money adjustment,
                String adjustmentReason,
                List<PaymentSplit> splits
        ) {
            this.appointmentId = appointmentId;
            this.adjustment = adjustment;
            this.adjustmentReason = adjustmentReason;
            this.splits = splits == null
                    ? Collections.emptyList()
                    : new ArrayList<>(splits);
        }
    }

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final CashRepository cashRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CompleteAndChargeUseCase(
            AppointmentRepository appointmentRepository,
            ProfessionalRepository professionalRepository,
            CashRepository cashRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.cashRepository = Objects.requireNonNull(cashRepository, "cashRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<ChargeResult> execute(Request request) {
        if (request == null || request.appointmentId == null
                || request.appointmentId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Turno inválido");
        }
        Result<Money> splitsResult = CashPaymentSupport.validateAndSumSplits(request.splits);
        if (!splitsResult.isSuccess()) {
            return Result.failure(splitsResult.getError(), splitsResult.getErrorMessage());
        }

        long today = clock.todayEpochDay();
        long now = clock.nowMillis();
        String operator = settingsRepository.getSettings().currentOperator();
        String appointmentId = request.appointmentId.trim();

        AtomicReference<Result<ChargeResult>> outcome = new AtomicReference<>();
        try {
            cashRepository.runInTransaction(() -> {
                if (cashRepository.isDayClosed(today)) {
                    outcome.set(Result.failure(
                            DomainError.CONFLICT, "La caja de hoy ya está cerrada"));
                    return;
                }
                Optional<Appointment> found = appointmentRepository.findById(appointmentId);
                if (!found.isPresent()) {
                    outcome.set(Result.failure(DomainError.NOT_FOUND, "Turno no encontrado"));
                    return;
                }
                Appointment current = found.get();
                if (current.status() != AppointmentStatus.PENDING
                        && current.status() != AppointmentStatus.CONFIRMED) {
                    outcome.set(Result.failure(
                            DomainError.CONFLICT,
                            "Solo se puede completar y cobrar desde Pendiente o Confirmado"));
                    return;
                }
                Optional<Professional> proOpt = professionalRepository.findById(
                        current.professionalId());
                if (!proOpt.isPresent()) {
                    outcome.set(Result.failure(
                            DomainError.NOT_FOUND, "Profesional no encontrado"));
                    return;
                }

                Result<Money> adjustmentResult = CashPaymentSupport.resolveAdjustment(
                        current.adjustment(), request.adjustment, request.adjustmentReason);
                if (!adjustmentResult.isSuccess()) {
                    outcome.set(Result.failure(
                            adjustmentResult.getError(), adjustmentResult.getErrorMessage()));
                    return;
                }
                Money effectiveAdjustment = adjustmentResult.getValue();
                long totalCents =
                        current.itemsSubtotal().cents() + effectiveAdjustment.cents();
                if (totalCents <= 0L) {
                    outcome.set(Result.failure(
                            DomainError.VALIDATION_ERROR,
                            "El total ajustado debe ser mayor a cero"));
                    return;
                }
                Money paidNow = splitsResult.getValue();
                if (paidNow.cents() > totalCents) {
                    outcome.set(Result.failure(
                            DomainError.VALIDATION_ERROR,
                            "El importe a cobrar supera el total del turno"));
                    return;
                }

                List<AppointmentItem> frozenItems = StatusTransitions.freezeCommissions(
                        current.items(), proOpt.get().commissionBasisPoints());
                Appointment completedWithAdjust = current.toBuilder()
                        .items(frozenItems)
                        .adjustment(effectiveAdjustment)
                        .updatedAt(now)
                        .build();

                AppointmentBalance balance =
                        AppointmentBalance.of(completedWithAdjust, paidNow);
                AppointmentStatus finalStatus =
                        StatusTransitions.resolveStatusAfterPaymentChange(
                                AppointmentStatus.COMPLETED, balance);
                Appointment savedAppointment = completedWithAdjust.toBuilder()
                        .status(finalStatus)
                        .build();
                appointmentRepository.save(savedAppointment);

                auditLogRepository.record(new AuditRecord.Builder()
                        .id(idGenerator.nextId())
                        .timestamp(now)
                        .operator(operator)
                        .action("APPOINTMENT_COMPLETE")
                        .entityType("APPOINTMENT")
                        .entityId(appointmentId)
                        .amountCents(balance.total().cents())
                        .concept("Servicio completado al cobrar")
                        .build());

                if (!effectiveAdjustment.equals(current.adjustment())) {
                    auditLogRepository.record(new AuditRecord.Builder()
                            .id(idGenerator.nextId())
                            .timestamp(now)
                            .operator(operator)
                            .action("APPOINTMENT_ADJUSTMENT")
                            .entityType("APPOINTMENT")
                            .entityId(appointmentId)
                            .amountCents(effectiveAdjustment.cents())
                            .concept("Ajuste al cobrar: " + request.adjustmentReason.trim())
                            .build());
                }

                List<CashMovement> movements = CashPaymentSupport.recordAppointmentSplits(
                        savedAppointment,
                        request.splits,
                        today,
                        now,
                        operator,
                        cashRepository,
                        auditLogRepository,
                        idGenerator
                );

                outcome.set(Result.success(
                        new ChargeResult(savedAppointment, movements, balance)));
            });
        } catch (RuntimeException ex) {
            return Result.failure(DomainError.DATABASE_ERROR,
                    "No se pudo completar y cobrar el turno");
        }

        return outcome.get();
    }
}
