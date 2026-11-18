package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL liviana para cumpleaños del día y estado de saludo anual.
 */
public final class BirthdayReminderProjection {

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "client_name")
    @NonNull
    public final String clientName;

    @ColumnInfo(name = "client_phone")
    @NonNull
    public final String clientPhone;

    @ColumnInfo(name = "birth_day")
    public final int birthDay;

    @ColumnInfo(name = "birth_month")
    public final int birthMonth;

    @ColumnInfo(name = "birth_year")
    @Nullable
    public final Integer birthYear;

    @ColumnInfo(name = "reminder_id")
    @Nullable
    public final String reminderId;

    @ColumnInfo(name = "sent_at")
    @Nullable
    public final Long sentAt;

    public BirthdayReminderProjection(
            @NonNull String clientId,
            @NonNull String clientName,
            @NonNull String clientPhone,
            int birthDay,
            int birthMonth,
            @Nullable Integer birthYear,
            @Nullable String reminderId,
            @Nullable Long sentAt
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.birthDay = birthDay;
        this.birthMonth = birthMonth;
        this.birthYear = birthYear;
        this.reminderId = reminderId;
        this.sentAt = sentAt;
    }

    public boolean isSent() {
        return reminderId != null;
    }
}
