package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registra movimientos inmutables de ingreso, egreso por uso interno o ajuste auditado,
 * recalculando transaccionalmente el stock desde la suma histórica de movimientos.
 */
public final class RecordStockMovementUseCase {

    private static final int MIN_ADJUSTMENT_REASON_LENGTH = 2;

    public static final class Request {
        private final String productId;
        private final StockMovementType type;
        private final int signedQuantity;
        private final String reason;

        public Request(
                String productId,
                StockMovementType type,
                int signedQuantity,
                String reason
        ) {
            this.productId = productId;
            this.type = type;
            this.signedQuantity = signedQuantity;
            this.reason = reason;
        }

        public static Request stockIn(String productId, int quantity, String reason) {
            return new Request(productId, StockMovementType.IN,
                    quantity > 0 ? quantity : 0, reason);
        }

        public static Request stockOutUse(String productId, int quantity, String reason) {
            return new Request(productId, StockMovementType.OUT_USE,
                    quantity > 0 ? -quantity : 0, reason);
        }

        public static Request adjustment(String productId, int signedDelta, String reason) {
            return new Request(productId, StockMovementType.ADJUSTMENT, signedDelta, reason);
        }
    }

    public static final class Outcome {
        private final StockMovement movement;
        private final Product updatedProduct;
        private final boolean negativeStockWarning;

        public Outcome(
                StockMovement movement,
                Product updatedProduct,
                boolean negativeStockWarning
        ) {
            this.movement = Objects.requireNonNull(movement, "movement");
            this.updatedProduct = Objects.requireNonNull(updatedProduct, "updatedProduct");
            this.negativeStockWarning = negativeStockWarning;
        }

        public StockMovement movement() {
            return movement;
        }

        public Product updatedProduct() {
            return updatedProduct;
        }

        public boolean negativeStockWarning() {
            return negativeStockWarning;
        }
    }

    private final StockRepository stockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public RecordStockMovementUseCase(
            StockRepository stockRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.stockRepository = Objects.requireNonNull(stockRepository, "stockRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Outcome> execute(Request request) {
        if (request == null || request.productId == null || request.productId.trim().isEmpty()
                || request.type == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Datos del movimiento inválidos");
        }
        if (request.type == StockMovementType.OUT_SALE) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Las ventas de productos deben registrarse desde Caja");
        }
        if (request.signedQuantity == 0) {
            return Result.failure(DomainError.VALIDATION_ERROR, "La cantidad debe ser distinta de cero");
        }
        if (request.type == StockMovementType.IN && request.signedQuantity < 0) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Un ingreso requiere cantidad positiva");
        }
        if (request.type == StockMovementType.OUT_USE && request.signedQuantity > 0) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Un egreso por uso requiere cantidad negativa");
        }
        String cleanReason = request.reason == null ? null : request.reason.trim();
        if (cleanReason != null && cleanReason.isEmpty()) {
            cleanReason = null;
        }
        if (request.type == StockMovementType.ADJUSTMENT
                && (cleanReason == null || cleanReason.length() < MIN_ADJUSTMENT_REASON_LENGTH)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Un ajuste requiere indicar un motivo de al menos 2 caracteres");
        }

        String productId = request.productId.trim();
        String finalReason = cleanReason;
        long now = clock.nowMillis();
        String operator = settingsRepository.getSettings().currentOperator();
        String movementId = idGenerator.nextId();

        AtomicReference<Result<Outcome>> outcome = new AtomicReference<>();
        stockRepository.runInTransaction(() -> {
            Optional<Product> found = stockRepository.findProductById(productId);
            if (!found.isPresent()) {
                outcome.set(Result.failure(DomainError.NOT_FOUND, "Producto no encontrado"));
                return;
            }
            Product product = found.get();
            if (!product.active()) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "No se pueden registrar movimientos en un producto dado de baja"));
                return;
            }

            int previousSum = stockRepository.sumMovementsByProductId(product.id());
            int newStock = Math.addExact(previousSum, request.signedQuantity);

            StockMovement movement = new StockMovement.Builder()
                    .id(movementId)
                    .productId(product.id())
                    .timestamp(now)
                    .type(request.type)
                    .quantity(request.signedQuantity)
                    .reason(finalReason)
                    .operator(operator)
                    .build();

            Product updatedProduct = product.toBuilder()
                    .currentStock(newStock)
                    .updatedAt(now)
                    .build();

            stockRepository.recordMovementAndUpdateProduct(movement, updatedProduct);

            String signedLabel = request.signedQuantity > 0
                    ? "+" + request.signedQuantity
                    : String.valueOf(request.signedQuantity);
            String concept = finalReason == null
                    ? product.name() + " (" + signedLabel + ")"
                    : product.name() + " (" + signedLabel + "): " + finalReason;

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("STOCK_" + request.type.name())
                    .entityType("STOCK_MOVEMENT")
                    .entityId(movement.id())
                    .concept(concept)
                    .build());

            outcome.set(Result.success(new Outcome(movement, updatedProduct, newStock < 0)));
        });

        return outcome.get();
    }
}
