package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.AppointmentPaymentCalculator;
import com.metanoia.domain.rules.CashPaymentSupport;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;
import com.metanoia.domain.rules.StatusTransitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registra pagos totales, parciales o divididos por método sobre turnos {@code COMPLETED} con saldo,
 * pasando automáticamente el turno a {@code PAID} al cubrir el total ajustado.
 */
public final class ChargeAppointmentUseCase {

    public static final class ChargeResult {
        private final Appointment appointment;
        private final List<CashMovement> movements;
        private final AppointmentBalance balance;

        public ChargeResult(
                Appointment appointment,
                List<CashMovement> movements,
                AppointmentBalance balance
        ) {
            this.appointment = appointment;
            this.movements = movements;
            this.balance = balance;
        }

        public Appointment appointment() {
            return appointment;
        }

        public List<CashMovement> movements() {
            return movements;
        }

        public AppointmentBalance balance() {
            return balance;
        }
    }

    public static final class Request {
        private final String appointmentId;
        private final Money newAdjustment;
        private final String adjustmentReason;
        private final List<PaymentSplit> splits;

        public Request(
                String appointmentId,
                Money newAdjustment,
                String adjustmentReason,
                List<PaymentSplit> splits
        ) {
            this.appointmentId = appointmentId;
            this.newAdjustment = newAdjustment;
            this.adjustmentReason = adjustmentReason;
            this.splits = splits == null
                    ? Collections.emptyList()
                    : new ArrayList<>(splits);
        }
    }

    private final AppointmentRepository appointmentRepository;
    private final CashRepository cashRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ChargeAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            CashRepository cashRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
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
                if (current.status() != AppointmentStatus.COMPLETED) {
                    outcome.set(Result.failure(
                            DomainError.CONFLICT,
                            "Solo se pueden cobrar turnos en estado COMPLETADO con saldo pendiente"));
                    return;
                }

                Result<Money> adjustmentResult = CashPaymentSupport.resolveAdjustment(
                        current.adjustment(), request.newAdjustment, request.adjustmentReason);
                if (!adjustmentResult.isSuccess()) {
                    outcome.set(Result.failure(
                            adjustmentResult.getError(), adjustmentResult.getErrorMessage()));
                    return;
                }
                Money effectiveAdjustment = adjustmentResult.getValue();
                long newTotalCents =
                        current.itemsSubtotal().cents() + effectiveAdjustment.cents();
                if (newTotalCents <= 0L) {
                    outcome.set(Result.failure(
                            DomainError.VALIDATION_ERROR,
                            "El total ajustado debe ser mayor a cero"));
                    return;
                }

                Money alreadyPaid = AppointmentPaymentCalculator.netPaid(
                        cashRepository.findMovementsByAppointmentId(appointmentId));
                long remainingCents = newTotalCents - alreadyPaid.cents();
                if (newTotalCents < alreadyPaid.cents()) {
                    outcome.set(Result.failure(
                            DomainError.VALIDATION_ERROR,
                            "El ajuste no puede dejar el total por debajo de lo ya pagado"));
                    return;
                }
                if (remainingCents <= 0L) {
                    outcome.set(Result.failure(
                            DomainError.CONFLICT, "El turno no tiene saldo pendiente de cobro"));
                    return;
                }

                long payingCents = splitsResult.getValue().cents();
                if (payingCents > remainingCents) {
                    outcome.set(Result.failure(
                            DomainError.VALIDATION_ERROR,
                            "El importe a cobrar supera el saldo pendiente"));
                    return;
                }

                Money updatedNetPaid = alreadyPaid.plus(splitsResult.getValue());
                Appointment adjustedApt = current.toBuilder()
                        .adjustment(effectiveAdjustment)
                        .updatedAt(now)
                        .build();
                AppointmentBalance updatedBalance =
                        AppointmentBalance.of(adjustedApt, updatedNetPaid);
                AppointmentStatus nextStatus =
                        StatusTransitions.resolveStatusAfterPaymentChange(
                                AppointmentStatus.COMPLETED, updatedBalance);

                Appointment savedAppointment = adjustedApt.toBuilder()
                        .status(nextStatus)
                        .build();
                appointmentRepository.save(savedAppointment);

                if (!effectiveAdjustment.equals(current.adjustment())) {
                    auditLogRepository.record(new AuditRecord.Builder()
                            .id(idGenerator.nextId())
                            .timestamp(now)
                            .operator(operator)
                            .action("APPOINTMENT_ADJUSTMENT")
                            .entityType("APPOINTMENT")
                            .entityId(appointmentId)
                            .amountCents(effectiveAdjustment.cents())
                            .concept("Ajuste en cobro: " + request.adjustmentReason.trim())
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

                outcome.set(Result.success(new ChargeResult(
                        savedAppointment, movements, updatedBalance)));
            });
        } catch (RuntimeException ex) {
            return Result.failure(DomainError.DATABASE_ERROR,
                    "No se pudo guardar el cobro del turno");
        }

        return outcome.get();
    }
}
