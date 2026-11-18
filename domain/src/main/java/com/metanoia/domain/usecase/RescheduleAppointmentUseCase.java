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
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.AppointmentSupport;
import com.metanoia.domain.rules.AvailabilityChecker;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reprograma día, horario y/o profesional de un turno editable revalidando disponibilidad
 * y manteniendo ID y snapshots de ítems.
 */
public final class RescheduleAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public RescheduleAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            ProfessionalRepository professionalRepository,
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
                    "Solo se pueden reprogramar turnos pendientes o confirmados");
        }

        String targetProId = (request.professionalId == null
                || request.professionalId.trim().isEmpty())
                ? existing.professionalId()
                : request.professionalId.trim();
        Optional<Professional> proOpt = professionalRepository.findById(targetProId);
        if (!proOpt.isPresent() || !proOpt.get().isActive()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Debe elegirse un profesional activo");
        }
        Professional targetPro = proOpt.get();
        for (AppointmentItem item : existing.items()) {
            if (!targetPro.serviceIds().contains(item.serviceId())) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El profesional elegido no realiza todos los servicios del turno");
            }
        }

        Settings settings = settingsRepository.getSettings();
        int newEndMin = Appointment.calculateEndMin(
                request.dayStartMin, existing.items(), settings.slotMinutes());

        List<Appointment> existingOnDay = appointmentRepository.findByDayAndProfessional(
                request.dayEpoch, targetPro.id());
        List<ScheduleBlock> blocksOnDay = scheduleBlockRepository.findActiveOverlappingRange(
                request.dayEpoch, request.dayEpoch);

        AvailabilityChecker.UnavailabilityReason availability = AvailabilityChecker.check(
                targetPro,
                request.dayEpoch,
                request.dayStartMin,
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
                .professionalId(targetPro.id())
                .day(request.dayEpoch)
                .startMin(request.dayStartMin)
                .endMin(newEndMin)
                .updatedAt(now)
                .build();

        appointmentRepository.save(updated);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("APPOINTMENT_RESCHEDULE")
                .entityType("APPOINTMENT")
                .entityId(updated.id())
                .concept("Reprogramación de turno")
                .build());
        return Result.success(updated);
    }

    public static final class Request {
        private final String appointmentId;
        private final String professionalId;
        private final long dayEpoch;
        private final int dayStartMin;

        public Request(
                String appointmentId,
                String professionalId,
                long dayEpoch,
                int dayStartMin
        ) {
            this.appointmentId = appointmentId;
            this.professionalId = professionalId;
            this.dayEpoch = dayEpoch;
            this.dayStartMin = dayStartMin;
        }
    }
}
