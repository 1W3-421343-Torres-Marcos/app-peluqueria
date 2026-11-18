package com.metanoia.app.data.db.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;

import java.util.List;

/**
 * Relación Room completa de un turno con todos sus ítems congelados.
 */
public final class AppointmentWithItems {

    @Embedded
    public AppointmentEntity appointment;

    @Relation(
            parentColumn = "id",
            entityColumn = "appointment_id"
    )
    public List<AppointmentItemEntity> items;
}
