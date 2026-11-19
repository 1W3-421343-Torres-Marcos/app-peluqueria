package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de clientes frecuentes sin columnas de notas sensibles.
 */
public final class FrequentClientProjection {

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "client_name")
    @NonNull
    public final String clientName;

    @NonNull
    public final String phone;

    @ColumnInfo(name = "visit_count")
    public final int visitCount;

    @ColumnInfo(name = "total_spent_cents")
    public final long totalSpentCents;

    @ColumnInfo(name = "last_visit_day")
    public final long lastVisitDay;

    public FrequentClientProjection(
            @NonNull String clientId,
            @NonNull String clientName,
            @NonNull String phone,
            int visitCount,
            long totalSpentCents,
            long lastVisitDay
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.phone = phone;
        this.visitCount = visitCount;
        this.totalSpentCents = totalSpentCents;
        this.lastVisitDay = lastVisitDay;
    }
}
