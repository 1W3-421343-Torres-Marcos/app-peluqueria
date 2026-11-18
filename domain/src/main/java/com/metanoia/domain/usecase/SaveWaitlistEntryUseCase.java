package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Da de alta o edita una entrada de lista de espera validando preferencias y evitando duplicados.
 */
public final class SaveWaitlistEntryUseCase {

    private final WaitlistRepository waitlistRepository;
    private final ClientRepository clientRepository;
    private final SalonServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SaveWaitlistEntryUseCase(
            WaitlistRepository waitlistRepository,
            ClientRepository clientRepository,
            SalonServiceRepository serviceRepository,
            ProfessionalRepository professionalRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.waitlistRepository = Objects.requireNonNull(
                waitlistRepository, "waitlistRepository");
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<WaitlistEntry> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        String clientId = trimToNull(request.clientId);
        if (clientId == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Seleccioná un cliente activo");
        }
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (!clientOpt.isPresent() || !clientOpt.get().isActive()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El cliente debe existir y estar activo");
        }

        List<String> normalizedServices = normalizeServiceIds(request.serviceIds);
        if (normalizedServices.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Seleccioná al menos un servicio");
        }
        for (String serviceId : normalizedServices) {
            Optional<SalonService> found = serviceRepository.findById(serviceId);
            if (!found.isPresent() || !found.get().isActive()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Los servicios elegidos deben estar activos");
            }
        }

        String professionalId = trimToNull(request.professionalId);
        if (professionalId != null) {
            Optional<Professional> proOpt = professionalRepository.findById(professionalId);
            if (!proOpt.isPresent() || !proOpt.get().isActive()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El profesional seleccionado debe estar activo");
            }
            if (!proOpt.get().serviceIds().containsAll(normalizedServices)) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El profesional elegido no realiza todos los servicios pedidos");
            }
        }

        long today = clock.todayEpochDay();
        Long specificDay = request.specificDay;
        Set<Integer> weekdays = normalizeWeekdays(request.preferredDaysOfWeek);
        if (weekdays == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Días preferidos inválidos");
        }
        if (specificDay == null && weekdays.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Indicá una fecha puntual o al menos un día de la semana");
        }
        if (specificDay != null && specificDay < today) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La fecha puntual no puede ser anterior a hoy");
        }
        if (request.fromMin < 0 || request.toMin > WaitlistEntry.MINUTES_PER_DAY
                || request.fromMin >= request.toMin) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La franja horaria desde/hasta es inválida");
        }

        Settings settings = settingsRepository.getSettings();
        long validUntil = request.validUntilDay != null
                ? request.validUntilDay
                : today + settings.waitlistExpiryDays();
        if (validUntil < today) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La fecha de vencimiento no puede ser anterior a hoy");
        }

        String notes = request.notes == null ? "" : request.notes.trim();
        if (notes.length() > WaitlistEntry.MAX_NOTES_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Las notas exceden los 300 caracteres");
        }

        String requestedId = trimToNull(request.id);
        boolean isEdit = requestedId != null;
        WaitlistEntry existing = null;
        if (isEdit) {
            Optional<WaitlistEntry> found = waitlistRepository.findById(requestedId);
            if (!found.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND, "Entrada no encontrada");
            }
            existing = found.get();
            if (!existing.status().isOpen()) {
                return Result.failure(DomainError.CONFLICT,
                        "Solo se pueden editar entradas activas u ofrecidas");
            }
        }

        long now = clock.nowMillis();
        String entryId = isEdit ? existing.id() : idGenerator.nextId();
        WaitlistEntry candidate = new WaitlistEntry.Builder()
                .id(entryId)
                .clientId(clientOpt.get().id())
                .serviceIds(normalizedServices)
                .professionalId(professionalId)
                .specificDay(specificDay)
                .preferredDaysOfWeek(specificDay != null ? Collections.emptySet() : weekdays)
                .fromMin(request.fromMin)
                .toMin(request.toMin)
                .validUntilDay(validUntil)
                .status(isEdit ? existing.status() : WaitlistStatus.ACTIVE)
                .notes(notes)
                .createdAt(isEdit ? existing.createdAt() : now)
                .offeredAt(isEdit ? existing.offeredAt().orElse(null) : null)
                .build();

        List<WaitlistEntry> openEntries = waitlistRepository.findOpenByClientId(clientOpt.get().id());
        for (WaitlistEntry open : openEntries) {
            if (!open.id().equals(candidate.id()) && open.hasSamePreferenceAs(candidate)) {
                return Result.failure(DomainError.CONFLICT,
                        "El cliente ya tiene una entrada activa con las mismas preferencias");
            }
        }

        waitlistRepository.save(candidate);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action(isEdit ? "WAITLIST_UPDATE" : "WAITLIST_CREATE")
                .entityType("WAITLIST")
                .entityId(candidate.id())
                .concept(isEdit ? "Edición de lista de espera" : "Alta en lista de espera")
                .build());
        return Result.success(candidate);
    }

    private static List<String> normalizeServiceIds(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String id : raw) {
            String clean = trimToNull(id);
            if (clean != null) {
                unique.add(clean);
            }
        }
        return new ArrayList<>(unique);
    }

    private static Set<Integer> normalizeWeekdays(Set<Integer> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> days = new TreeSet<>();
        for (Integer day : raw) {
            if (day == null || day < 1 || day > 7) {
                return null;
            }
            days.add(day);
        }
        return days;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Request {
        private final String id;
        private final String clientId;
        private final List<String> serviceIds;
        private final String professionalId;
        private final Long specificDay;
        private final Set<Integer> preferredDaysOfWeek;
        private final int fromMin;
        private final int toMin;
        private final Long validUntilDay;
        private final String notes;

        public Request(
                String id,
                String clientId,
                List<String> serviceIds,
                String professionalId,
                Long specificDay,
                Set<Integer> preferredDaysOfWeek,
                int fromMin,
                int toMin,
                Long validUntilDay,
                String notes
        ) {
            this.id = id;
            this.clientId = clientId;
            this.serviceIds = serviceIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(serviceIds);
            this.professionalId = professionalId;
            this.specificDay = specificDay;
            this.preferredDaysOfWeek = preferredDaysOfWeek == null
                    ? Collections.emptySet()
                    : new LinkedHashSet<>(preferredDaysOfWeek);
            this.fromMin = fromMin;
            this.toMin = toMin;
            this.validUntilDay = validUntilDay;
            this.notes = notes;
        }
    }
}
