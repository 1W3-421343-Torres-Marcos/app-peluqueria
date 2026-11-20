package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.db.dao.ClientDao;
import com.metanoia.app.data.db.dao.ClientSearchFilter;
import com.metanoia.app.data.db.entity.AuditLogEntity;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.mapper.ClientMapper;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.repository.ClientRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link ClientRepository} con soporte transaccional para sincronización.
 */
public final class ClientRepositoryImpl implements ClientRepository {

    private final AppDatabase database;
    private final ClientDao dao;

    public ClientRepositoryImpl(AppDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.dao = database.clientDao();
    }

    @Override
    public Optional<Client> findById(String id) {
        ClientEntity entity = dao.findByIdSync(id);
        return entity == null ? Optional.empty() : Optional.of(ClientMapper.toDomain(entity));
    }

    @Override
    public Optional<Client> findActiveByPhone(String normalizedPhone) {
        ClientEntity entity = dao.findActiveByPhoneSync(normalizedPhone);
        return entity == null ? Optional.empty() : Optional.of(ClientMapper.toDomain(entity));
    }

    @Override
    public List<ClientSummary> searchSummaries(String normalizedQuery, boolean includeInactive) {
        ClientSearchFilter filter = ClientSearchFilter.fromRawQuery(normalizedQuery, includeInactive);
        return ClientMapper.toSummaryList(dao.searchSummariesSync(
                filter.query,
                filter.prefixPattern,
                filter.wordPrefixPattern,
                filter.phoneContainsPattern,
                filter.includeInactive
        ));
    }

    @Override
    public void save(Client client) {
        dao.upsert(ClientMapper.toEntity(client));
    }

    @Override
    public List<Client> findAllForContactSync() {
        List<ClientEntity> entities = dao.findAllSync();
        List<Client> out = new ArrayList<>(entities.size());
        for (ClientEntity entity : entities) {
            out.add(ClientMapper.toDomain(entity));
        }
        return out;
    }

    @Override
    public void saveAllNewClientsWithAudit(List<Client> newClients, AuditRecord auditRecord) {
        if ((newClients == null || newClients.isEmpty()) && auditRecord == null) {
            return;
        }
        database.runInTransaction(() -> {
            if (newClients != null && !newClients.isEmpty()) {
                List<ClientEntity> entities = new ArrayList<>(newClients.size());
                for (Client client : newClients) {
                    entities.add(ClientMapper.toEntity(client));
                }
                dao.insertAllIgnore(entities);
            }
            if (auditRecord != null) {
                database.auditLogDao().insert(new AuditLogEntity(
                        auditRecord.id(),
                        auditRecord.timestamp(),
                        auditRecord.operator(),
                        auditRecord.action(),
                        auditRecord.entityType(),
                        auditRecord.entityId(),
                        auditRecord.amountCents().orElse(null),
                        auditRecord.concept()
                ));
            }
        });
    }
}
