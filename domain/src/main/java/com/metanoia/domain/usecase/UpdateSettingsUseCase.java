package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ScheduleValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Valida y actualiza la configuración del negocio, auditando cambios sensibles.
 */
public final class UpdateSettingsUseCase {

    private static final int MIN_BUSINESS_NAME_LEN = 2;
    private static final int MAX_BUSINESS_NAME_LEN = 80;

    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public UpdateSettingsUseCase(
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.settingsRepository = Objects.requireNonNull(
                settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(
                auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Settings> execute(Settings candidate) {
        if (candidate == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Configuración nula");
        }
        String businessName = candidate.businessName().trim();
        if (businessName.length() < MIN_BUSINESS_NAME_LEN
                || businessName.length() > MAX_BUSINESS_NAME_LEN) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El nombre del negocio debe tener entre 2 y 80 caracteres");
        }
        if (!ScheduleValidator.isValidSlotMinutes(candidate.slotMinutes())) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La duración de bloque debe ser 10, 15, 20 o 30 minutos");
        }
        if (candidate.waitlistExpiryDays() <= 0) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El vencimiento de lista de espera debe ser mayor a cero");
        }
        List<String> cleanOperators = sanitizeOperators(candidate.operators());
        if (cleanOperators.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Debe existir al menos un operador");
        }
        String currentOperator = candidate.currentOperator().trim();
        if (!cleanOperators.contains(currentOperator)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El operador actual debe formar parte de la lista de operadores");
        }
        if (!ScheduleValidator.validateDayRanges(candidate.generalSchedule())) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El horario general contiene tramos inválidos o solapados");
        }
        String reminderTpl = trimSafe(candidate.reminderTemplate());
        String birthdayTpl = trimSafe(candidate.birthdayTemplate());
        String waitlistTpl = trimSafe(candidate.waitlistOfferTemplate());
        if (reminderTpl.isEmpty() || birthdayTpl.isEmpty() || waitlistTpl.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Las plantillas de mensajes no pueden quedar vacías");
        }

        List<TimeRange> sortedSchedule = new ArrayList<>(candidate.generalSchedule());
        Collections.sort(sortedSchedule);

        Settings normalized = candidate.toBuilder()
                .businessName(businessName)
                .subtitle(candidate.subtitle().trim())
                .address(candidate.address().trim())
                .phone(candidate.phone().trim())
                .cuit(candidate.cuit().trim())
                .currentOperator(currentOperator)
                .operators(cleanOperators)
                .generalSchedule(sortedSchedule)
                .reminderTemplate(reminderTpl)
                .birthdayTemplate(birthdayTpl)
                .waitlistOfferTemplate(waitlistTpl)
                .build();

        Settings previous = settingsRepository.getSettings();
        settingsRepository.saveSettings(normalized);
        recordAuditIfRelevant(previous, normalized);
        return Result.success(normalized);
    }

    private List<String> sanitizeOperators(List<String> raw) {
        Set<String> unique = new LinkedHashSet<>();
        for (String op : raw) {
            if (op == null) {
                continue;
            }
            String clean = op.replace(",", "").trim();
            if (!clean.isEmpty()) {
                unique.add(clean);
            }
        }
        return new ArrayList<>(unique);
    }

    private static String trimSafe(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private void recordAuditIfRelevant(Settings previous, Settings updated) {
        boolean scheduleChanged = !previous.generalSchedule().equals(updated.generalSchedule());
        boolean slotChanged = previous.slotMinutes() != updated.slotMinutes();
        boolean operatorChanged = !previous.currentOperator().equals(updated.currentOperator())
                || !previous.operators().equals(updated.operators());
        boolean templatesChanged = !previous.reminderTemplate().equals(updated.reminderTemplate())
                || !previous.birthdayTemplate().equals(updated.birthdayTemplate())
                || !previous.waitlistOfferTemplate().equals(updated.waitlistOfferTemplate());
        if (!scheduleChanged && !slotChanged && !operatorChanged && !templatesChanged) {
            return;
        }
        StringBuilder concept = new StringBuilder("Actualización de configuración:");
        if (scheduleChanged) {
            concept.append(" horarios");
        }
        if (slotChanged) {
            concept.append(" bloque(").append(updated.slotMinutes()).append("m)");
        }
        if (operatorChanged) {
            concept.append(" operadores");
        }
        if (templatesChanged) {
            concept.append(" plantillas");
        }
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(clock.nowMillis())
                .operator(updated.currentOperator())
                .action("UPDATE")
                .entityType("SETTINGS")
                .entityId("default")
                .concept(concept.toString())
                .build());
    }
}
