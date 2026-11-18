package com.metanoia.app.data.repository;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.db.dao.CashDao;
import com.metanoia.app.data.db.entity.CashClosingEntity;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.data.mapper.CashMapper;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.repository.CashRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link CashRepository} con transacciones atómicas compartidas.
 */
public final class CashRepositoryImpl implements CashRepository {

    private final AppDatabase database;
    private final CashDao cashDao;

    public CashRepositoryImpl(@NonNull AppDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.cashDao = database.cashDao();
    }

    @Override
    public Optional<CashMovement> findMovementById(String id) {
        CashMovementEntity entity = cashDao.findByIdSync(id);
        return entity == null ? Optional.empty() : Optional.of(CashMapper.toDomain(entity));
    }

    @Override
    public List<CashMovement> findMovementsByDay(long dayEpoch) {
        return mapMovements(cashDao.findByDaySync(dayEpoch));
    }

    @Override
    public List<CashMovement> findMovementsByAppointmentId(String appointmentId) {
        return mapMovements(cashDao.findByAppointmentIdSync(appointmentId));
    }

    @Override
    public List<CashMovement> findMovementsByClientId(String clientId) {
        return mapMovements(cashDao.findByClientIdSync(clientId));
    }

    @Override
    public boolean existsReversalForMovement(String movementId) {
        return cashDao.existsReversalForMovementSync(movementId);
    }

    @Override
    public boolean isDayClosed(long dayEpoch) {
        return cashDao.isDayClosedSync(dayEpoch);
    }

    @Override
    public Optional<CashClosing> findClosingByDay(long dayEpoch) {
        CashClosingEntity entity = cashDao.findClosingByDaySync(dayEpoch);
        return entity == null ? Optional.empty() : Optional.of(CashMapper.toDomain(entity));
    }

    @Override
    public Optional<Long> findLatestUnclosedDayWithMovementsBefore(long dayEpoch) {
        return Optional.ofNullable(cashDao.findLatestUnclosedDayWithMovementsBeforeSync(dayEpoch));
    }

    @Override
    public void saveMovement(CashMovement movement) {
        cashDao.insertMovement(CashMapper.toEntity(movement));
    }

    @Override
    public void saveMovements(List<CashMovement> movements) {
        if (movements.isEmpty()) {
            return;
        }
        List<CashMovementEntity> entities = new ArrayList<>(movements.size());
        for (CashMovement item : movements) {
            entities.add(CashMapper.toEntity(item));
        }
        cashDao.insertMovements(entities);
    }

    @Override
    public void saveClosingAndLockDay(CashClosing closing) {
        cashDao.saveClosingAndLockDay(CashMapper.toEntity(closing));
    }

    @Override
    public void runInTransaction(Runnable action) {
        database.runInTransaction(action);
    }

    private static List<CashMovement> mapMovements(List<CashMovementEntity> entities) {
        List<CashMovement> list = new ArrayList<>(entities.size());
        for (CashMovementEntity entity : entities) {
            list.add(CashMapper.toDomain(entity));
        }
        return list;
    }
}
