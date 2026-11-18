package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.ProfessionalDayOffEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.WorkScheduleEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.TimeRange;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapea entre {@link ProfessionalWithDetails} y {@link Professional}.
 */
public final class ProfessionalMapper {

    private ProfessionalMapper() {
    }

    public static Professional toDomain(ProfessionalWithDetails details) {
        ProfessionalEntity entity = details.professional;
        List<TimeRange> schedules = new ArrayList<>();
        if (details.workSchedules != null) {
            for (WorkScheduleEntity ws : details.workSchedules) {
                if (ws.dayOfWeek >= 1 && ws.dayOfWeek <= 7
                        && ws.startMin >= 0 && ws.endMin <= 1440 && ws.startMin < ws.endMin) {
                    schedules.add(new TimeRange(ws.dayOfWeek, ws.startMin, ws.endMin));
                }
            }
        }
        Set<String> serviceIds = new LinkedHashSet<>();
        if (details.serviceRefs != null) {
            for (ProfessionalServiceCrossRef ref : details.serviceRefs) {
                serviceIds.add(ref.serviceId);
            }
        }
        Set<Long> daysOff = new LinkedHashSet<>();
        if (details.daysOff != null) {
            for (ProfessionalDayOffEntity day : details.daysOff) {
                daysOff.add(day.dayEpoch);
            }
        }
        return new Professional.Builder()
                .id(entity.id)
                .name(entity.name)
                .phone(entity.phone)
                .colorTag(entity.colorTag)
                .commissionBasisPoints(entity.commissionBasisPoints)
                .active(entity.active)
                .workSchedules(schedules)
                .serviceIds(serviceIds)
                .daysOff(daysOff)
                .createdAt(entity.createdAt)
                .updatedAt(entity.updatedAt)
                .build();
    }

    public static ProfessionalEntity toEntity(Professional professional) {
        return new ProfessionalEntity(
                professional.id(),
                professional.name(),
                professional.phone(),
                professional.colorTag(),
                professional.commissionBasisPoints(),
                professional.isActive(),
                professional.createdAt(),
                professional.updatedAt()
        );
    }

    public static List<WorkScheduleEntity> toScheduleEntities(Professional professional) {
        List<WorkScheduleEntity> result = new ArrayList<>();
        for (TimeRange range : professional.workSchedules()) {
            result.add(new WorkScheduleEntity(
                    professional.id(), range.dayOfWeek(), range.startMin(), range.endMin()));
        }
        return result;
    }

    public static List<ProfessionalServiceCrossRef> toServiceRefs(Professional professional) {
        List<ProfessionalServiceCrossRef> result = new ArrayList<>();
        for (String serviceId : professional.serviceIds()) {
            result.add(new ProfessionalServiceCrossRef(professional.id(), serviceId));
        }
        return result;
    }

    public static List<ProfessionalDayOffEntity> toDayOffEntities(Professional professional) {
        List<ProfessionalDayOffEntity> result = new ArrayList<>();
        for (Long epochDay : professional.daysOff()) {
            result.add(new ProfessionalDayOffEntity(professional.id(), epochDay));
        }
        return result;
    }
}
