package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para movimientos contables inmutables de caja con FK RESTRICT.
 */
@Entity(
        tableName = "cash_movements",
        foreignKeys = {
                @ForeignKey(
                        entity = AppointmentEntity.class,
                        parentColumns = "id",
                        childColumns = "appointment_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = ClientEntity.class,
                        parentColumns = "id",
                        childColumns = "client_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = CashMovementEntity.class,
                        parentColumns = "id",
                        childColumns = "reverses_movement_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = CashClosingEntity.class,
                        parentColumns = "id",
                        childColumns = "closing_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"day"}),
                @Index(value = {"method"}),
                @Index(value = {"appointment_id"}),
                @Index(value = {"client_id"}),
                @Index(value = {"reverses_movement_id"}),
                @Index(value = {"closing_id"})
        }
)
public final class CashMovementEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    public final long day;

    public final long timestamp;

    @NonNull
    public final String type;

    @NonNull
    public final String method;

    @ColumnInfo(name = "amount_cents")
    public final long amountCents;

    @NonNull
    public final String concept;

    @ColumnInfo(name = "appointment_id")
    @Nullable
    public final String appointmentId;

    @ColumnInfo(name = "client_id")
    @Nullable
    public final String clientId;

    @ColumnInfo(name = "product_id")
    @Nullable
    public final String productId;

    @Nullable
    public final Integer quantity;

    @ColumnInfo(name = "reverses_movement_id")
    @Nullable
    public final String reversesMovementId;

    @NonNull
    public final String operator;

    @ColumnInfo(name = "closing_id")
    @Nullable
    public final String closingId;

    public CashMovementEntity(
            @NonNull String id,
            long day,
            long timestamp,
            @NonNull String type,
            @NonNull String method,
            long amountCents,
            @NonNull String concept,
            @Nullable String appointmentId,
            @Nullable String clientId,
            @Nullable String productId,
            @Nullable Integer quantity,
            @Nullable String reversesMovementId,
            @NonNull String operator,
            @Nullable String closingId
    ) {
        this.id = id;
        this.day = day;
        this.timestamp = timestamp;
        this.type = type;
        this.method = method;
        this.amountCents = amountCents;
        this.concept = concept;
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.productId = productId;
        this.quantity = quantity;
        this.reversesMovementId = reversesMovementId;
        this.operator = operator;
        this.closingId = closingId;
    }
}
