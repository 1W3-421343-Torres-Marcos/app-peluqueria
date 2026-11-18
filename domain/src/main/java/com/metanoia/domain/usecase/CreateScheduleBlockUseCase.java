package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.ScheduleBlockType;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Crea un bloqueo general o por profesional y detecta los turnos existentes afectados por él.
 */
public final class CreateScheduleBlockUseCase {

    private static final int MIN_REASON_LENGTH = 2;
    private static final int MAX_REASON_LENGTH = 120;
    private static final int MINUTES_PER_DAY = 1440;

    private final ScheduleBlockRepository blockRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CreateScheduleBlockUseCase(
            ScheduleBlockRepository blockRepository,
            AppointmentRepository appointmentRepository,
            ProfessionalRepository professionalRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.blockRepository = Objects.requireNonNull(blockRepository, "blockRepository");
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<CreateResult> execute(Request request) {
        if (request == null || request.type == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Datos de bloqueo inválidos");
        }
        if (request.startDay > request.endDay) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La fecha desde no puede ser posterior a la fecha hasta");
        }
        if ((request.startMin == null) != (request.endMin == null)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Debe indicarse hora de inicio y fin para bloqueo parcial");
        }
        if (request.startMin != null && (request.startMin < 0
                || request.endMin > MINUTES_PER_DAY || request.startMin >= request.endMin)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El rango horario del bloqueo es inválido");
        }
        String cleanReason = request.reason == null ? "" : request.reason.trim();
        if (cleanReason.length() < MIN_REASON_LENGTH
                || cleanReason.length() > MAX_REASON_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El motivo debe tener entre 2 y 120 caracteres");
        }

        String proId = (request.professionalId == null
                || request.professionalId.trim().isEmpty())
                ? null
                : request.professionalId.trim();
        if (proId != null && !professionalRepository.findById(proId).isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado");
        }

        long now = clock.nowMillis();
        ScheduleBlock block = new ScheduleBlock.Builder()
                .id(idGenerator.nextId())
                .professionalId(proId)
                .startDay(request.startDay)
                .endDay(request.endDay)
                .startMin(request.startMin)
                .endMin(request.endMin)
                .type(request.type)
                .reason(cleanReason)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        blockRepository.save(block);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settingsRepository.getSettings().currentOperator())
                .action("SCHEDULE_BLOCK_CREATE")
                .entityType("SCHEDULE_BLOCK")
                .entityId(block.id())
                .concept("Bloqueo: " + cleanReason)
                .build());

        List<Appointment> candidates = appointmentRepository.findByDayRange(
                block.startDay(), block.endDay());
        List<Appointment> affected = new ArrayList<>();
        for (Appointment appointment : candidates) {
            if (appointment.status().occupiesSchedule() && block.blocksAppointment(appointment)) {
                affected.add(appointment);
            }
        }
        return Result.success(new CreateResult(block, affected));
    }

    public static final class Request {
        private final String professionalId;
        private final long startDay;
        private final long endDay;
        private final Integer startMin;
        private final Integer endMin;
        private final ScheduleBlockType type;
        private final String reason;

        public Request(
                String professionalId,
                long startDay,
                long endDay,
                Integer startMin,
                Integer endMin,
                ScheduleBlockType type,
                String reason
        ) {
            this.professionalId = professionalId;
            this.startDay = startDay;
            this.endDay = endDay;
            this.startMin = startMin;
            this.endMin = endMin;
            this.type = type;
            this.reason = reason;
        }
    }

    public static final class CreateResult {
        private final ScheduleBlock block;
        private final List<Appointment> affectedAppointments;

        public CreateResult(ScheduleBlock block, List<Appointment> affectedAppointments) {
            this.block = Objects.requireNonNull(block, "block");
            this.affectedAppointments = Collections.unmodifiableList(
                    new ArrayList<>(affectedAppointments));
        }

        public ScheduleBlock block() {
            return block;
        }

        public List<Appointment> affectedAppointments() {
            return affectedAppointments;
        }
    }
}
