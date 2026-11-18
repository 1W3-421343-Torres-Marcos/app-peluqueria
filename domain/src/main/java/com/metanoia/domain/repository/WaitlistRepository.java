package com.metanoia.domain.repository;

import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio para gestionar entradas de la lista de espera y su vencimiento automático.
 */
public interface WaitlistRepository {

    Optional<WaitlistEntry> findById(String id);

    List<WaitlistEntry> findByStatuses(Set<WaitlistStatus> statuses);

    List<WaitlistEntry> findOpenByClientId(String clientId);

    void save(WaitlistEntry entry);

    int expireBefore(long todayEpochDay);
}
