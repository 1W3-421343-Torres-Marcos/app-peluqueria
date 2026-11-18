package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para solicitudes de clientes en lista de espera.
 */
@Entity(
        tableName = "waitlist_entries",
        foreignKeys = {
                @ForeignKey(
                        entity = ClientEntity.class,
                        parentColumns = "id",
                        childColumns = "client_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"status", "valid_until_day"}),
                @Index(value = {"client_id", "status"}),
                @Index(value = {"professional_id"})
        }
)
public final class WaitlistEntryEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "professional_id")
    @Nullable
    public final String professionalId;

    @ColumnInfo(name = "specific_day")
    @Nullable
    public final Long specificDay;

    @ColumnInfo(name = "preferred_weekdays")
    @NonNull
    public final String preferredWeekdays;

    @ColumnInfo(name = "from_min")
    public final int fromMin;

    @ColumnInfo(name = "to_min")
    public final int toMin;

    @ColumnInfo(name = "valid_until_day")
    public final long validUntilDay;

    @NonNull
    public final String status;

    @NonNull
    public final String notes;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "offered_at")
    @Nullable
    public final Long offeredAt;

    public WaitlistEntryEntity(
            @NonNull String id,
            @NonNull String clientId,
            @Nullable String professionalId,
            @Nullable Long specificDay,
            @NonNull String preferredWeekdays,
            int fromMin,
            int toMin,
            long validUntilDay,
            @NonNull String status,
            @NonNull String notes,
            long createdAt,
            @Nullable Long offeredAt
    ) {
        this.id = id;
        this.clientId = clientId;
        this.professionalId = professionalId;
        this.specificDay = specificDay;
        this.preferredWeekdays = preferredWeekdays;
        this.fromMin = fromMin;
        this.toMin = toMin;
        this.validUntilDay = validUntilDay;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.offeredAt = offeredAt;
    }
}
