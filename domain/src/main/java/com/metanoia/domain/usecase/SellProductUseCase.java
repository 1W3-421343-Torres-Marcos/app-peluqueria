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
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registra en una única transacción atómica una venta suelta en caja ({@link CashMovementType#PRODUCT_SALE})
 * y el correspondiente egreso de inventario ({@link StockMovementType#OUT_SALE}).
 */
public final class SellProductUseCase {

    public static final class Request {
        private final String productId;
        private final int quantity;
        private final PaymentMethod method;
        private final Money totalAmountOverride;
        private final String clientId;

        public Request(
                String productId,
                int quantity,
                PaymentMethod method,
                Money totalAmountOverride,
                String clientId
        ) {
            this.productId = productId;
            this.quantity = quantity;
            this.method = method;
            this.totalAmountOverride = totalAmountOverride;
            this.clientId = clientId;
        }
    }

    public static final class Outcome {
        private final CashMovement cashMovement;
        private final StockMovement stockMovement;
        private final Product updatedProduct;
        private final boolean negativeStockWarning;

        public Outcome(
                CashMovement cashMovement,
                StockMovement stockMovement,
                Product updatedProduct,
                boolean negativeStockWarning
        ) {
            this.cashMovement = Objects.requireNonNull(cashMovement, "cashMovement");
            this.stockMovement = Objects.requireNonNull(stockMovement, "stockMovement");
            this.updatedProduct = Objects.requireNonNull(updatedProduct, "updatedProduct");
            this.negativeStockWarning = negativeStockWarning;
        }

        public CashMovement cashMovement() {
            return cashMovement;
        }

        public StockMovement stockMovement() {
            return stockMovement;
        }

        public Product updatedProduct() {
            return updatedProduct;
        }

        public boolean negativeStockWarning() {
            return negativeStockWarning;
        }
    }

    private final StockRepository stockRepository;
    private final CashRepository cashRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SellProductUseCase(
            StockRepository stockRepository,
            CashRepository cashRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.stockRepository = Objects.requireNonNull(stockRepository, "stockRepository");
        this.cashRepository = Objects.requireNonNull(cashRepository, "cashRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Outcome> execute(Request request) {
        if (request == null
                || request.productId == null
                || request.productId.trim().isEmpty()
                || request.quantity <= 0
                || request.method == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Datos de venta inválidos");
        }
        if (request.totalAmountOverride != null && !request.totalAmountOverride.isPositive()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "El importe debe ser mayor a cero");
        }

        long today = clock.todayEpochDay();
        if (cashRepository.isDayClosed(today)) {
            return Result.failure(DomainError.CONFLICT,
                    "La caja del día actual ya está cerrada; no admite nuevas ventas");
        }

        String productId = request.productId.trim();
        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        String cashMovementId = idGenerator.nextId();
        String stockMovementId = idGenerator.nextId();

        AtomicReference<Result<Outcome>> outcome = new AtomicReference<>();
        cashRepository.runInTransaction(() -> {
            if (cashRepository.isDayClosed(today)) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "La caja del día actual ya está cerrada"));
                return;
            }

            Optional<Product> foundProduct = stockRepository.findProductById(productId);
            if (!foundProduct.isPresent()) {
                outcome.set(Result.failure(DomainError.NOT_FOUND, "Producto no encontrado"));
                return;
            }
            Product product = foundProduct.get();
            if (!product.active()) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "No se puede vender un producto dado de baja"));
                return;
            }
            if (!product.type().isForSale()) {
                outcome.set(Result.failure(DomainError.VALIDATION_ERROR,
                        "Los productos solo de uso interno no están habilitados para la venta"));
                return;
            }

            Money totalAmount = request.totalAmountOverride != null
                    ? request.totalAmountOverride
                    : product.salePrice()
                            .map(unitPrice -> unitPrice.multiply(request.quantity))
                            .orElse(null);
            if (totalAmount == null || !totalAmount.isPositive()) {
                outcome.set(Result.failure(DomainError.VALIDATION_ERROR,
                        "El producto no tiene precio de venta válido"));
                return;
            }

            String concept = "Venta: " + product.name() + " x" + request.quantity;
            CashMovement cashMovement = new CashMovement.Builder()
                    .id(cashMovementId)
                    .day(today)
                    .timestamp(now)
                    .type(CashMovementType.PRODUCT_SALE)
                    .method(request.method)
                    .amount(totalAmount)
                    .concept(concept)
                    .clientId(request.clientId)
                    .productId(product.id())
                    .quantity(request.quantity)
                    .operator(operator)
                    .build();
            cashRepository.saveMovement(cashMovement);

            int previousStock = stockRepository.sumMovementsByProductId(product.id());
            int newStock = Math.subtractExact(previousStock, request.quantity);
            StockMovement stockMovement = new StockMovement.Builder()
                    .id(stockMovementId)
                    .productId(product.id())
                    .timestamp(now)
                    .type(StockMovementType.OUT_SALE)
                    .quantity(-request.quantity)
                    .reason(concept)
                    .cashMovementId(cashMovement.id())
                    .operator(operator)
                    .build();

            Product updatedProduct = product.toBuilder()
                    .currentStock(newStock)
                    .updatedAt(now)
                    .build();
            stockRepository.recordMovementAndUpdateProduct(stockMovement, updatedProduct);

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("CASH_PRODUCT_SALE")
                    .entityType("CASH_MOVEMENT")
                    .entityId(cashMovement.id())
                    .amountCents(totalAmount.cents())
                    .concept(concept)
                    .build());

            outcome.set(Result.success(
                    new Outcome(cashMovement, stockMovement, updatedProduct, newStock < 0)));
        });

        return outcome.get();
    }
}
