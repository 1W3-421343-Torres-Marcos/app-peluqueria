package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ColorFormulaRules;

import java.util.Objects;
import java.util.Optional;

/**
 * Da de baja lógica una fórmula de coloración que no pertenezca a un turno cerrado.
 */
public final class DeleteColorFormulaUseCase {

    private final ColorFormulaRepository colorFormulaRepository;
    private final AppointmentRepository appointmentRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public DeleteColorFormulaUseCase(
            ColorFormulaRepository colorFormulaRepository,
            AppointmentRepository appointmentRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.colorFormulaRepository = Objects.requireNonNull(
                colorFormulaRepository, "colorFormulaRepository");
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Boolean> execute(String formulaId) {
        if (formulaId == null || formulaId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de fórmula inválido");
        }
        Optional<ColorFormula> found = colorFormulaRepository.findById(formulaId.trim());
        if (!found.isPresent() || found.get().isDeleted()) {
            return Result.failure(DomainError.NOT_FOUND, "Fórmula no encontrada");
        }
        ColorFormula existing = found.get();
        if (isLocked(existing)) {
            return Result.failure(DomainError.CONFLICT,
                    "Las fórmulas de turnos completados o pagados no pueden eliminarse");
        }

        long now = clock.nowMillis();
        colorFormulaRepository.softDelete(existing.id(), now);

        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("COLOR_FORMULA_DELETE")
                .entityType("COLOR_FORMULA")
                .entityId(existing.id())
                .concept("Baja lógica de fórmula")
                .build());
        return Result.success(Boolean.TRUE);
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
}
