package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL liviana para tarjetas de turno en la agenda diaria, semanal e historial.
 */
public final class AppointmentCardProjection {

    @NonNull
    public final String id;

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "client_name")
    @NonNull
    public final String clientName;

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    @ColumnInfo(name = "professional_name")
    @NonNull
    public final String professionalName;

    public final long day;

    @ColumnInfo(name = "start_min")
    public final int startMin;

    @ColumnInfo(name = "end_min")
    public final int endMin;

    @NonNull
    public final String status;

    @ColumnInfo(name = "services_summary")
    @NonNull
    public final String servicesSummary;

    @ColumnInfo(name = "items_subtotal_cents")
    public final long itemsSubtotalCents;

    @ColumnInfo(name = "adjustment_cents")
    public final long adjustmentCents;

    @ColumnInfo(name = "paid_cents")
    public final long paidCents;

    @ColumnInfo(name = "recurrence_rule_id")
    @Nullable
    public final String recurrenceRuleId;

    public AppointmentCardProjection(
            @NonNull String id,
            @NonNull String clientId,
            @NonNull String clientName,
            @NonNull String professionalId,
            @NonNull String professionalName,
            long day,
            int startMin,
            int endMin,
            @NonNull String status,
            @NonNull String servicesSummary,
            long itemsSubtotalCents,
            long adjustmentCents,
            long paidCents,
            @Nullable String recurrenceRuleId
    ) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.professionalId = professionalId;
        this.professionalName = professionalName;
        this.day = day;
        this.startMin = startMin;
        this.endMin = endMin;
        this.status = status;
        this.servicesSummary = servicesSummary;
        this.itemsSubtotalCents = itemsSubtotalCents;
        this.adjustmentCents = adjustmentCents;
        this.paidCents = paidCents;
        this.recurrenceRuleId = recurrenceRuleId;
    }

    public long totalCents() {
        return Math.max(0L, itemsSubtotalCents + adjustmentCents);
    }

    public long pendingBalanceCents() {
        return "COMPLETED".equals(status) ? Math.max(0L, totalCents() - paidCents) : 0L;
    }
}
