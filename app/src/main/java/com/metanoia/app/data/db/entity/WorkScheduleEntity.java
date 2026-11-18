package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tramo de horario laboral semanal asignado a un profesional.
 */
@Entity(
        tableName = "work_schedules",
        indices = {@Index(value = {"professional_id", "day_of_week"})},
        foreignKeys = {
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
                        onDelete = ForeignKey.RESTRICT
                )
        }
)
public final class WorkScheduleEntity {

    @PrimaryKey(autoGenerate = true)
    public final long id;

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    @ColumnInfo(name = "day_of_week")
    public final int dayOfWeek;

    @ColumnInfo(name = "start_min")
    public final int startMin;

    @ColumnInfo(name = "end_min")
    public final int endMin;

    public WorkScheduleEntity(
            long id,
            @NonNull String professionalId,
            int dayOfWeek,
            int startMin,
            int endMin
    ) {
        this.id = id;
        this.professionalId = professionalId;
        this.dayOfWeek = dayOfWeek;
        this.startMin = startMin;
        this.endMin = endMin;
    }

    @Ignore
    public WorkScheduleEntity(@NonNull String professionalId, int dayOfWeek, int startMin, int endMin) {
        this(0L, professionalId, dayOfWeek, startMin, endMin);
    }
}
