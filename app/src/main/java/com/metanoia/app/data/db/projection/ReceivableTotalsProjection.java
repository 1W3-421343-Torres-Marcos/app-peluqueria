package com.metanoia.app.data.db.projection;

import androidx.room.ColumnInfo;

/**
 * Proyección SQL liviana de totales históricos pendientes por cobrar.
 */
public final class ReceivableTotalsProjection {

    @ColumnInfo(name = "total_balance_cents")
    public final long totalBalanceCents;

    @ColumnInfo(name = "appointment_count")
    public final int appointmentCount;

    public ReceivableTotalsProjection(long totalBalanceCents, int appointmentCount) {
        this.totalBalanceCents = totalBalanceCents;
        this.appointmentCount = appointmentCount;
    }
}
