package com.metanoia.domain.repository;

import com.metanoia.domain.model.ColorFormula;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para consultar, guardar y dar de baja lógica fórmulas de coloración.
 */
public interface ColorFormulaRepository {

    Optional<ColorFormula> findById(String id);

    List<ColorFormula> findActiveByClientId(String clientId);

    Optional<ColorFormula> findLatestActiveByClientId(String clientId);

    boolean isLinkedToClosedAppointment(String formulaId);

    void save(ColorFormula formula);

    void softDelete(String formulaId, long deletedAtMillis);
}
