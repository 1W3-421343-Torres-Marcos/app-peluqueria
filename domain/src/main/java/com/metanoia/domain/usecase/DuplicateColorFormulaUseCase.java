package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ColorFormulaRules;

import java.util.Objects;
import java.util.Optional;

/**
 * Duplica una fórmula existente (incluida una cerrada) como nuevo borrador editable.
 */
public final class DuplicateColorFormulaUseCase {

    private final ColorFormulaRepository colorFormulaRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public DuplicateColorFormulaUseCase(
            ColorFormulaRepository colorFormulaRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.colorFormulaRepository = Objects.requireNonNull(
                colorFormulaRepository, "colorFormulaRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<ColorFormula> execute(Request request) {
        if (request == null || request.sourceFormulaId == null
                || request.sourceFormulaId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Fórmula origen inválida");
        }
        Optional<ColorFormula> found = colorFormulaRepository.findById(
                request.sourceFormulaId.trim());
        if (!found.isPresent() || found.get().isDeleted()) {
            return Result.failure(DomainError.NOT_FOUND, "Fórmula no encontrada");
        }

        long now = clock.nowMillis();
        long day = request.targetDay != null ? request.targetDay : clock.todayEpochDay();
        ColorFormula copy = ColorFormulaRules.duplicateAsNewDraft(
                found.get(),
                idGenerator.nextId(),
                request.targetAppointmentId,
                request.targetProfessionalId,
                day,
                now,
                idGenerator
        );
        colorFormulaRepository.save(copy);

        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("COLOR_FORMULA_DUPLICATE")
                .entityType("COLOR_FORMULA")
                .entityId(copy.id())
                .concept("Duplicado de fórmula " + found.get().id())
                .build());
        return Result.success(copy);
    }

    public static final class Request {
        private final String sourceFormulaId;
        private final String targetAppointmentId;
        private final String targetProfessionalId;
        private final Long targetDay;

        public Request(String sourceFormulaId) {
            this(sourceFormulaId, null, null, null);
        }

        public Request(
                String sourceFormulaId,
                String targetAppointmentId,
                String targetProfessionalId,
                Long targetDay
        ) {
            this.sourceFormulaId = sourceFormulaId;
            this.targetAppointmentId = targetAppointmentId;
            this.targetProfessionalId = targetProfessionalId;
            this.targetDay = targetDay;
        }
    }
}
