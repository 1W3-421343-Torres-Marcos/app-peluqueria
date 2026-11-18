package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para los servicios del catálogo del salón.
 */
@Entity(
        tableName = "salon_services",
        indices = {
                @Index(value = {"category_normalized", "active", "sort_order"})
        }
)
public final class SalonServiceEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @NonNull
    public final String name;

    @ColumnInfo(name = "name_normalized")
    @NonNull
    public final String nameNormalized;

    @NonNull
    public final String category;

    @ColumnInfo(name = "category_normalized")
    @NonNull
    public final String categoryNormalized;

    @ColumnInfo(name = "price_cents")
    public final long priceCents;

    @ColumnInfo(name = "duration_min")
    public final int durationMin;

    @ColumnInfo(name = "is_color_service")
    public final boolean colorService;

    public final boolean active;

    @ColumnInfo(name = "sort_order")
    public final int sortOrder;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public SalonServiceEntity(
            @NonNull String id,
            @NonNull String name,
            @NonNull String nameNormalized,
            @NonNull String category,
            @NonNull String categoryNormalized,
            long priceCents,
            int durationMin,
            boolean colorService,
            boolean active,
            int sortOrder,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.category = category;
        this.categoryNormalized = categoryNormalized;
        this.priceCents = priceCents;
        this.durationMin = durationMin;
        this.colorService = colorService;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
