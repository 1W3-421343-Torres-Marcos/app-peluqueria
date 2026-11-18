package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL liviana para turnos de mañana pendientes o ya recordados.
 */
public final class TomorrowReminderProjection {

    @ColumnInfo(name = "appointment_id")
    @NonNull
    public final String appointmentId;

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "client_name")
    @NonNull
    public final String clientName;

    @ColumnInfo(name = "client_phone")
    @NonNull
    public final String clientPhone;

    @ColumnInfo(name = "professional_name")
    @NonNull
    public final String professionalName;

    public final long day;

    @ColumnInfo(name = "start_min")
    public final int startMin;

    @NonNull
    public final String status;

    @ColumnInfo(name = "services_summary")
    @NonNull
    public final String servicesSummary;

    @ColumnInfo(name = "reminder_id")
    @Nullable
    public final String reminderId;

    @ColumnInfo(name = "sent_at")
    @Nullable
    public final Long sentAt;

    public TomorrowReminderProjection(
            @NonNull String appointmentId,
            @NonNull String clientId,
            @NonNull String clientName,
            @NonNull String clientPhone,
            @NonNull String professionalName,
            long day,
            int startMin,
            @NonNull String status,
            @NonNull String servicesSummary,
            @Nullable String reminderId,
            @Nullable Long sentAt
    ) {
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.professionalName = professionalName;
        this.day = day;
        this.startMin = startMin;
        this.status = status;
        this.servicesSummary = servicesSummary;
        this.reminderId = reminderId;
        this.sentAt = sentAt;
    }

    public boolean isSent() {
        return reminderId != null;
    }
}
