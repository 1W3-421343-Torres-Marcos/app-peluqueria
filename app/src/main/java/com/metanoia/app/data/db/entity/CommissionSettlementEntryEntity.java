package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Renglón Room inmutable vinculando una liquidación con un turno liquidado o un ajuste por reverso.
 */
@Entity(
        tableName = "commission_settlement_entries",
        foreignKeys = {
                @ForeignKey(
                        entity = CommissionSettlementEntity.class,
                        parentColumns = "id",
                        childColumns = "settlement_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = AppointmentEntity.class,
                        parentColumns = "id",
                        childColumns = "appointment_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"settlement_id"}),
                @Index(value = {"appointment_id"})
        }
)
public final class CommissionSettlementEntryEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "settlement_id")
    @NonNull
    public final String settlementId;

    @ColumnInfo(name = "appointment_id")
    @NonNull
    public final String appointmentId;

    @NonNull
    public final String type;

    @ColumnInfo(name = "base_cents")
    public final long baseCents;

    @ColumnInfo(name = "commission_cents")
    public final long commissionCents;

    public CommissionSettlementEntryEntity(
            @NonNull String id,
            @NonNull String settlementId,
            @NonNull String appointmentId,
            @NonNull String type,
            long baseCents,
            long commissionCents
    ) {
        this.id = id;
        this.settlementId = settlementId;
        this.appointmentId = appointmentId;
        this.type = type;
        this.baseCents = baseCents;
        this.commissionCents = commissionCents;
    }
}
