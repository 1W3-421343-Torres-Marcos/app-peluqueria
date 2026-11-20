package com.metanoia.domain.repository;

import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para consultar, buscar y persistir clientes del salón.
 */
public interface ClientRepository {

    Optional<Client> findById(String id);

    Optional<Client> findActiveByPhone(String normalizedPhone);

    List<ClientSummary> searchSummaries(String normalizedQuery, boolean includeInactive);

    void save(Client client);

    default List<Client> findAllForContactSync() {
        return Collections.emptyList();
    }

    default void saveAllNewClientsWithAudit(List<Client> newClients, AuditRecord auditRecord) {
        for (Client client : newClients) {
            save(client);
        }
    }
}
