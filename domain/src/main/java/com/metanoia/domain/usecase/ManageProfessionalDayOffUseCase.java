package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.repository.ProfessionalRepository;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Agrega o quita fechas libres puntuales para un profesional.
 */
public final class ManageProfessionalDayOffUseCase {

    private final ProfessionalRepository professionalRepository;
    private final Clock clock;

    public ManageProfessionalDayOffUseCase(
            ProfessionalRepository professionalRepository,
            Clock clock
    ) {
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Result<Professional> addDayOff(String professionalId, long epochDay) {
        return updateDayOff(professionalId, epochDay, true);
    }

    public Result<Professional> removeDayOff(String professionalId, long epochDay) {
        return updateDayOff(professionalId, epochDay, false);
    }

    private Result<Professional> updateDayOff(
            String professionalId, long epochDay, boolean add) {
        if (professionalId == null || professionalId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de profesional inválido");
        }
        Optional<Professional> found = professionalRepository.findById(professionalId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado");
        }
        Professional current = found.get();
        Set<Long> days = new LinkedHashSet<>(current.daysOff());
        if (add) {
            days.add(epochDay);
        } else {
            days.remove(epochDay);
        }
        Professional updated = current.toBuilder()
                .daysOff(days)
                .updatedAt(clock.nowMillis())
                .build();
        professionalRepository.save(updated);
        return Result.success(updated);
    }
}
