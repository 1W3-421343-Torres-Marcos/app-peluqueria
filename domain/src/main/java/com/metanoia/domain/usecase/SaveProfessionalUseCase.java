package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ProfessionalValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Valida y guarda (alta o edición) un profesional con horario, comisión y servicios activos.
 */
public final class SaveProfessionalUseCase {

    private final ProfessionalRepository professionalRepository;
    private final SalonServiceRepository serviceRepository;
    private final SettingsRepository settingsRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SaveProfessionalUseCase(
            ProfessionalRepository professionalRepository,
            SalonServiceRepository serviceRepository,
            SettingsRepository settingsRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Professional> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        if (!ProfessionalValidator.isValidName(request.name)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El nombre debe tener entre 2 y 80 caracteres");
        }
        if (!ProfessionalValidator.isValidCommissionBasisPoints(request.commissionBasisPoints)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La comisión debe estar entre 0,00 % y 100,00 %");
        }
        List<TimeRange> salonSchedule = settingsRepository.getSettings().generalSchedule();
        if (!ProfessionalValidator.areValidWorkSchedules(request.workSchedules, salonSchedule)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El horario laboral tiene solapamientos o excede el horario del salón");
        }
        if (!ProfessionalValidator.areAllServicesActive(request.serviceIds, serviceRepository)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Solo pueden asignarse servicios existentes y activos");
        }

        boolean isEdit = request.id != null && !request.id.trim().isEmpty();
        Professional existing = null;
        if (isEdit) {
            Optional<Professional> found = professionalRepository.findById(request.id.trim());
            if (!found.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado");
            }
            existing = found.get();
            if (!request.active && existing.isActive() && professionalRepository
                    .hasFutureActiveAppointments(existing.id(), clock.todayEpochDay())) {
                return Result.failure(DomainError.CONFLICT,
                        "No se puede desactivar un profesional con turnos futuros activos");
            }
        }

        List<TimeRange> sortedSchedules = new ArrayList<>(request.workSchedules);
        Collections.sort(sortedSchedules);

        long now = clock.nowMillis();
        Professional professional = new Professional.Builder()
                .id(isEdit ? existing.id() : idGenerator.nextId())
                .name(request.name.trim())
                .phone(request.phone == null ? "" : request.phone.trim())
                .colorTag(request.colorTag == null ? "" : request.colorTag.trim())
                .commissionBasisPoints(request.commissionBasisPoints)
                .active(request.active)
                .workSchedules(sortedSchedules)
                .serviceIds(request.serviceIds)
                .daysOff(request.daysOff)
                .createdAt(isEdit ? existing.createdAt() : now)
                .updatedAt(now)
                .build();

        professionalRepository.save(professional);
        return Result.success(professional);
    }

    public static final class Request {
        private final String id;
        private final String name;
        private final String phone;
        private final String colorTag;
        private final int commissionBasisPoints;
        private final boolean active;
        private final List<TimeRange> workSchedules;
        private final Set<String> serviceIds;
        private final Set<Long> daysOff;

        public Request(
                String id,
                String name,
                String phone,
                String colorTag,
                int commissionBasisPoints,
                boolean active,
                List<TimeRange> workSchedules,
                Set<String> serviceIds,
                Set<Long> daysOff
        ) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.colorTag = colorTag;
            this.commissionBasisPoints = commissionBasisPoints;
            this.active = active;
            this.workSchedules = workSchedules == null
                    ? Collections.emptyList()
                    : new ArrayList<>(workSchedules);
            this.serviceIds = serviceIds == null
                    ? Collections.emptySet()
                    : new LinkedHashSet<>(serviceIds);
            this.daysOff = daysOff == null
                    ? Collections.emptySet()
                    : new LinkedHashSet<>(daysOff);
        }
    }
}
