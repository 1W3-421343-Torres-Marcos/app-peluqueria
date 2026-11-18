package com.metanoia.domain.repository;

import com.metanoia.domain.model.SalonService;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de servicios del salón.
 */
public interface SalonServiceRepository {

    List<SalonService> findAll(boolean includeInactive);

    Optional<SalonService> findById(String id);

    Optional<SalonService> findActiveByCategoryAndNormalizedName(
            String normalizedCategory, String normalizedName);

    void save(SalonService service);
}
