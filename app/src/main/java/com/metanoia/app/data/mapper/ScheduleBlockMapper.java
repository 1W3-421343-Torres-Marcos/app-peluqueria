package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.ScheduleBlockType;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte entre {@link ScheduleBlockEntity} y {@link ScheduleBlock}.
 */
public final class ScheduleBlockMapper {

    private ScheduleBlockMapper() {
    }

    public static ScheduleBlock toDomain(ScheduleBlockEntity entity) {
        return new ScheduleBlock.Builder()
                .id(entity.id)
                .professionalId(entity.professionalId)
                .startDay(entity.startDay)
                .endDay(entity.endDay)
                .startMin(entity.startMin)
                .endMin(entity.endMin)
                .type(ScheduleBlockType.valueOf(entity.type))
                .reason(entity.reason)
                .active(entity.active)
                .createdAt(entity.createdAt)
                .updatedAt(entity.updatedAt)
                .build();
    }

    public static List<ScheduleBlock> toDomainList(List<ScheduleBlockEntity> entities) {
        List<ScheduleBlock> result = new ArrayList<>(entities.size());
        for (ScheduleBlockEntity entity : entities) {
            result.add(toDomain(entity));
        }
        return result;
    }

    public static ScheduleBlockEntity toEntity(ScheduleBlock block) {
        return new ScheduleBlockEntity(
                block.id(),
                block.professionalId().orElse(null),
                block.startDay(),
                block.endDay(),
                block.startMin().orElse(null),
                block.endMin().orElse(null),
                block.type().name(),
                block.reason(),
                block.isActive(),
                block.createdAt(),
                block.updatedAt()
        );
    }
}
