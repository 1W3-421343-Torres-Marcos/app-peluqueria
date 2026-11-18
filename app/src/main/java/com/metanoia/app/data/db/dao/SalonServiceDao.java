package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.metanoia.app.data.db.entity.SalonServiceEntity;

import java.util.List;

@Dao
public interface SalonServiceDao {

    @Query("SELECT * FROM salon_services "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY category_normalized ASC, sort_order ASC, name_normalized ASC")
    List<SalonServiceEntity> findAllSync(boolean includeInactive);

    @Query("SELECT * FROM salon_services "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY category_normalized ASC, sort_order ASC, name_normalized ASC")
    LiveData<List<SalonServiceEntity>> observeAll(boolean includeInactive);

    @Query("SELECT * FROM salon_services WHERE id = :id LIMIT 1")
    SalonServiceEntity findByIdSync(String id);

    @Query("SELECT * FROM salon_services "
            + "WHERE active = 1 AND category_normalized = :normalizedCategory "
            + "AND name_normalized = :normalizedName LIMIT 1")
    SalonServiceEntity findActiveByCategoryAndNormalizedName(
            String normalizedCategory, String normalizedName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SalonServiceEntity entity);
}
