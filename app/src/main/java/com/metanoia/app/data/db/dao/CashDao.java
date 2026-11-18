package com.metanoia.app.data.db.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.metanoia.app.data.db.entity.CashClosingEntity;
import com.metanoia.app.data.db.entity.CashMovementEntity;

import java.util.List;

/**
 * DAO de movimientos inmutables de caja y cierres diarios.
 */
@Dao
public interface CashDao {

    @Query("SELECT * FROM cash_movements WHERE id = :id LIMIT 1")
    CashMovementEntity findByIdSync(String id);

    @Query("SELECT * FROM cash_movements WHERE day = :dayEpoch ORDER BY timestamp DESC")
    LiveData<List<CashMovementEntity>> observeByDay(long dayEpoch);

    @Query("SELECT * FROM cash_movements WHERE day = :dayEpoch ORDER BY timestamp ASC")
    List<CashMovementEntity> findByDaySync(long dayEpoch);

    @Query("SELECT * FROM cash_movements WHERE appointment_id = :appointmentId ORDER BY timestamp ASC")
    LiveData<List<CashMovementEntity>> observeByAppointmentId(String appointmentId);

    @Query("SELECT * FROM cash_movements WHERE appointment_id = :appointmentId ORDER BY timestamp ASC")
    List<CashMovementEntity> findByAppointmentIdSync(String appointmentId);

    @Query("SELECT * FROM cash_movements WHERE client_id = :clientId ORDER BY timestamp DESC")
    LiveData<List<CashMovementEntity>> observeByClientId(String clientId);

    @Query("SELECT * FROM cash_movements WHERE client_id = :clientId ORDER BY timestamp DESC")
    List<CashMovementEntity> findByClientIdSync(String clientId);

    @Query("SELECT COALESCE(SUM("
            + "CASE "
            + "WHEN type IN ('APPOINTMENT_PAYMENT', 'PRODUCT_SALE', 'OTHER_INCOME') THEN amount_cents "
            + "WHEN type = 'REVERSAL' THEN -amount_cents "
            + "ELSE 0 END), 0) "
            + "FROM cash_movements WHERE client_id = :clientId")
    LiveData<Long> observeClientTotalSpentCents(String clientId);

    @Query("SELECT EXISTS(SELECT 1 FROM cash_movements WHERE reverses_movement_id = :movementId)")
    boolean existsReversalForMovementSync(String movementId);

    @Query("SELECT reverses_movement_id FROM cash_movements "
            + "WHERE day = :dayEpoch AND reverses_movement_id IS NOT NULL")
    LiveData<List<String>> observeReversedIdsOnDay(long dayEpoch);

    @Query("SELECT * FROM cash_closings WHERE day = :dayEpoch LIMIT 1")
    CashClosingEntity findClosingByDaySync(long dayEpoch);

    @Query("SELECT * FROM cash_closings WHERE day = :dayEpoch LIMIT 1")
    LiveData<CashClosingEntity> observeClosingByDay(long dayEpoch);

    @Query("SELECT EXISTS(SELECT 1 FROM cash_closings WHERE day = :dayEpoch)")
    boolean isDayClosedSync(long dayEpoch);

    @Query("SELECT MAX(m.day) FROM cash_movements m "
            + "LEFT JOIN cash_closings c ON c.day = m.day "
            + "WHERE m.day < :beforeDayEpoch AND c.id IS NULL")
    Long findLatestUnclosedDayWithMovementsBeforeSync(long beforeDayEpoch);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertMovement(CashMovementEntity entity);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertMovements(List<CashMovementEntity> entities);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertClosing(CashClosingEntity entity);

    @Query("UPDATE cash_movements SET closing_id = :closingId WHERE day = :dayEpoch AND closing_id IS NULL")
    void linkMovementsToClosing(long dayEpoch, String closingId);

    @Transaction
    default void saveClosingAndLockDay(@NonNull CashClosingEntity closing) {
        insertClosing(closing);
        linkMovementsToClosing(closing.day, closing.id);
    }
}
