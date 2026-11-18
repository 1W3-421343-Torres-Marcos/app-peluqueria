package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.rules.TextNormalizer;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Valida y registra el alta o edición de un producto de stock junto con su stock inicial opcional.
 */
public final class SaveProductUseCase {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_BRAND_LENGTH = 60;

    public static final class Request {
        private final String existingId;
        private final String name;
        private final String brand;
        private final ProductType type;
        private final ProductUnit unit;
        private final Money salePrice;
        private final int minStock;
        private final int initialStock;

        public Request(
                String existingId,
                String name,
                String brand,
                ProductType type,
                ProductUnit unit,
                Money salePrice,
                int minStock,
                int initialStock
        ) {
            this.existingId = existingId;
            this.name = name;
            this.brand = brand;
            this.type = type;
            this.unit = unit;
            this.salePrice = salePrice;
            this.minStock = minStock;
            this.initialStock = initialStock;
        }
    }

    private final StockRepository stockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SaveProductUseCase(
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

    public Result<Product> execute(Request request) {
        if (request == null || request.type == null || request.unit == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Datos del producto inválidos");
        }
        String cleanName = request.name == null ? "" : request.name.trim();
        if (cleanName.length() < MIN_NAME_LENGTH || cleanName.length() > MAX_NAME_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El nombre debe tener entre 2 y 80 caracteres");
        }
        String cleanBrand = request.brand == null ? null : request.brand.trim();
        if (cleanBrand != null && cleanBrand.isEmpty()) {
            cleanBrand = null;
        }
        if (cleanBrand != null && cleanBrand.length() > MAX_BRAND_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La marca no puede superar 60 caracteres");
        }
        if (request.minStock < 0) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El stock mínimo no puede ser negativo");
        }
        boolean isEdit = request.existingId != null && !request.existingId.trim().isEmpty();
        if (!isEdit && request.initialStock < 0) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El stock inicial no puede ser negativo");
        }
        Money effectivePrice = request.type.isForSale() ? request.salePrice : null;
        if (request.type.isForSale() && (effectivePrice == null || !effectivePrice.isPositive())) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Indicá un precio de venta mayor a cero");
        }

        long now = clock.nowMillis();
        String operator = settingsRepository.getSettings().currentOperator();

        if (isEdit) {
            Optional<Product> found = stockRepository.findProductById(request.existingId.trim());
            if (!found.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND, "Producto no encontrado");
            }
            Product existing = found.get();
            String finalBrand = cleanBrand;
            AtomicReference<Result<Product>> outcome = new AtomicReference<>();
            stockRepository.runInTransaction(() -> {
                Product updated = existing.toBuilder()
                        .name(cleanName)
                        .nameNormalized(TextNormalizer.normalize(cleanName))
                        .brand(finalBrand)
                        .type(request.type)
                        .unit(request.unit)
                        .salePrice(effectivePrice)
                        .minStock(request.minStock)
                        .updatedAt(now)
                        .build();
                stockRepository.saveProduct(updated);
                auditLogRepository.record(new AuditRecord.Builder()
                        .id(idGenerator.nextId())
                        .timestamp(now)
                        .operator(operator)
                        .action("PRODUCT_UPDATE")
                        .entityType("PRODUCT")
                        .entityId(updated.id())
                        .amountCents(effectivePrice == null ? null : effectivePrice.cents())
                        .concept(cleanName)
                        .build());
                outcome.set(Result.success(updated));
            });
            return outcome.get();
        }

        String productId = idGenerator.nextId();
        String finalBrand = cleanBrand;
        AtomicReference<Result<Product>> outcome = new AtomicReference<>();
        stockRepository.runInTransaction(() -> {
            Product created = new Product.Builder()
                    .id(productId)
                    .name(cleanName)
                    .nameNormalized(TextNormalizer.normalize(cleanName))
                    .brand(finalBrand)
                    .type(request.type)
                    .unit(request.unit)
                    .salePrice(effectivePrice)
                    .minStock(request.minStock)
                    .currentStock(request.initialStock)
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            stockRepository.saveProduct(created);

            if (request.initialStock > 0) {
                StockMovement initialMovement = new StockMovement.Builder()
                        .id(idGenerator.nextId())
                        .productId(productId)
                        .timestamp(now)
                        .type(StockMovementType.IN)
                        .quantity(request.initialStock)
                        .reason("Stock inicial")
                        .operator(operator)
                        .build();
                stockRepository.saveMovement(initialMovement);
            }

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("PRODUCT_CREATE")
                    .entityType("PRODUCT")
                    .entityId(created.id())
                    .amountCents(effectivePrice == null ? null : effectivePrice.cents())
                    .concept(cleanName)
                    .build());

            outcome.set(Result.success(created));
        });

        return outcome.get();
    }
}
