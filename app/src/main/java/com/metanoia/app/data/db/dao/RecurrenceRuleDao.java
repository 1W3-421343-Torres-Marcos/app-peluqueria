package com.metanoia.app.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.metanoia.app.data.db.entity.RecurrenceRuleEntity;

/**
 * DAO de reglas de recurrencia de turnos.
 */
@Dao
public interface RecurrenceRuleDao {

    @Query("SELECT * FROM recurrence_rules WHERE id = :id LIMIT 1")
    RecurrenceRuleEntity findByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(RecurrenceRuleEntity entity);
}
