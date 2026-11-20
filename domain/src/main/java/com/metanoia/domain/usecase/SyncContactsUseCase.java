package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ContactImportPreview;
import com.metanoia.domain.model.ContactSyncOutcome;
import com.metanoia.domain.model.DeviceContactCandidate;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ClientContactSyncPlanner;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Importa o sincroniza contactos nuevos del teléfono de forma idempotente sin sobrescribir existentes.
 */
public final class SyncContactsUseCase {

    public static final String ACTION_BULK_IMPORT = "CONTACTS_IMPORT";
    public static final String ACTION_AUTO_SYNC = "CONTACTS_SYNC";

    private final ClientRepository clientRepository;
    private final SettingsRepository settingsRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SyncContactsUseCase(
            ClientRepository clientRepository,
            SettingsRepository settingsRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<ContactSyncOutcome> execute(
            List<DeviceContactCandidate> candidates,
            String auditAction
    ) {
        if (candidates == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Lista de contactos nula");
        }
        String action = ACTION_BULK_IMPORT.equals(auditAction) ? ACTION_BULK_IMPORT : ACTION_AUTO_SYNC;
        List<Client> existing = clientRepository.findAllForContactSync();
        long now = clock.nowMillis();
        int maxYear = LocalDate.ofEpochDay(clock.todayEpochDay()).getYear();

        ClientContactSyncPlanner.PlanResult plan = ClientContactSyncPlanner.plan(
                candidates, existing, maxYear, now, idGenerator);
        List<Client> newClients = plan.newClients();
        if (!newClients.isEmpty()) {
            AuditRecord auditRecord = buildAuditRecord(action, newClients.size(), now);
            clientRepository.saveAllNewClientsWithAudit(newClients, auditRecord);
        }

        ContactImportPreview preview = plan.preview();
        return Result.success(new ContactSyncOutcome(
                newClients.size(),
                preview.alreadyLoadedCount(),
                preview.skippedCount(),
                now
        ));
    }

    private AuditRecord buildAuditRecord(String action, int addedCount, long timestamp) {
        String label = ACTION_BULK_IMPORT.equals(action)
                ? "Importación inicial de contactos: "
                : "Sincronización de contactos: ";
        return new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(timestamp)
                .operator(settingsRepository.getSettings().currentOperator())
                .action(action)
                .entityType("CLIENT")
                .entityId("CONTACT_SYNC")
                .concept(label + addedCount + " cliente(s) nuevo(s)")
                .build();
    }
}
