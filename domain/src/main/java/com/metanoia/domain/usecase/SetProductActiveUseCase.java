package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Realiza la baja lógica o reactivación de un producto preservando intacto su historial.
 */
public final class SetProductActiveUseCase {

    private final StockRepository stockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SetProductActiveUseCase(
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

    public Result<Product> execute(String productId, boolean active) {
        if (productId == null || productId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de producto inválido");
        }
        Optional<Product> found = stockRepository.findProductById(productId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Producto no encontrado");
        }
        Product current = found.get();
        if (current.active() == active) {
            return Result.success(current);
        }
        long now = clock.nowMillis();
        String operator = settingsRepository.getSettings().currentOperator();
        Product updated = current.toBuilder()
                .active(active)
                .updatedAt(now)
                .build();
        stockRepository.saveProduct(updated);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(operator)
                .action(active ? "PRODUCT_ACTIVATE" : "PRODUCT_DEACTIVATE")
                .entityType("PRODUCT")
                .entityId(updated.id())
                .concept((active ? "Reactivación: " : "Baja lógica: ") + updated.name())
                .build());
        return Result.success(updated);
    }
}
