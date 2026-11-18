package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Desactiva lógicamente un bloqueo de agenda liberando el horario afectado.
 */
public final class DeactivateScheduleBlockUseCase {

    private final ScheduleBlockRepository blockRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public DeactivateScheduleBlockUseCase(
            ScheduleBlockRepository blockRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.blockRepository = Objects.requireNonNull(blockRepository, "blockRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<ScheduleBlock> execute(String blockId) {
        if (blockId == null || blockId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de bloqueo inválido");
        }
        Optional<ScheduleBlock> found = blockRepository.findById(blockId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Bloqueo no encontrado");
        }
        ScheduleBlock current = found.get();
        if (!current.isActive()) {
            return Result.success(current);
        }
        long now = clock.nowMillis();
        ScheduleBlock updated = current.toBuilder()
                .active(false)
                .updatedAt(now)
                .build();
        blockRepository.save(updated);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settingsRepository.getSettings().currentOperator())
                .action("SCHEDULE_BLOCK_DEACTIVATE")
                .entityType("SCHEDULE_BLOCK")
                .entityId(updated.id())
                .concept("Desactivación de bloqueo")
                .build());
        return Result.success(updated);
    }
}
