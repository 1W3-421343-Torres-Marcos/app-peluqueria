package com.metanoia.app.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tramo de horario general de atención del salón por día de la semana (1 = lunes a 7 = domingo).
 */
@Entity(
        tableName = "salon_hours",
        indices = {@Index("day_of_week")}
)
public final class SalonHoursEntity {

    @PrimaryKey(autoGenerate = true)
    public final long id;

    @ColumnInfo(name = "day_of_week")
    public final int dayOfWeek;

    @ColumnInfo(name = "start_min")
    public final int startMin;

    @ColumnInfo(name = "end_min")
    public final int endMin;

    public SalonHoursEntity(long id, int dayOfWeek, int startMin, int endMin) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startMin = startMin;
        this.endMin = endMin;
    }

    @Ignore
    public SalonHoursEntity(int dayOfWeek, int startMin, int endMin) {
        this(0L, dayOfWeek, startMin, endMin);
    }
}
