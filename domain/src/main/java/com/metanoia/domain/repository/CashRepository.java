package com.metanoia.domain.repository;

import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para consultar y registrar movimientos inmutables y cierres de caja.
 */
public interface CashRepository {

    Optional<CashMovement> findMovementById(String id);

    List<CashMovement> findMovementsByDay(long dayEpoch);

    List<CashMovement> findMovementsByAppointmentId(String appointmentId);

    List<CashMovement> findMovementsByClientId(String clientId);

    boolean existsReversalForMovement(String movementId);

    boolean isDayClosed(long dayEpoch);

    Optional<CashClosing> findClosingByDay(long dayEpoch);

    Optional<Long> findLatestUnclosedDayWithMovementsBefore(long dayEpoch);

    void saveMovement(CashMovement movement);

    void saveMovements(List<CashMovement> movements);

    void saveClosingAndLockDay(CashClosing closing);

    void runInTransaction(Runnable action);
}
