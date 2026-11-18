package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ColorFormulaRules;
import com.metanoia.domain.rules.ColorFormulaRules.ComponentDraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Crea o actualiza una fórmula de coloración, respetando la inmutabilidad si pertenece a un turno cerrado.
 */
public final class SaveColorFormulaUseCase {

    private final ColorFormulaRepository colorFormulaRepository;
    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SaveColorFormulaUseCase(
            ColorFormulaRepository colorFormulaRepository,
            ClientRepository clientRepository,
            ProfessionalRepository professionalRepository,
            AppointmentRepository appointmentRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.colorFormulaRepository = Objects.requireNonNull(
                colorFormulaRepository, "colorFormulaRepository");
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<ColorFormula> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        String clientId = trimToNull(request.clientId);
        if (clientId == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Cliente obligatorio");
        }
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (!clientOpt.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Cliente no encontrado");
        }

        String professionalId = trimToNull(request.professionalId);
        if (professionalId != null) {
            Optional<Professional> proOpt = professionalRepository.findById(professionalId);
            if (!proOpt.isPresent()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El profesional seleccionado no existe");
            }
        }

        if (request.exposureMin < ColorFormula.MIN_EXPOSURE_MIN
                || request.exposureMin > ColorFormula.MAX_EXPOSURE_MIN) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El tiempo de exposición debe estar entre 0 y 240 minutos");
        }
        String developerVolume = trimToNull(request.developerVolume);
        if (developerVolume != null
                && developerVolume.length() > ColorFormula.MAX_DEVELOPER_VOLUME_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El volumen de oxidante excede los 40 caracteres");
        }
        String notes = request.notes == null ? "" : request.notes.trim();
        if (notes.length() > ColorFormula.MAX_NOTES_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Las notas exceden los 500 caracteres");
        }
        String resultText = trimToNull(request.result);
        if (resultText != null && resultText.length() > ColorFormula.MAX_RESULT_LENGTH) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El resultado excede los 500 caracteres");
        }

        String requestedId = trimToNull(request.id);
        boolean isEdit = requestedId != null;
        ColorFormula existing = null;
        if (isEdit) {
            Optional<ColorFormula> found = colorFormulaRepository.findById(requestedId);
            if (!found.isPresent() || found.get().isDeleted()) {
                return Result.failure(DomainError.NOT_FOUND, "Fórmula no encontrada");
            }
            existing = found.get();
            if (isLocked(existing)) {
                return Result.failure(DomainError.CONFLICT,
                        "Las fórmulas de turnos completados o pagados son inmutables; duplicá la fórmula");
            }
        }

        long now = clock.nowMillis();
        String formulaId = isEdit ? existing.id() : idGenerator.nextId();
        Result<List<ColorFormulaComponent>> componentsResult =
                ColorFormulaRules.validateAndNormalizeComponents(
                        formulaId, request.components, idGenerator);
        if (componentsResult.isFailure()) {
            return Result.failure(
                    componentsResult.getError(), componentsResult.getErrorMessage());
        }

        String resolvedAppointmentId = trimToNull(request.appointmentId);
        if (resolvedAppointmentId == null && isEdit) {
            resolvedAppointmentId = existing.appointmentId().orElse(null);
        }
        long createdAt = isEdit ? existing.createdAt() : now;

        ColorFormula saved = new ColorFormula.Builder()
                .id(formulaId)
                .clientId(clientOpt.get().id())
                .appointmentId(resolvedAppointmentId)
                .professionalId(professionalId)
                .day(request.day)
                .exposureMin(request.exposureMin)
                .developerVolume(developerVolume)
                .notes(notes)
                .result(resultText)
                .deletedAt(null)
                .createdAt(createdAt)
                .components(componentsResult.getValue())
                .build();

        colorFormulaRepository.save(saved);
        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action(isEdit ? "COLOR_FORMULA_UPDATE" : "COLOR_FORMULA_CREATE")
                .entityType("COLOR_FORMULA")
                .entityId(saved.id())
                .concept(saved.mainShadesSummary())
                .build());
        return Result.success(saved);
    }

    private boolean isLocked(ColorFormula existing) {
        if (colorFormulaRepository.isLinkedToClosedAppointment(existing.id())) {
            return true;
        }
        if (existing.appointmentId().isPresent()) {
            Optional<Appointment> apt = appointmentRepository.findById(
                    existing.appointmentId().get());
            if (ColorFormulaRules.isLockedByAppointmentStatus(apt.map(Appointment::status))) {
                return true;
            }
        }
        return false;
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
        private final String appointmentId;
        private final String professionalId;
        private final long day;
        private final int exposureMin;
        private final String developerVolume;
        private final String notes;
        private final String result;
        private final List<ComponentDraft> components;

        public Request(
                String id,
                String clientId,
                String appointmentId,
                String professionalId,
                long day,
                int exposureMin,
                String developerVolume,
                String notes,
                String result,
                List<ComponentDraft> components
        ) {
            this.id = id;
            this.clientId = clientId;
            this.appointmentId = appointmentId;
            this.professionalId = professionalId;
            this.day = day;
            this.exposureMin = exposureMin;
            this.developerVolume = developerVolume;
            this.notes = notes;
            this.result = result;
            this.components = components == null
                    ? Collections.emptyList()
                    : new ArrayList<>(components);
        }
    }
}
