package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.AppointmentSupport;
import com.metanoia.domain.rules.AvailabilityChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Edita los servicios y/o notas de un turno PENDING o CONFIRMED conservando los snapshots previos.
 */
public final class UpdateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final SalonServiceRepository serviceRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public UpdateAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            ProfessionalRepository professionalRepository,
            SalonServiceRepository serviceRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.scheduleBlockRepository = Objects.requireNonNull(
                scheduleBlockRepository, "scheduleBlockRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Appointment> execute(Request request) {
        if (request == null || request.appointmentId == null
                || request.appointmentId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de turno inválido");
        }
        Optional<Appointment> found = appointmentRepository.findById(request.appointmentId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Turno no encontrado");
        }
        Appointment existing = found.get();
        if (!existing.status().isEditable()) {
            return Result.failure(DomainError.CONFLICT,
                    "Solo pueden editarse turnos pendientes o confirmados");
        }
        if (existing.day() < clock.todayEpochDay()) {
            return Result.failure(DomainError.CONFLICT,
                    "Los turnos pasados no admiten edición de servicios ni horario");
        }

        Optional<Professional> proOpt = professionalRepository.findById(existing.professionalId());
        if (!proOpt.isPresent() || !proOpt.get().isActive()) {
            return Result.failure(DomainError.CONFLICT, "El profesional del turno está inactivo");
        }
        Professional professional = proOpt.get();

        Result<List<AppointmentItem>> itemsResult = AppointmentSupport.buildFrozenItems(
                existing.id(),
                request.serviceIds,
                existing.items(),
                professional,
                serviceRepository,
                idGenerator
        );
        if (itemsResult.isFailure()) {
            return Result.failure(itemsResult.getError(), itemsResult.getErrorMessage());
        }

        Settings settings = settingsRepository.getSettings();
        int newEndMin = Appointment.calculateEndMin(
                existing.startMin(), itemsResult.getValue(), settings.slotMinutes());

        List<Appointment> existingOnDay = appointmentRepository.findByDayAndProfessional(
                existing.day(), professional.id());
        List<ScheduleBlock> blocksOnDay = scheduleBlockRepository.findActiveOverlappingRange(
                existing.day(), existing.day());

        AvailabilityChecker.UnavailabilityReason availability = AvailabilityChecker.check(
                professional,
                existing.day(),
                existing.startMin(),
                newEndMin,
                settings.generalSchedule(),
                existingOnDay,
                blocksOnDay,
                existing.id()
        );
        if (!availability.isAvailable()) {
            return Result.failure(DomainError.CONFLICT,
                    AppointmentSupport.unavailabilityMessage(availability));
        }

        long now = clock.nowMillis();
        Appointment updated = existing.toBuilder()
                .endMin(newEndMin)
                .notes(request.notes == null ? "" : request.notes.trim())
                .items(itemsResult.getValue())
                .updatedAt(now)
                .build();

        appointmentRepository.save(updated);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("APPOINTMENT_UPDATE")
                .entityType("APPOINTMENT")
                .entityId(updated.id())
                .amountCents(updated.totalAmount().cents())
                .concept("Edición de turno")
                .build());
        return Result.success(updated);
    }

    public static final class Request {
        private final String appointmentId;
        private final List<String> serviceIds;
        private final String notes;

        public Request(String appointmentId, List<String> serviceIds, String notes) {
            this.appointmentId = appointmentId;
            this.serviceIds = serviceIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(serviceIds);
            this.notes = notes;
        }
    }
}
