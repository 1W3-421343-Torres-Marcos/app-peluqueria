package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para bloqueos de agenda generales o por profesional.
 */
@Entity(
        tableName = "schedule_blocks",
        foreignKeys = {
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"start_day", "end_day"}),
                @Index(value = {"professional_id"})
        }
)
public final class ScheduleBlockEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "professional_id")
    @Nullable
    public final String professionalId;

    @ColumnInfo(name = "start_day")
    public final long startDay;

    @ColumnInfo(name = "end_day")
    public final long endDay;

    @ColumnInfo(name = "start_min")
    @Nullable
    public final Integer startMin;

    @ColumnInfo(name = "end_min")
    @Nullable
    public final Integer endMin;

    @NonNull
    public final String type;

    @NonNull
    public final String reason;

    public final boolean active;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public ScheduleBlockEntity(
            @NonNull String id,
            @Nullable String professionalId,
            long startDay,
            long endDay,
            @Nullable Integer startMin,
            @Nullable Integer endMin,
            @NonNull String type,
            @NonNull String reason,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.professionalId = professionalId;
        this.startDay = startDay;
        this.endDay = endDay;
        this.startMin = startMin;
        this.endMin = endMin;
        this.type = type;
        this.reason = reason;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
