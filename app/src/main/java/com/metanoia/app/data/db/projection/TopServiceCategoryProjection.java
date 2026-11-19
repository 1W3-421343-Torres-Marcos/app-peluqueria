package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de servicios más solicitados agrupados por categoría.
 */
public final class TopServiceCategoryProjection {

    @NonNull
    public final String category;

    @ColumnInfo(name = "service_count")
    public final int serviceCount;

    @ColumnInfo(name = "revenue_cents")
    public final long revenueCents;

    public TopServiceCategoryProjection(
            @NonNull String category,
            int serviceCount,
            long revenueCents
    ) {
        this.category = category;
        this.serviceCount = serviceCount;
        this.revenueCents = revenueCents;
    }
}
