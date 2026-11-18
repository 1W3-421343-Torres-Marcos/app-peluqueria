package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.metanoia.app.data.db.entity.ScheduleBlockEntity;

import java.util.List;

/**
 * DAO de bloqueos de agenda por rango de fechas.
 */
@Dao
public interface ScheduleBlockDao {

    @Query("SELECT * FROM schedule_blocks WHERE active = 1 "
            + "AND start_day <= :endDayEpoch AND end_day >= :startDayEpoch "
            + "ORDER BY start_day ASC, COALESCE(start_min, 0) ASC")
    LiveData<List<ScheduleBlockEntity>> observeActiveOverlappingRange(
            long startDayEpoch,
            long endDayEpoch
    );

    @Query("SELECT * FROM schedule_blocks WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY start_day DESC, COALESCE(start_min, 0) ASC")
    LiveData<List<ScheduleBlockEntity>> observeAll(boolean includeInactive);

    @Query("SELECT * FROM schedule_blocks WHERE id = :id LIMIT 1")
    ScheduleBlockEntity findByIdSync(String id);

    @Query("SELECT * FROM schedule_blocks WHERE active = 1 "
            + "AND start_day <= :endDayEpoch AND end_day >= :startDayEpoch "
            + "ORDER BY start_day ASC, COALESCE(start_min, 0) ASC")
    List<ScheduleBlockEntity> findActiveOverlappingRangeSync(
            long startDayEpoch,
            long endDayEpoch
    );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ScheduleBlockEntity entity);
}
