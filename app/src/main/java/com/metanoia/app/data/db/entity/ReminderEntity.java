package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para registrar envíos de recordatorios de turno y saludos de cumpleaños.
 */
@Entity(
        tableName = "reminders",
        foreignKeys = {
                @ForeignKey(
                        entity = AppointmentEntity.class,
                        parentColumns = "id",
                        childColumns = "appointment_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = ClientEntity.class,
                        parentColumns = "id",
                        childColumns = "client_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"appointment_id"}),
                @Index(value = {"client_id"}),
                @Index(value = {"type", "appointment_id"}),
                @Index(value = {"type", "client_id", "year"})
        }
)
public final class ReminderEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @NonNull
    public final String type;

    @ColumnInfo(name = "appointment_id")
    @Nullable
    public final String appointmentId;

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @Nullable
    public final Integer year;

    @ColumnInfo(name = "sent_at")
    public final long sentAt;

    @ColumnInfo(name = "sent_by")
    @NonNull
    public final String sentBy;

    public ReminderEntity(
            @NonNull String id,
            @NonNull String type,
            @Nullable String appointmentId,
            @NonNull String clientId,
            @Nullable Integer year,
            long sentAt,
            @NonNull String sentBy
    ) {
        this.id = id;
        this.type = type;
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.year = year;
        this.sentAt = sentAt;
        this.sentBy = sentBy;
    }
}
