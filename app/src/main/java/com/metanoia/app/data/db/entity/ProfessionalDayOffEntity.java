package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Fecha puntual libre de un profesional (epochDay).
 */
@Entity(
        tableName = "professional_days_off",
        indices = {@Index(value = {"professional_id", "day_epoch"}, unique = true)},
        foreignKeys = {
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
                        onDelete = ForeignKey.RESTRICT
                )
        }
)
public final class ProfessionalDayOffEntity {

    @PrimaryKey(autoGenerate = true)
    public final long id;

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    @ColumnInfo(name = "day_epoch")
    public final long dayEpoch;

    public ProfessionalDayOffEntity(long id, @NonNull String professionalId, long dayEpoch) {
        this.id = id;
        this.professionalId = professionalId;
        this.dayEpoch = dayEpoch;
    }

    @Ignore
    public ProfessionalDayOffEntity(@NonNull String professionalId, long dayEpoch) {
        this(0L, professionalId, dayEpoch);
    }
}
