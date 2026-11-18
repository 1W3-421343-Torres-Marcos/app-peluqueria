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
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.AppointmentPaymentCalculator;
import com.metanoia.domain.rules.StatusTransitions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Revierte un movimiento inmutable mediante un {@link CashMovementType#REVERSAL} y actualiza el
 * estado del turno ({@code PAID -> COMPLETED}) si el reverso vuelve a dejar saldo pendiente.
 */
public final class ReverseMovementUseCase {

    private static final int MIN_REASON_LENGTH = 2;

    private final CashRepository cashRepository;
    private final AppointmentRepository appointmentRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ReverseMovementUseCase(
            CashRepository cashRepository,
            AppointmentRepository appointmentRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.cashRepository = Objects.requireNonNull(cashRepository, "cashRepository");
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<CashMovement> execute(String movementId, String reason) {
        if (movementId == null || movementId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Movimiento inválido");
        }
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() < MIN_REASON_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Indicá un motivo válido para el reverso");
        }

        Optional<CashMovement> found = cashRepository.findMovementById(movementId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Movimiento no encontrado");
        }
        CashMovement original = found.get();
        if (original.type() == CashMovementType.REVERSAL) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "No se puede revertir un movimiento de reverso");
        }
        if (cashRepository.existsReversalForMovement(original.id())) {
            return Result.failure(DomainError.CONFLICT, "Este movimiento ya fue revertido");
        }

        long today = clock.todayEpochDay();
        long targetDay = cashRepository.isDayClosed(original.day()) ? today : original.day();
        if (cashRepository.isDayClosed(targetDay)) {
            return Result.failure(DomainError.CONFLICT,
                    "La caja del día actual ya está cerrada; no admite reversos");
        }

        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        String reversalId = idGenerator.nextId();
        String concept = "Reverso (" + cleanReason + "): " + original.concept();

        AtomicReference<Result<CashMovement>> outcome = new AtomicReference<>();
        cashRepository.runInTransaction(() -> {
            if (cashRepository.existsReversalForMovement(original.id())) {
                outcome.set(Result.failure(
                        DomainError.CONFLICT, "Este movimiento ya fue revertido"));
                return;
            }
            if (cashRepository.isDayClosed(targetDay)) {
                outcome.set(Result.failure(
                        DomainError.CONFLICT, "La caja del día seleccionado está cerrada"));
                return;
            }

            CashMovement reversal = new CashMovement.Builder()
                    .id(reversalId)
                    .day(targetDay)
                    .timestamp(now)
                    .type(CashMovementType.REVERSAL)
                    .method(original.method())
                    .amount(original.amount())
                    .concept(concept)
                    .appointmentId(original.appointmentId().orElse(null))
                    .clientId(original.clientId().orElse(null))
                    .productId(original.productId().orElse(null))
                    .quantity(original.quantity().orElse(null))
                    .reversesMovementId(original.id())
                    .operator(operator)
                    .build();
            cashRepository.saveMovement(reversal);

            if (original.type() == CashMovementType.APPOINTMENT_PAYMENT
                    && original.appointmentId().isPresent()) {
                String aptId = original.appointmentId().get();
                Optional<Appointment> aptOpt = appointmentRepository.findById(aptId);
                if (aptOpt.isPresent()) {
                    Appointment apt = aptOpt.get();
                    List<CashMovement> movements = cashRepository.findMovementsByAppointmentId(aptId);
                    AppointmentBalance balance = AppointmentPaymentCalculator.balanceOf(
                            apt, movements);
                    AppointmentStatus updatedStatus =
                            StatusTransitions.resolveStatusAfterPaymentChange(
                                    apt.status(), balance);
                    if (updatedStatus != apt.status()) {
                        Appointment updatedApt = apt.toBuilder()
                                .status(updatedStatus)
                                .updatedAt(now)
                                .build();
                        appointmentRepository.save(updatedApt);
                    }
                }
            }

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("CASH_REVERSAL")
                    .entityType("CASH_MOVEMENT")
                    .entityId(reversal.id())
                    .amountCents(original.amount().cents())
                    .concept(concept)
                    .build());

            outcome.set(Result.success(reversal));
        });

        return outcome.get();
    }
}
