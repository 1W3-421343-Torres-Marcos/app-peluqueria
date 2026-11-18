package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Relación entre una entrada de lista de espera y los servicios solicitados.
 */
@Entity(
        tableName = "waitlist_entry_services",
        primaryKeys = {"entry_id", "service_id"},
        indices = {@Index("service_id")},
        foreignKeys = {
                @ForeignKey(
                        entity = WaitlistEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "entry_id",
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
public final class WaitlistEntryServiceCrossRef {

    @ColumnInfo(name = "entry_id")
    @NonNull
    public final String entryId;

    @ColumnInfo(name = "service_id")
    @NonNull
    public final String serviceId;

    public WaitlistEntryServiceCrossRef(
            @NonNull String entryId,
            @NonNull String serviceId
    ) {
        this.entryId = entryId;
        this.serviceId = serviceId;
    }
}
