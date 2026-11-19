package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de rendimiento y comisiones por profesional en el período.
 */
public final class ProfessionalPerformanceProjection {

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    @ColumnInfo(name = "professional_name")
    @NonNull
    public final String professionalName;

    @ColumnInfo(name = "completed_count")
    public final int completedCount;

    @ColumnInfo(name = "billed_cents")
    public final long billedCents;

    @ColumnInfo(name = "no_show_count")
    public final int noShowCount;

    @ColumnInfo(name = "commission_cents")
    public final long commissionCents;

    public ProfessionalPerformanceProjection(
            @NonNull String professionalId,
            @NonNull String professionalName,
            int completedCount,
            long billedCents,
            int noShowCount,
            long commissionCents
    ) {
        this.professionalId = professionalId;
        this.professionalName = professionalName;
        this.completedCount = completedCount;
        this.billedCents = billedCents;
        this.noShowCount = noShowCount;
        this.commissionCents = commissionCents;
    }
}
