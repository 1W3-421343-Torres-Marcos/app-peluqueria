package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de servicios más solicitados sobre instantáneas de ítems de turno.
 */
public final class TopServiceProjection {

    @ColumnInfo(name = "service_id")
    @NonNull
    public final String serviceId;

    @ColumnInfo(name = "service_name")
    @NonNull
    public final String serviceName;

    @NonNull
    public final String category;

    @ColumnInfo(name = "service_count")
    public final int serviceCount;

    @ColumnInfo(name = "revenue_cents")
    public final long revenueCents;

    public TopServiceProjection(
            @NonNull String serviceId,
            @NonNull String serviceName,
            @NonNull String category,
            int serviceCount,
            long revenueCents
    ) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.category = category;
        this.serviceCount = serviceCount;
        this.revenueCents = revenueCents;
    }
}
