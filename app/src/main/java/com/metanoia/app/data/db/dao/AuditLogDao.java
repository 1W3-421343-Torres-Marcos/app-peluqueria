package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.metanoia.app.data.db.entity.AuditLogEntity;

import java.util.List;

@Dao
public interface AuditLogDao {

    @Insert
    void insert(AuditLogEntity log);

    @Query("SELECT * FROM audit_logs "
            + "WHERE timestamp BETWEEN :fromMillis AND :toMillis "
            + "ORDER BY timestamp DESC")
    List<AuditLogEntity> findByTimestampRange(long fromMillis, long toMillis);

    @Query("SELECT * FROM audit_logs "
            + "WHERE entity_type = :entityType AND entity_id = :entityId "
            + "ORDER BY timestamp DESC")
    List<AuditLogEntity> findByEntity(String entityType, String entityId);

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<AuditLogEntity>> observeRecent(int limit);
}
