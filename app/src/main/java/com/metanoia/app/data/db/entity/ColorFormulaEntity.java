package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para fichas de coloración de clientes con claves foráneas RESTRICT.
 */
@Entity(
        tableName = "color_formulas",
        foreignKeys = {
                @ForeignKey(
                        entity = ClientEntity.class,
                        parentColumns = "id",
                        childColumns = "client_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = AppointmentEntity.class,
                        parentColumns = "id",
                        childColumns = "appointment_id",
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
                @Index(value = {"client_id", "day"}),
                @Index(value = {"appointment_id"}),
                @Index(value = {"professional_id"})
        }
)
public final class ColorFormulaEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "client_id")
    @NonNull
    public final String clientId;

    @ColumnInfo(name = "appointment_id")
    @Nullable
    public final String appointmentId;

    @ColumnInfo(name = "professional_id")
    @Nullable
    public final String professionalId;

    public final long day;

    @ColumnInfo(name = "exposure_min")
    public final int exposureMin;

    @ColumnInfo(name = "developer_volume")
    @Nullable
    public final String developerVolume;

    @NonNull
    public final String notes;

    @Nullable
    public final String result;

    @ColumnInfo(name = "deleted_at")
    @Nullable
    public final Long deletedAt;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    public ColorFormulaEntity(
            @NonNull String id,
            @NonNull String clientId,
            @Nullable String appointmentId,
            @Nullable String professionalId,
            long day,
            int exposureMin,
            @Nullable String developerVolume,
            @NonNull String notes,
            @Nullable String result,
            @Nullable Long deletedAt,
            long createdAt
    ) {
        this.id = id;
        this.clientId = clientId;
        this.appointmentId = appointmentId;
        this.professionalId = professionalId;
        this.day = day;
        this.exposureMin = exposureMin;
        this.developerVolume = developerVolume;
        this.notes = notes;
        this.result = result;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }
}
