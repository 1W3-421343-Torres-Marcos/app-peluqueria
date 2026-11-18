package com.metanoia.app.data.db.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.metanoia.app.data.db.entity.ProfessionalDayOffEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.WorkScheduleEntity;

import java.util.List;

/**
 * Proyección compuesta de un profesional junto con su horario, servicios y días libres.
 */
public final class ProfessionalWithDetails {

    @Embedded
    public ProfessionalEntity professional;

    @Relation(parentColumn = "id", entityColumn = "professional_id")
    public List<WorkScheduleEntity> workSchedules;

    @Relation(parentColumn = "id", entityColumn = "professional_id")
    public List<ProfessionalServiceCrossRef> serviceRefs;

    @Relation(parentColumn = "id", entityColumn = "professional_id")
    public List<ProfessionalDayOffEntity> daysOff;
}
