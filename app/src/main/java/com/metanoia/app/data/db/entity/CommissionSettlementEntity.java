package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para liquidaciones inmutables de comisiones por profesional y período.
 */
@Entity(
        tableName = "commission_settlements",
        foreignKeys = {
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
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
                @Index(value = {"professional_id", "from_day", "to_day"}),
                @Index(value = {"cash_movement_id"}),
                @Index(value = {"settled_at"})
        }
)
public final class CommissionSettlementEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    @ColumnInfo(name = "from_day")
    public final long fromDay;

    @ColumnInfo(name = "to_day")
    public final long toDay;

    @ColumnInfo(name = "base_cents")
    public final long baseCents;

    @ColumnInfo(name = "commission_cents")
    public final long commissionCents;

    @ColumnInfo(name = "cash_movement_id")
    @NonNull
    public final String cashMovementId;

    @ColumnInfo(name = "settled_at")
    public final long settledAt;

    @NonNull
    public final String operator;

    public CommissionSettlementEntity(
            @NonNull String id,
            @NonNull String professionalId,
            long fromDay,
            long toDay,
            long baseCents,
            long commissionCents,
            @NonNull String cashMovementId,
            long settledAt,
            @NonNull String operator
    ) {
        this.id = id;
        this.professionalId = professionalId;
        this.fromDay = fromDay;
        this.toDay = toDay;
        this.baseCents = baseCents;
        this.commissionCents = commissionCents;
        this.cashMovementId = cashMovementId;
        this.settledAt = settledAt;
        this.operator = operator;
    }
}
