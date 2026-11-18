package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.repository.ProfessionalRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Activa o desactiva un profesional verificando que no tenga turnos futuros pendientes/confirmados.
 */
public final class SetProfessionalActiveUseCase {

    private final ProfessionalRepository professionalRepository;
    private final Clock clock;

    public SetProfessionalActiveUseCase(
            ProfessionalRepository professionalRepository,
            Clock clock
    ) {
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Result<Professional> execute(String professionalId, boolean active) {
        if (professionalId == null || professionalId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de profesional inválido");
        }
        Optional<Professional> found = professionalRepository.findById(professionalId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado");
        }
        Professional current = found.get();
        if (current.isActive() == active) {
            return Result.success(current);
        }
        if (!active && professionalRepository.hasFutureActiveAppointments(
                current.id(), clock.todayEpochDay())) {
            return Result.failure(DomainError.CONFLICT,
                    "No se puede desactivar un profesional con turnos futuros activos");
        }
        Professional updated = current.toBuilder()
                .active(active)
                .updatedAt(clock.nowMillis())
                .build();
        professionalRepository.save(updated);
        return Result.success(updated);
    }
}
