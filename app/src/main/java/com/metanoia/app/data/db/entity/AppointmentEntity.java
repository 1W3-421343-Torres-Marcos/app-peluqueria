package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room de turnos de agenda con índices por profesional/día y cliente/día y FK RESTRICT.
 */
@Entity(
        tableName = "appointments",
        foreignKeys = {
                @ForeignKey(
                        entity = ClientEntity.class,
                        parentColumns = "id",
                        childColumns = "client_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"professional_id", "day"}),
                @Index(value = {"client_id", "day"}),
                @Index(value = {"day", "start_min"}),
                @Index(value = {"recurrence_rule_id"})
        }
)
public final class AppointmentEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    public final long day;

    @ColumnInfo(name = "start_min")
    public final int startMin;

    @ColumnInfo(name = "end_min")
    public final int endMin;

    @NonNull
    public final String status;

    @ColumnInfo(name = "adjustment_cents")
    public final long adjustmentCents;

    @NonNull
    public final String notes;

    @ColumnInfo(name = "recurrence_rule_id")
    @Nullable
    public final String recurrenceRuleId;

    @ColumnInfo(name = "cancel_reason")
    @Nullable
    public final String cancelReason;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    @ColumnInfo(name = "created_by")
    @NonNull
    public final String createdBy;

    public AppointmentEntity(
            @NonNull String id,
            @NonNull String clientId,
            @NonNull String professionalId,
            long day,
            int startMin,
            int endMin,
            @NonNull String status,
            long adjustmentCents,
            @NonNull String notes,
            @Nullable String recurrenceRuleId,
            @Nullable String cancelReason,
            long createdAt,
            long updatedAt,
            @NonNull String createdBy
    ) {
        this.id = id;
        this.clientId = clientId;
        this.professionalId = professionalId;
        this.day = day;
        this.startMin = startMin;
        this.endMin = endMin;
        this.status = status;
        this.adjustmentCents = adjustmentCents;
        this.notes = notes;
        this.recurrenceRuleId = recurrenceRuleId;
        this.cancelReason = cancelReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }
}
