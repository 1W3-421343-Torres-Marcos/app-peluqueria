package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de saldos pendientes por cobrar agrupados por cliente.
 */
public final class ReceivableClientProjection {

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "client_name")
    @NonNull
    public final String clientName;

    @ColumnInfo(name = "appointment_count")
    public final int appointmentCount;

    @ColumnInfo(name = "balance_cents")
    public final long balanceCents;

    @ColumnInfo(name = "oldest_day")
    public final long oldestDay;

    public ReceivableClientProjection(
            @NonNull String clientId,
            @NonNull String clientName,
            int appointmentCount,
            long balanceCents,
            long oldestDay
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.appointmentCount = appointmentCount;
        this.balanceCents = balanceCents;
        this.oldestDay = oldestDay;
    }
}
