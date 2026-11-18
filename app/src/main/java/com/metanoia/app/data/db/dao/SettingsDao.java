package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.metanoia.app.data.db.entity.SalonHoursEntity;
import com.metanoia.app.data.db.entity.SettingsEntity;

import java.util.List;

@Dao
public interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 'default' LIMIT 1")
    SettingsEntity getSettingsSync();

    @Query("SELECT * FROM settings WHERE id = 'default' LIMIT 1")
    LiveData<SettingsEntity> observeSettings();

    @Query("SELECT * FROM salon_hours ORDER BY day_of_week ASC, start_min ASC")
    List<SalonHoursEntity> getHoursSync();

    @Query("SELECT * FROM salon_hours ORDER BY day_of_week ASC, start_min ASC")
    LiveData<List<SalonHoursEntity>> observeHours();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertSettings(SettingsEntity entity);

    @Query("DELETE FROM salon_hours")
    void clearHours();

    @Insert
    void insertHours(List<SalonHoursEntity> hours);

    @Transaction
    default void saveSettingsAndHours(SettingsEntity entity, List<SalonHoursEntity> hours) {
        upsertSettings(entity);
        clearHours();
        if (!hours.isEmpty()) {
            insertHours(hours);
        }
    }
}
