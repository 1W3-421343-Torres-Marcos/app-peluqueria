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
import com.metanoia.domain.model.RecurrenceRule;
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
import com.metanoia.domain.rules.AvailabilityChecker.UnavailabilityReason;
import com.metanoia.domain.rules.ColorFormulaRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Genera una serie recurrente cada N semanas hasta fecha o cantidad límite,
 * creando solo los turnos válidos y reportando las ocurrencias omitidas por conflicto.
 */
public final class GenerateRecurringAppointmentsUseCase {

    private static final long DAYS_PER_WEEK = 7L;

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

    public GenerateRecurringAppointmentsUseCase(
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

    public GenerateRecurringAppointmentsUseCase(
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

    public Result<RecurringResult> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        if (request.intervalWeeks < 1 || request.intervalWeeks > 12) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El intervalo en semanas debe estar entre 1 y 12");
        }
        if (request.untilDay == null && request.maxOccurrences == null) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Indicá fecha límite o cantidad de repeticiones");
        }
        if (request.maxOccurrences != null && (request.maxOccurrences < 1
                || request.maxOccurrences > RecurrenceRule.MAX_OCCURRENCES_LIMIT)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La cantidad de repeticiones debe estar entre 1 y 52");
        }
        if (request.untilDay != null && request.untilDay < request.startDay) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La fecha límite no puede ser anterior al primer turno");
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

        Result<List<AppointmentItem>> templateResult = AppointmentSupport.buildFrozenItems(
                "",
                request.serviceIds,
                Collections.emptyList(),
                professional,
                serviceRepository,
                idGenerator
        );
        if (templateResult.isFailure()) {
            return Result.failure(templateResult.getError(), templateResult.getErrorMessage());
        }

        Settings settings = settingsRepository.getSettings();
        int endMin = Appointment.calculateEndMin(
                request.startMin, templateResult.getValue(), settings.slotMinutes());

        Set<String> colorServiceIds = Collections.emptySet();
        ColorFormula latestFormula = null;
        if (request.repeatLastColorFormula) {
            if (colorFormulaRepository == null) {
                return Result.failure(DomainError.UNEXPECTED,
                        "Repositorio de fichas de color no configurado");
            }
            colorServiceIds = AppointmentSupport.collectColorServiceIds(
                    templateResult.getValue(), serviceRepository);
            if (colorServiceIds.isEmpty()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Seleccioná un servicio de coloración para repetir una fórmula");
            }
            Optional<ColorFormula> latestOpt = colorFormulaRepository.findLatestActiveByClientId(
                    clientOpt.get().id());
            if (!latestOpt.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND,
                        "El cliente no tiene fórmulas de coloración previas");
            }
            latestFormula = latestOpt.get();
        }

        List<Long> candidateDays = buildCandidateDays(
                request.startDay, request.intervalWeeks, request.untilDay, request.maxOccurrences);
        if (candidateDays.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Sin ocurrencias para generar");
        }

        long firstDay = candidateDays.get(0);
        long lastDay = candidateDays.get(candidateDays.size() - 1);
        List<Appointment> existingInRange = appointmentRepository.findByDayRange(firstDay, lastDay);
        List<ScheduleBlock> blocksInRange = scheduleBlockRepository.findActiveOverlappingRange(
                firstDay, lastDay);

        long now = clock.nowMillis();
        RecurrenceRule rule = new RecurrenceRule(
                idGenerator.nextId(),
                request.intervalWeeks,
                request.untilDay,
                request.maxOccurrences,
                now
        );

        AppointmentStatus initialStatus = request.confirmed
                ? AppointmentStatus.CONFIRMED
                : AppointmentStatus.PENDING;
        List<Appointment> created = new ArrayList<>();
        List<ColorFormula> copiedFormulas = new ArrayList<>();
        List<SkippedOccurrence> conflicts = new ArrayList<>();
        List<SkippedOccurrence> overlaps = new ArrayList<>();

        for (long candidateDay : candidateDays) {
            UnavailabilityReason reason = AvailabilityChecker.check(
                    professional,
                    candidateDay,
                    request.startMin,
                    endMin,
                    settings.generalSchedule(),
                    existingInRange,
                    blocksInRange,
                    null
            );
            if (reason.isAppointmentOverlap()) {
                overlaps.add(new SkippedOccurrence(candidateDay, reason));
            } else if (!reason.isAvailable()) {
                conflicts.add(new SkippedOccurrence(candidateDay, reason));
                continue;
            }
            String aptId = idGenerator.nextId();
            List<AppointmentItem> items = cloneItemsForAppointment(
                    aptId, templateResult.getValue());
            if (latestFormula != null) {
                ColorFormula draft = ColorFormulaRules.duplicateAsNewDraft(
                        latestFormula,
                        idGenerator.nextId(),
                        aptId,
                        professional.id(),
                        candidateDay,
                        now,
                        idGenerator
                );
                items = AppointmentSupport.attachColorFormulaToColorItems(
                        items, colorServiceIds, draft.id());
                copiedFormulas.add(draft);
            }
            Appointment appointment = new Appointment.Builder()
                    .id(aptId)
                    .clientId(clientOpt.get().id())
                    .professionalId(professional.id())
                    .day(candidateDay)
                    .startMin(request.startMin)
                    .endMin(endMin)
                    .status(initialStatus)
                    .notes(request.notes == null ? "" : request.notes.trim())
                    .recurrenceRuleId(rule.id())
                    .items(items)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(settings.currentOperator())
                    .build();
            created.add(appointment);
            existingInRange.add(appointment);
        }

        if (created.isEmpty()) {
            return Result.failure(DomainError.CONFLICT,
                    AppointmentSupport.unavailabilityMessage(conflicts.get(0).reason()));
        }

        appointmentRepository.saveRecurrenceRule(rule);
        appointmentRepository.saveAll(created);
        for (ColorFormula draft : copiedFormulas) {
            colorFormulaRepository.save(draft);
        }
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("APPOINTMENT_RECURRING_CREATE")
                .entityType("RECURRENCE_RULE")
                .entityId(rule.id())
                .concept("Serie recurrente (" + created.size() + " turnos)")
                .build());

        return Result.success(new RecurringResult(rule, created, conflicts, overlaps));
    }

    private List<AppointmentItem> cloneItemsForAppointment(
            String appointmentId,
            List<AppointmentItem> templateItems
    ) {
        List<AppointmentItem> items = new ArrayList<>(templateItems.size());
        for (AppointmentItem template : templateItems) {
            items.add(template.toBuilder()
                    .id(idGenerator.nextId())
                    .appointmentId(appointmentId)
                    .build());
        }
        return items;
    }

    private static List<Long> buildCandidateDays(
            long startDay,
            int intervalWeeks,
            Long untilDay,
            Integer maxOccurrences
    ) {
        int limit = maxOccurrences != null
                ? maxOccurrences
                : RecurrenceRule.MAX_OCCURRENCES_LIMIT;
        long stepDays = (long) intervalWeeks * DAYS_PER_WEEK;
        List<Long> days = new ArrayList<>();
        for (int index = 0; index < limit; index++) {
            long day = startDay + index * stepDays;
            if (untilDay != null && day > untilDay) {
                break;
            }
            days.add(day);
        }
        return days;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class Request {
        private final String clientId;
        private final String professionalId;
        private final long startDay;
        private final int startMin;
        private final List<String> serviceIds;
        private final String notes;
        private final boolean confirmed;
        private final int intervalWeeks;
        private final Long untilDay;
        private final Integer maxOccurrences;
        private final boolean repeatLastColorFormula;

        public Request(
                String clientId,
                String professionalId,
                long startDay,
                int startMin,
                List<String> serviceIds,
                String notes,
                boolean confirmed,
                int intervalWeeks,
                Long untilDay,
                Integer maxOccurrences
        ) {
            this(clientId, professionalId, startDay, startMin, serviceIds, notes, confirmed,
                    intervalWeeks, untilDay, maxOccurrences, false);
        }

        public Request(
                String clientId,
                String professionalId,
                long startDay,
                int startMin,
                List<String> serviceIds,
                String notes,
                boolean confirmed,
                int intervalWeeks,
                Long untilDay,
                Integer maxOccurrences,
                boolean repeatLastColorFormula
        ) {
            this.clientId = clientId;
            this.professionalId = professionalId;
            this.startDay = startDay;
            this.startMin = startMin;
            this.serviceIds = serviceIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(serviceIds);
            this.notes = notes;
            this.confirmed = confirmed;
            this.intervalWeeks = intervalWeeks;
            this.untilDay = untilDay;
            this.maxOccurrences = maxOccurrences;
            this.repeatLastColorFormula = repeatLastColorFormula;
        }
    }

    public static final class SkippedOccurrence {
        private final long dayEpoch;
        private final UnavailabilityReason reason;

        public SkippedOccurrence(long dayEpoch, UnavailabilityReason reason) {
            this.dayEpoch = dayEpoch;
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        public long dayEpoch() {
            return dayEpoch;
        }

        public UnavailabilityReason reason() {
            return reason;
        }
    }

    public static final class RecurringResult {
        private final RecurrenceRule rule;
        private final List<Appointment> createdAppointments;
        private final List<SkippedOccurrence> conflicts;
        private final List<SkippedOccurrence> overlaps;

        public RecurringResult(
                RecurrenceRule rule,
                List<Appointment> createdAppointments,
                List<SkippedOccurrence> conflicts
        ) {
            this(rule, createdAppointments, conflicts, Collections.emptyList());
        }

        public RecurringResult(
                RecurrenceRule rule,
                List<Appointment> createdAppointments,
                List<SkippedOccurrence> conflicts,
                List<SkippedOccurrence> overlaps
        ) {
            this.rule = Objects.requireNonNull(rule, "rule");
            this.createdAppointments = Collections.unmodifiableList(
                    new ArrayList<>(createdAppointments));
            this.conflicts = Collections.unmodifiableList(new ArrayList<>(conflicts));
            this.overlaps = Collections.unmodifiableList(new ArrayList<>(overlaps));
        }

        public RecurrenceRule rule() {
            return rule;
        }

        public List<Appointment> createdAppointments() {
            return createdAppointments;
        }

        public List<SkippedOccurrence> conflicts() {
            return conflicts;
        }

        public List<SkippedOccurrence> overlaps() {
            return overlaps;
        }
    }
}
