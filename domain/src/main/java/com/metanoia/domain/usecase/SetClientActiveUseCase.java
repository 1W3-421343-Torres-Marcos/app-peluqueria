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

import java.util.Objects;
import java.util.Optional;

/**
 * Realiza la baja lógica o reactivación de un cliente conservando su historial.
 */
public final class SetClientActiveUseCase {

    private final ClientRepository clientRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SetClientActiveUseCase(
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

    public Result<Client> execute(String clientId, boolean active) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "ID de cliente inválido");
        }
        Optional<Client> found = clientRepository.findById(clientId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Cliente no encontrado");
        }
        Client current = found.get();
        if (current.isActive() == active) {
            return Result.success(current);
        }
        if (active) {
            Optional<Client> duplicate = clientRepository.findActiveByPhone(current.phone());
            if (duplicate.isPresent() && !duplicate.get().id().equals(current.id())) {
                return Result.failure(DomainError.CONFLICT, duplicate.get().id());
            }
        }
        long now = clock.nowMillis();
        Client updated = current.toBuilder()
                .active(active)
                .updatedAt(now)
                .build();
        clientRepository.save(updated);
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settingsRepository.getSettings().currentOperator())
                .action(active ? "CLIENT_REACTIVATE" : "CLIENT_DEACTIVATE")
                .entityType("CLIENT")
                .entityId(updated.id())
                .concept(active ? "Reactivación de cliente" : "Baja lógica de cliente")
                .build());
        return Result.success(updated);
    }
}
