package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.rules.TextNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapea entre {@link SalonServiceEntity} y {@link SalonService}.
 */
public final class SalonServiceMapper {

    private SalonServiceMapper() {
    }

    public static SalonService toDomain(SalonServiceEntity entity) {
        return new SalonService.Builder()
                .id(entity.id)
                .name(entity.name)
                .category(entity.category)
                .price(Money.ofCents(entity.priceCents))
                .durationMin(entity.durationMin)
                .colorService(entity.colorService)
                .active(entity.active)
                .sortOrder(entity.sortOrder)
                .createdAt(entity.createdAt)
                .updatedAt(entity.updatedAt)
                .build();
    }

    public static List<SalonService> toDomainList(List<SalonServiceEntity> entities) {
        List<SalonService> result = new ArrayList<>(entities.size());
        for (SalonServiceEntity entity : entities) {
            result.add(toDomain(entity));
        }
        return result;
    }

    public static SalonServiceEntity toEntity(SalonService service) {
        return new SalonServiceEntity(
                service.id(),
                service.name(),
                TextNormalizer.normalize(service.name()),
                service.category(),
                TextNormalizer.normalize(service.category()),
                service.price().cents(),
                service.durationMin(),
                service.isColorService(),
                service.isActive(),
                service.sortOrder(),
                service.createdAt(),
                service.updatedAt()
        );
    }
}
