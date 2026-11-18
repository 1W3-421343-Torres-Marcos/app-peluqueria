package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ClientValidator;
import com.metanoia.domain.rules.PhoneNormalizer;
import com.metanoia.domain.rules.TextNormalizer;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Normaliza, valida y guarda (alta o edición) un cliente verificando teléfono duplicado entre activos.
 * Ante duplicado devuelve {@link DomainError#CONFLICT} con el ID del cliente existente como detalle.
 */
public final class SaveClientUseCase {

    private final ClientRepository clientRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SaveClientUseCase(
            ClientRepository clientRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Client> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        if (!ClientValidator.isValidName(request.name)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El nombre debe tener entre 2 y 80 caracteres");
        }
        String normalizedPhone = PhoneNormalizer.normalize(request.phone);
        if (!ClientValidator.isValidPhone(normalizedPhone)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Ingresá un teléfono válido con código de área");
        }
        int currentYear = LocalDate.ofEpochDay(clock.todayEpochDay()).getYear();
        if (!ClientValidator.isValidBirthday(
                request.birthDay, request.birthMonth, request.birthYear, currentYear)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La fecha de cumpleaños no es válida");
        }
        if (!ClientValidator.isValidNotes(request.notes)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Las notas no pueden superar los 500 caracteres");
        }

        boolean isEdit = request.id != null && !request.id.trim().isEmpty();
        Client existing = null;
        if (isEdit) {
            Optional<Client> found = clientRepository.findById(request.id.trim());
            if (!found.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND, "Cliente no encontrado");
            }
            existing = found.get();
        }

        if (request.active) {
            Optional<Client> duplicate = clientRepository.findActiveByPhone(normalizedPhone);
            if (duplicate.isPresent()
                    && (!isEdit || !duplicate.get().id().equals(existing.id()))) {
                return Result.failure(DomainError.CONFLICT, duplicate.get().id());
            }
        }

        String cleanName = request.name.trim().replaceAll("\\s+", " ");
        long now = clock.nowMillis();
        Client client = new Client.Builder()
                .id(isEdit ? existing.id() : idGenerator.nextId())
                .name(cleanName)
                .nameNormalized(TextNormalizer.normalize(cleanName))
                .phone(normalizedPhone)
                .birthDay(request.birthDay)
                .birthMonth(request.birthMonth)
                .birthYear(request.birthYear)
                .notes(request.notes == null ? "" : request.notes.trim())
                .noShowCount(isEdit ? existing.noShowCount() : 0)
                .active(request.active)
                .createdAt(isEdit ? existing.createdAt() : now)
                .updatedAt(now)
                .build();

        clientRepository.save(client);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settingsRepository.getSettings().currentOperator())
                .action(isEdit ? "CLIENT_UPDATE" : "CLIENT_CREATE")
                .entityType("CLIENT")
                .entityId(client.id())
                .concept(isEdit ? "Edición de cliente" : "Alta de cliente")
                .build());
        return Result.success(client);
    }

    public static final class Request {
        private final String id;
        private final String name;
        private final String phone;
        private final Integer birthDay;
        private final Integer birthMonth;
        private final Integer birthYear;
        private final String notes;
        private final boolean active;

        public Request(
                String id,
                String name,
                String phone,
                Integer birthDay,
                Integer birthMonth,
                Integer birthYear,
                String notes,
                boolean active
        ) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.birthDay = birthDay;
            this.birthMonth = birthMonth;
            this.birthYear = birthYear;
            this.notes = notes;
            this.active = active;
        }
    }
}
