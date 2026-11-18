package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.CashClosingCalculator;
import com.metanoia.domain.rules.CashClosingCalculator.ClosingPreview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ejecuta el cierre diario de caja: calcula esperados vs contados, registra la diferencia,
 * vincula los movimientos abiertos del día y bloquea futuras modificaciones.
 */
public final class CloseCashDayUseCase {

    public static final class Request {
        private final long dayEpoch;
        private final Map<PaymentMethod, Money> countedByMethod;
        private final String notes;

        public Request(
                long dayEpoch,
                Map<PaymentMethod, Money> countedByMethod,
                String notes
        ) {
            this.dayEpoch = dayEpoch;
            this.countedByMethod = countedByMethod;
            this.notes = notes;
        }
    }

    private final CashRepository cashRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CloseCashDayUseCase(
            CashRepository cashRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.cashRepository = Objects.requireNonNull(cashRepository, "cashRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<CashClosing> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud de cierre inválida");
        }
        if (request.countedByMethod != null) {
            for (Money value : request.countedByMethod.values()) {
                if (value != null && value.isNegative()) {
                    return Result.failure(DomainError.VALIDATION_ERROR,
                            "Los importes contados no pueden ser negativos");
                }
            }
        }
        if (cashRepository.isDayClosed(request.dayEpoch)) {
            return Result.failure(DomainError.CONFLICT, "La caja de este día ya fue cerrada");
        }

        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        String closingId = idGenerator.nextId();
        String cleanNotes = request.notes == null ? "" : request.notes.trim();

        AtomicReference<Result<CashClosing>> outcome = new AtomicReference<>();
        cashRepository.runInTransaction(() -> {
            if (cashRepository.isDayClosed(request.dayEpoch)) {
                outcome.set(Result.failure(
                        DomainError.CONFLICT, "La caja de este día ya fue cerrada"));
                return;
            }
            List<CashMovement> dayMovements = cashRepository.findMovementsByDay(request.dayEpoch);
            Map<String, CashMovementType> externalOriginalTypes =
                    resolveExternalOriginalTypes(dayMovements);
            ClosingPreview preview = CashClosingCalculator.preview(
                    dayMovements, externalOriginalTypes, request.countedByMethod);

            CashClosing closing = new CashClosing.Builder()
                    .id(closingId)
                    .day(request.dayEpoch)
                    .expectedByMethod(preview.expectedByMethod())
                    .countedByMethod(preview.countedByMethod())
                    .difference(preview.totalDifference())
                    .notes(cleanNotes)
                    .closedAt(now)
                    .operator(operator)
                    .build();
            cashRepository.saveClosingAndLockDay(closing);

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("CASH_CLOSE")
                    .entityType("CASH_CLOSING")
                    .entityId(closing.id())
                    .amountCents(closing.totalCounted().cents())
                    .concept("Cierre diario · Dif " + closing.difference().cents())
                    .build());

            outcome.set(Result.success(closing));
        });

        return outcome.get();
    }

    private Map<String, CashMovementType> resolveExternalOriginalTypes(
            List<CashMovement> dayMovements) {
        Map<String, CashMovementType> known = new HashMap<>();
        for (CashMovement m : dayMovements) {
            known.put(m.id(), m.type());
        }
        Map<String, CashMovementType> external = new HashMap<>();
        for (CashMovement m : dayMovements) {
            if (m.type() == CashMovementType.REVERSAL && m.reversesMovementId().isPresent()) {
                String targetId = m.reversesMovementId().get();
                if (!known.containsKey(targetId)) {
                    Optional<CashMovement> target = cashRepository.findMovementById(targetId);
                    target.ifPresent(found -> external.put(found.id(), found.type()));
                }
            }
        }
        return external;
    }
}
