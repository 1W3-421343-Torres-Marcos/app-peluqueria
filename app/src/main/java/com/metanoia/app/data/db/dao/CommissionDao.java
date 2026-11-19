package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.metanoia.app.data.db.entity.CommissionSettlementEntity;
import com.metanoia.app.data.db.entity.CommissionSettlementEntryEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.app.data.db.pojo.CommissionSettlementWithEntries;

import java.util.List;

/**
 * DAO para registrar liquidaciones inmutables de comisiones y consultar solapamientos e historial.
 */
@Dao
public interface CommissionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertSettlement(CommissionSettlementEntity settlement);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertEntries(List<CommissionSettlementEntryEntity> entries);

    @Transaction
    default void saveSettlementWithEntries(
            CommissionSettlementEntity settlement,
            List<CommissionSettlementEntryEntity> entries
    ) {
        insertSettlement(settlement);
        if (entries != null && !entries.isEmpty()) {
            insertEntries(entries);
        }
    }

    @Transaction
    @Query("SELECT * FROM commission_settlements WHERE professional_id = :professionalId "
            + "ORDER BY settled_at DESC")
    List<CommissionSettlementWithEntries> findByProfessionalWithEntriesSync(String professionalId);

    @Query("SELECT * FROM commission_settlements WHERE professional_id = :professionalId "
            + "ORDER BY settled_at DESC")
    LiveData<List<CommissionSettlementEntity>> observeSettlementsByProfessional(
            String professionalId);

    @Query("SELECT EXISTS(SELECT 1 FROM commission_settlements "
            + "WHERE professional_id = :professionalId "
            + "AND from_day <= :toDay AND to_day >= :fromDay)")
    boolean hasOverlappingSettlementSync(String professionalId, long fromDay, long toDay);

    @Transaction
    @Query("SELECT a.* FROM appointments a "
            + "WHERE a.professional_id = :professionalId "
            + "AND a.status = 'PAID' "
            + "AND a.day < :beforeDayEpoch "
            + "AND EXISTS (SELECT 1 FROM commission_settlements s "
            + "WHERE s.professional_id = a.professional_id "
            + "AND a.day BETWEEN s.from_day AND s.to_day) "
            + "ORDER BY a.day ASC, a.start_min ASC")
    List<AppointmentWithItems> findPaidInPreviousSettledPeriodsSync(
            String professionalId,
            long beforeDayEpoch
    );
}
