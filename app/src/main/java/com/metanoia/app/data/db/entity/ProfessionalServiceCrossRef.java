package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Relación entre profesionales y los servicios que realizan.
 */
@Entity(
        tableName = "professional_services",
        primaryKeys = {"professional_id", "service_id"},
        indices = {@Index("service_id")},
        foreignKeys = {
                @ForeignKey(
                        entity = ProfessionalEntity.class,
                        parentColumns = "id",
                        childColumns = "professional_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = SalonServiceEntity.class,
                        parentColumns = "id",
                        childColumns = "service_id",
                        onDelete = ForeignKey.RESTRICT
                )
        }
)
public final class ProfessionalServiceCrossRef {

    @ColumnInfo(name = "professional_id")
    @NonNull
    public final String professionalId;

    @ColumnInfo(name = "service_id")
    @NonNull
    public final String serviceId;

    public ProfessionalServiceCrossRef(
            @NonNull String professionalId,
            @NonNull String serviceId
    ) {
        this.professionalId = professionalId;
        this.serviceId = serviceId;
    }
}
