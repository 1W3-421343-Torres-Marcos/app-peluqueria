package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;

import java.util.Objects;
import java.util.Optional;

/**
 * Asigna un hueco liberado creando el turno vía {@link CreateAppointmentUseCase}
 * (revalidando disponibilidad) y pasando la entrada de lista de espera a {@code FULFILLED}.
 */
public final class AssignWaitlistEntryUseCase {

    private final WaitlistRepository waitlistRepository;
    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public AssignWaitlistEntryUseCase(
            WaitlistRepository waitlistRepository,
            CreateAppointmentUseCase createAppointmentUseCase,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.waitlistRepository = Objects.requireNonNull(
                waitlistRepository, "waitlistRepository");
        this.createAppointmentUseCase = Objects.requireNonNull(
                createAppointmentUseCase, "createAppointmentUseCase");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<AssignResult> execute(String entryId, FreedSlot slot) {
        if (entryId == null || entryId.trim().isEmpty() || slot == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud inválida");
        }
        Optional<WaitlistEntry> found = waitlistRepository.findById(entryId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Entrada no encontrada");
        }
        WaitlistEntry entry = found.get();
        if (!entry.status().isOpen()) {
            return Result.failure(DomainError.CONFLICT,
                    "La entrada ya no está disponible para asignar");
        }

        Result<Appointment> createdResult = createAppointmentUseCase.execute(
                new CreateAppointmentUseCase.Request(
                        entry.clientId(),
                        slot.professionalId(),
                        slot.dayEpoch(),
                        slot.startMin(),
                        entry.serviceIds(),
                        entry.notes(),
                        true
                ));
        if (createdResult.isFailure()) {
            return Result.failure(createdResult.getError(), createdResult.getErrorMessage());
        }

        Appointment createdAppointment = createdResult.getValue();
        WaitlistEntry fulfilled = entry.toBuilder()
                .status(WaitlistStatus.FULFILLED)
                .build();
        waitlistRepository.save(fulfilled);

        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(clock.nowMillis())
                .operator(settings.currentOperator())
                .action("WAITLIST_FULFILL")
                .entityType("WAITLIST")
                .entityId(fulfilled.id())
                .concept("Turno asignado desde lista de espera: " + createdAppointment.id())
                .build());
        return Result.success(new AssignResult(createdAppointment, fulfilled));
    }

    public static final class AssignResult {
        private final Appointment appointment;
        private final WaitlistEntry entry;

        public AssignResult(Appointment appointment, WaitlistEntry entry) {
            this.appointment = Objects.requireNonNull(appointment, "appointment");
            this.entry = Objects.requireNonNull(entry, "entry");
        }

        public Appointment appointment() {
            return appointment;
        }

        public WaitlistEntry entry() {
            return entry;
        }
    }
}
