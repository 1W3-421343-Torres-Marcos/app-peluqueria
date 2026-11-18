package com.metanoia.domain.repository;

import com.metanoia.domain.model.Professional;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de profesionales del salón, sus horarios, servicios habilitados y días libres.
 */
public interface ProfessionalRepository {

    List<Professional> findAll(boolean includeInactive);

    Optional<Professional> findById(String id);

    void save(Professional professional);

    boolean hasFutureActiveAppointments(String professionalId, long fromEpochDay);
}
