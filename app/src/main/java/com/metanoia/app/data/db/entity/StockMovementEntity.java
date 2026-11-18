package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para movimientos inmutables de stock con claves foráneas RESTRICT.
 */
@Entity(
        tableName = "stock_movements",
        foreignKeys = {
                @ForeignKey(
                        entity = ProductEntity.class,
                        parentColumns = "id",
                        childColumns = "product_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = CashMovementEntity.class,
                        parentColumns = "id",
                        childColumns = "cash_movement_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"product_id", "timestamp"}),
                @Index(value = {"cash_movement_id"})
        }
)
public final class StockMovementEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "product_id")
    @NonNull
    public final String productId;

    public final long timestamp;

    @NonNull
    public final String type;

    public final int quantity;

    @Nullable
    public final String reason;

    @ColumnInfo(name = "cash_movement_id")
    @Nullable
    public final String cashMovementId;

    @NonNull
    public final String operator;

    public StockMovementEntity(
            @NonNull String id,
            @NonNull String productId,
            long timestamp,
            @NonNull String type,
            int quantity,
            @Nullable String reason,
            @Nullable String cashMovementId,
            @NonNull String operator
    ) {
        this.id = id;
        this.productId = productId;
        this.timestamp = timestamp;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.cashMovementId = cashMovementId;
        this.operator = operator;
    }
}
