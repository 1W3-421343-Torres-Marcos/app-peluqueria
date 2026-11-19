package com.metanoia.app.data.db.projection;

import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de minutos ocupados por día ISO de semana y hora del día.
 */
public final class OccupiedSlotProjection {

    @ColumnInfo(name = "day_of_week")
    public final int dayOfWeek;

    @ColumnInfo(name = "slot_hour")
    public final int slotHour;

    @ColumnInfo(name = "occupied_minutes")
    public final long occupiedMinutes;

    @ColumnInfo(name = "appointment_count")
    public final int appointmentCount;

    public OccupiedSlotProjection(
            int dayOfWeek,
            int slotHour,
            long occupiedMinutes,
            int appointmentCount
    ) {
        this.dayOfWeek = dayOfWeek;
        this.slotHour = slotHour;
        this.occupiedMinutes = occupiedMinutes;
        this.appointmentCount = appointmentCount;
    }
}
