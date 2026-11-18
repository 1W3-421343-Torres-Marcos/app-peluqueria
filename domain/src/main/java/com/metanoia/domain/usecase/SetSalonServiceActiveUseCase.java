package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.rules.TextNormalizer;

import java.util.Objects;
import java.util.Optional;

/**
 * Realiza la baja o reactivación lógica de un servicio sin borrar registros históricos.
 */
public final class SetSalonServiceActiveUseCase {

    private final SalonServiceRepository serviceRepository;
    private final Clock clock;

    public SetSalonServiceActiveUseCase(SalonServiceRepository serviceRepository, Clock clock) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Result<SalonService> execute(String serviceId, boolean active) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de servicio inválido");
        }
        Optional<SalonService> found = serviceRepository.findById(serviceId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Servicio no encontrado");
        }
        SalonService current = found.get();
        if (current.isActive() == active) {
            return Result.success(current);
        }
        if (active) {
            Optional<SalonService> conflict = serviceRepository
                    .findActiveByCategoryAndNormalizedName(
                            TextNormalizer.normalize(current.category()),
                            TextNormalizer.normalize(current.name()));
            if (conflict.isPresent() && !conflict.get().id().equals(current.id())) {
                return Result.failure(DomainError.CONFLICT,
                        "Ya existe otro servicio activo con el mismo nombre y categoría");
            }
        }
        SalonService updated = current.toBuilder()
                .active(active)
                .updatedAt(clock.nowMillis())
                .build();
        serviceRepository.save(updated);
        return Result.success(updated);
    }
}
