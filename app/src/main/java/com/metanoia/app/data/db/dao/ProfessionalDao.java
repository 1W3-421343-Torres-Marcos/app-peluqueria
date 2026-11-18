package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.metanoia.app.data.db.entity.ProfessionalDayOffEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.WorkScheduleEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;

import java.util.List;

@Dao
public interface ProfessionalDao {

    @Transaction
    @Query("SELECT * FROM professionals "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<ProfessionalWithDetails>> observeAllWithDetails(boolean includeInactive);

    @Transaction
    @Query("SELECT * FROM professionals "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY name COLLATE NOCASE ASC")
    List<ProfessionalWithDetails> findAllWithDetailsSync(boolean includeInactive);

    @Transaction
    @Query("SELECT * FROM professionals WHERE id = :id LIMIT 1")
    ProfessionalWithDetails findByIdWithDetailsSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertProfessional(ProfessionalEntity entity);

    @Query("DELETE FROM work_schedules WHERE professional_id = :professionalId")
    void clearWorkSchedules(String professionalId);

    @Insert
    void insertWorkSchedules(List<WorkScheduleEntity> schedules);

    @Query("DELETE FROM professional_services WHERE professional_id = :professionalId")
    void clearServiceRefs(String professionalId);

    @Insert
    void insertServiceRefs(List<ProfessionalServiceCrossRef> refs);

    @Query("DELETE FROM professional_days_off WHERE professional_id = :professionalId")
    void clearDaysOff(String professionalId);

    @Insert
    void insertDaysOff(List<ProfessionalDayOffEntity> daysOff);

    @Transaction
    default void saveComplete(
            ProfessionalEntity entity,
            List<WorkScheduleEntity> schedules,
            List<ProfessionalServiceCrossRef> refs,
            List<ProfessionalDayOffEntity> daysOff
    ) {
        upsertProfessional(entity);
        clearWorkSchedules(entity.id);
        if (!schedules.isEmpty()) {
            insertWorkSchedules(schedules);
        }
        clearServiceRefs(entity.id);
        if (!refs.isEmpty()) {
            insertServiceRefs(refs);
        }
        clearDaysOff(entity.id);
        if (!daysOff.isEmpty()) {
            insertDaysOff(daysOff);
        }
    }
}
