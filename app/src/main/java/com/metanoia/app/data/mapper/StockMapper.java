package com.metanoia.app.data.mapper;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.data.db.entity.StockMovementEntity;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;

/**
 * Conversión entre entidades Room de stock y objetos inmutables del dominio.
 */
public final class StockMapper {

    private StockMapper() {
    }

    @NonNull
    public static Product toDomain(@NonNull ProductEntity entity) {
        return new Product.Builder()
                .id(entity.id)
                .name(entity.name)
                .nameNormalized(entity.nameNormalized)
                .brand(entity.brand)
                .type(ProductType.valueOf(entity.type))
                .unit(ProductUnit.valueOf(entity.unit))
                .salePrice(entity.salePriceCents == null ? null : Money.ofCents(entity.salePriceCents))
                .minStock(entity.minStock)
                .currentStock(entity.currentStock)
                .active(entity.active)
                .createdAt(entity.createdAt)
                .updatedAt(entity.updatedAt)
                .build();
    }

    @NonNull
    public static ProductEntity toEntity(@NonNull Product model) {
        return new ProductEntity(
                model.id(),
                model.name(),
                model.nameNormalized(),
                model.brand().orElse(null),
                model.type().name(),
                model.unit().name(),
                model.salePrice().map(Money::cents).orElse(null),
                model.minStock(),
                model.currentStock(),
                model.active(),
                model.createdAt(),
                model.updatedAt()
        );
    }

    @NonNull
    public static StockMovement toDomain(@NonNull StockMovementEntity entity) {
        return new StockMovement.Builder()
                .id(entity.id)
                .productId(entity.productId)
                .timestamp(entity.timestamp)
                .type(StockMovementType.valueOf(entity.type))
                .quantity(entity.quantity)
                .reason(entity.reason)
                .cashMovementId(entity.cashMovementId)
                .operator(entity.operator)
                .build();
    }

    @NonNull
    public static StockMovementEntity toEntity(@NonNull StockMovement model) {
        return new StockMovementEntity(
                model.id(),
                model.productId(),
                model.timestamp(),
                model.type().name(),
                model.quantity(),
                model.reason().orElse(null),
                model.cashMovementId().orElse(null),
                model.operator()
        );
    }
}
