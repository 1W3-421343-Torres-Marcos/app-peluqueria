package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.StatusTransitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Cancela un turno perteneciente a una serie recurrente en modalidad "solo este"
 * o "este y los siguientes" activos.
 */
public final class CancelRecurringSeriesUseCase {

    public enum Scope {
        ONLY_THIS,
        THIS_AND_FOLLOWING
    }

    private final AppointmentRepository appointmentRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CancelRecurringSeriesUseCase(
            AppointmentRepository appointmentRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<List<Appointment>> execute(
            String appointmentId,
            Scope scope,
            String cancelReason
    ) {
        if (appointmentId == null || appointmentId.trim().isEmpty() || scope == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud inválida");
        }
        String cleanReason = cancelReason == null ? "" : cancelReason.trim();
        if (cleanReason.length() < 2) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Indicá un motivo de cancelación");
        }
        Optional<Appointment> found = appointmentRepository.findById(appointmentId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Turno no encontrado");
        }
        Appointment anchor = found.get();
        if (!StatusTransitions.canTransitionManually(
                anchor.status(), AppointmentStatus.CANCELLED, 0L)) {
            return Result.failure(DomainError.CONFLICT,
                    "El turno seleccionado no puede cancelarse en su estado actual");
        }

        List<Appointment> targets;
        if (scope == Scope.ONLY_THIS || !anchor.recurrenceRuleId().isPresent()) {
            targets = Collections.singletonList(anchor);
        } else {
            targets = appointmentRepository.findFutureActiveFromSeries(
                    anchor.recurrenceRuleId().get(), anchor.day(), anchor.startMin());
        }

        long now = clock.nowMillis();
        List<Appointment> cancelled = new ArrayList<>(targets.size());
        for (Appointment target : targets) {
            if (!StatusTransitions.canTransitionManually(
                    target.status(), AppointmentStatus.CANCELLED, 0L)) {
                continue;
            }
            cancelled.add(target.toBuilder()
                    .status(AppointmentStatus.CANCELLED)
                    .cancelReason(cleanReason)
                    .updatedAt(now)
                    .build());
        }
        if (cancelled.isEmpty()) {
            return Result.failure(DomainError.CONFLICT,
                    "No se encontraron turnos activos para cancelar");
        }

        appointmentRepository.saveAll(cancelled);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settingsRepository.getSettings().currentOperator())
                .action(scope == Scope.THIS_AND_FOLLOWING
                        ? "APPOINTMENT_SERIES_CANCEL"
                        : "APPOINTMENT_CANCEL")
                .entityType("APPOINTMENT")
                .entityId(anchor.id())
                .concept("Cancelación (" + cancelled.size() + "): " + cleanReason)
                .build());

        return Result.success(cancelled);
    }
}
