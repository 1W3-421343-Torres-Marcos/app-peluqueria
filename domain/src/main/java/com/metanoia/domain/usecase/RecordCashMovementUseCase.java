package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registra movimientos sueltos de caja del día actual: venta suelta ({@link CashMovementType#PRODUCT_SALE}),
 * otro ingreso ({@link CashMovementType#OTHER_INCOME}) o gasto ({@link CashMovementType#EXPENSE}).
 */
public final class RecordCashMovementUseCase {

    private static final int MIN_CONCEPT_LENGTH = 2;

    public static final class Request {
        private final CashMovementType type;
        private final PaymentMethod method;
        private final Money amount;
        private final String concept;

        public Request(
                CashMovementType type,
                PaymentMethod method,
                Money amount,
                String concept
        ) {
            this.type = type;
            this.method = method;
            this.amount = amount;
            this.concept = concept;
        }
    }

    private final CashRepository cashRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public RecordCashMovementUseCase(
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

    public Result<CashMovement> execute(Request request) {
        if (request == null || !isSupportedType(request.type) || request.method == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Tipo o medio de movimiento inválido");
        }
        if (request.amount == null || !request.amount.isPositive()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "El importe debe ser mayor a cero");
        }
        String cleanConcept = request.concept == null ? "" : request.concept.trim();
        if (cleanConcept.length() < MIN_CONCEPT_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Indicá un concepto de al menos 2 caracteres");
        }

        long today = clock.todayEpochDay();
        if (cashRepository.isDayClosed(today)) {
            return Result.failure(DomainError.CONFLICT,
                    "La caja del día actual ya está cerrada; no admite nuevos movimientos");
        }

        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        String movementId = idGenerator.nextId();

        AtomicReference<Result<CashMovement>> outcome = new AtomicReference<>();
        cashRepository.runInTransaction(() -> {
            if (cashRepository.isDayClosed(today)) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "La caja del día actual ya está cerrada"));
                return;
            }
            CashMovement movement = new CashMovement.Builder()
                    .id(movementId)
                    .day(today)
                    .timestamp(now)
                    .type(request.type)
                    .method(request.method)
                    .amount(request.amount)
                    .concept(cleanConcept)
                    .operator(operator)
                    .build();
            cashRepository.saveMovement(movement);

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("CASH_" + request.type.name())
                    .entityType("CASH_MOVEMENT")
                    .entityId(movement.id())
                    .amountCents(request.amount.cents())
                    .concept(cleanConcept)
                    .build());

            outcome.set(Result.success(movement));
        });

        return outcome.get();
    }

    private static boolean isSupportedType(CashMovementType type) {
        return type == CashMovementType.PRODUCT_SALE
                || type == CashMovementType.OTHER_INCOME
                || type == CashMovementType.EXPENSE;
    }
}
