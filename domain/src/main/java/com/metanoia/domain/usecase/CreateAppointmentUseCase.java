package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.AppointmentSupport;
import com.metanoia.domain.rules.AvailabilityChecker;
import com.metanoia.domain.rules.ColorFormulaRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Crea un turno congelando nombre, precio y duración de los servicios y validando disponibilidad.
 */
public final class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final SalonServiceRepository serviceRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final ColorFormulaRepository colorFormulaRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CreateAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            ClientRepository clientRepository,
            ProfessionalRepository professionalRepository,
            SalonServiceRepository serviceRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this(
                appointmentRepository,
                clientRepository,
                professionalRepository,
                serviceRepository,
                scheduleBlockRepository,
                settingsRepository,
                auditLogRepository,
                null,
                clock,
                idGenerator
        );
    }

    public CreateAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            ClientRepository clientRepository,
            ProfessionalRepository professionalRepository,
            SalonServiceRepository serviceRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            ColorFormulaRepository colorFormulaRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.scheduleBlockRepository = Objects.requireNonNull(
                scheduleBlockRepository, "scheduleBlockRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.colorFormulaRepository = colorFormulaRepository;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Appointment> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        Optional<Client> clientOpt = isBlank(request.clientId)
                ? Optional.empty()
                : clientRepository.findById(request.clientId.trim());
        if (!clientOpt.isPresent() || !clientOpt.get().isActive()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Debe seleccionarse un cliente activo");
        }
        Optional<Professional> proOpt = isBlank(request.professionalId)
                ? Optional.empty()
                : professionalRepository.findById(request.professionalId.trim());
        if (!proOpt.isPresent() || !proOpt.get().isActive()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Debe seleccionarse un profesional activo");
        }
        Professional professional = proOpt.get();

        String appointmentId = idGenerator.nextId();
        Result<List<AppointmentItem>> itemsResult = AppointmentSupport.buildFrozenItems(
                appointmentId,
                request.serviceIds,
                Collections.emptyList(),
                professional,
                serviceRepository,
                idGenerator
        );
        if (itemsResult.isFailure()) {
            return Result.failure(itemsResult.getError(), itemsResult.getErrorMessage());
        }

        Settings settings = settingsRepository.getSettings();
        int endMin = Appointment.calculateEndMin(
                request.startMin, itemsResult.getValue(), settings.slotMinutes());

        List<Appointment> existingOnDay = appointmentRepository.findByDayAndProfessional(
                request.day, professional.id());
        List<ScheduleBlock> blocksOnDay = scheduleBlockRepository.findActiveOverlappingRange(
                request.day, request.day);

        AvailabilityChecker.UnavailabilityReason availability = AvailabilityChecker.check(
                professional,
                request.day,
                request.startMin,
                endMin,
                settings.generalSchedule(),
                existingOnDay,
                blocksOnDay,
                null
        );
        if (!availability.isAvailable()) {
            return Result.failure(DomainError.CONFLICT,
                    AppointmentSupport.unavailabilityMessage(availability));
        }

        long now = clock.nowMillis();
        List<AppointmentItem> finalItems = itemsResult.getValue();
        ColorFormula copiedFormula = null;
        if (request.repeatLastColorFormula) {
            if (colorFormulaRepository == null) {
                return Result.failure(DomainError.UNEXPECTED,
                        "Repositorio de fichas de color no configurado");
            }
            Set<String> colorServiceIds = AppointmentSupport.collectColorServiceIds(
                    finalItems, serviceRepository);
            if (colorServiceIds.isEmpty()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Seleccioná un servicio de coloración para repetir una fórmula");
            }
            Optional<ColorFormula> latest = colorFormulaRepository.findLatestActiveByClientId(
                    clientOpt.get().id());
            if (!latest.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND,
                        "El cliente no tiene fórmulas de coloración previas");
            }
            copiedFormula = ColorFormulaRules.duplicateAsNewDraft(
                    latest.get(),
                    idGenerator.nextId(),
                    appointmentId,
                    professional.id(),
                    request.day,
                    now,
                    idGenerator
            );
            finalItems = AppointmentSupport.attachColorFormulaToColorItems(
                    finalItems, colorServiceIds, copiedFormula.id());
        }

        AppointmentStatus initialStatus = request.confirmed
                ? AppointmentStatus.CONFIRMED
                : AppointmentStatus.PENDING;
        Appointment created = new Appointment.Builder()
                .id(appointmentId)
                .clientId(clientOpt.get().id())
                .professionalId(professional.id())
                .day(request.day)
                .startMin(request.startMin)
                .endMin(endMin)
                .status(initialStatus)
                .notes(request.notes == null ? "" : request.notes.trim())
                .items(finalItems)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(settings.currentOperator())
                .build();

        appointmentRepository.save(created);
        if (copiedFormula != null) {
            colorFormulaRepository.save(copiedFormula);
        }
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("APPOINTMENT_CREATE")
                .entityType("APPOINTMENT")
                .entityId(created.id())
                .amountCents(created.totalAmount().cents())
                .concept("Alta de turno")
                .build());
        return Result.success(created);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class Request {
        private final String clientId;
        private final String professionalId;
        private final long day;
        private final int startMin;
        private final List<String> serviceIds;
        private final String notes;
        private final boolean confirmed;
        private final boolean repeatLastColorFormula;

        public Request(
                String clientId,
                String professionalId,
                long day,
                int startMin,
                List<String> serviceIds,
                String notes,
                boolean confirmed
        ) {
            this(clientId, professionalId, day, startMin, serviceIds, notes, confirmed, false);
        }

        public Request(
                String clientId,
                String professionalId,
                long day,
                int startMin,
                List<String> serviceIds,
                String notes,
                boolean confirmed,
                boolean repeatLastColorFormula
        ) {
            this.clientId = clientId;
            this.professionalId = professionalId;
            this.day = day;
            this.startMin = startMin;
            this.serviceIds = serviceIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(serviceIds);
            this.notes = notes;
            this.confirmed = confirmed;
            this.repeatLastColorFormula = repeatLastColorFormula;
        }
    }
}
