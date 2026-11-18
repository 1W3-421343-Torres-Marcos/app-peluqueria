package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para productos de uso interno y/o venta con stock cacheado.
 */
@Entity(
        tableName = "products",
        indices = {
                @Index(value = {"active", "type", "name_normalized"}),
                @Index(value = {"active", "current_stock", "min_stock"})
        }
)
public final class ProductEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @NonNull
    public final String name;

    @ColumnInfo(name = "name_normalized")
    @NonNull
    public final String nameNormalized;

    @Nullable
    public final String brand;

    @NonNull
    public final String type;

    @NonNull
    public final String unit;

    @ColumnInfo(name = "sale_price_cents")
    @Nullable
    public final Long salePriceCents;

    @ColumnInfo(name = "min_stock")
    public final int minStock;

    @ColumnInfo(name = "current_stock")
    public final int currentStock;

    public final boolean active;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public ProductEntity(
            @NonNull String id,
            @NonNull String name,
            @NonNull String nameNormalized,
            @Nullable String brand,
            @NonNull String type,
            @NonNull String unit,
            @Nullable Long salePriceCents,
            int minStock,
            int currentStock,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.brand = brand;
        this.type = type;
        this.unit = unit;
        this.salePriceCents = salePriceCents;
        this.minStock = minStock;
        this.currentStock = currentStock;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
