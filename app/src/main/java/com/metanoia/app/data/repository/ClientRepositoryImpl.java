package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.ClientDao;
import com.metanoia.app.data.db.dao.ClientSearchFilter;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.mapper.ClientMapper;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.repository.ClientRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link ClientRepository}.
 */
public final class ClientRepositoryImpl implements ClientRepository {

    private final ClientDao dao;

    public ClientRepositoryImpl(ClientDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
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
}
