package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Cancela una entrada abierta de lista de espera dejándola registrada para historial y auditoría.
 */
public final class CancelWaitlistEntryUseCase {

    private final WaitlistRepository waitlistRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CancelWaitlistEntryUseCase(
            WaitlistRepository waitlistRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.waitlistRepository = Objects.requireNonNull(
                waitlistRepository, "waitlistRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<WaitlistEntry> execute(String entryId) {
        if (entryId == null || entryId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de entrada inválido");
        }
        Optional<WaitlistEntry> found = waitlistRepository.findById(entryId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Entrada no encontrada");
        }
        WaitlistEntry existing = found.get();
        if (!existing.status().isOpen()) {
            return Result.failure(DomainError.CONFLICT,
                    "La entrada ya se encuentra cerrada");
        }

        WaitlistEntry cancelled = existing.toBuilder()
                .status(WaitlistStatus.CANCELLED)
                .build();
        waitlistRepository.save(cancelled);

        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(clock.nowMillis())
                .operator(settings.currentOperator())
                .action("WAITLIST_CANCEL")
                .entityType("WAITLIST")
                .entityId(cancelled.id())
                .concept("Cancelación de lista de espera")
                .build());
        return Result.success(cancelled);
    }
}
