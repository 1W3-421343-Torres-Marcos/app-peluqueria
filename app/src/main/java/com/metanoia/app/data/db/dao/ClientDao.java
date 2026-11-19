package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.projection.ClientRowProjection;

import java.util.List;

/**
 * Acceso a datos de clientes con búsqueda por prefijo normalizado/teléfono y proyección liviana.
 */
@Dao
public interface ClientDao {

    @Query("SELECT id, name, phone, no_show_count, active FROM clients "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "AND (:query = '' "
            + "OR name_normalized LIKE :prefixPattern ESCAPE '\\' "
            + "OR name_normalized LIKE :wordPrefixPattern ESCAPE '\\' "
            + "OR (:phoneContainsPattern != '' AND phone LIKE :phoneContainsPattern ESCAPE '\\')) "
            + "ORDER BY active DESC, name_normalized ASC")
    LiveData<List<ClientRowProjection>> observeSummaries(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            boolean includeInactive
    );

    @Query("SELECT id, name, phone, no_show_count, active FROM clients "
            + "WHERE active = 1 "
            + "AND (:query = '' "
            + "OR name_normalized LIKE :prefixPattern ESCAPE '\\' "
            + "OR name_normalized LIKE :wordPrefixPattern ESCAPE '\\' "
            + "OR (:phoneContainsPattern != '' AND phone LIKE :phoneContainsPattern ESCAPE '\\')) "
            + "ORDER BY name_normalized ASC "
            + "LIMIT :limit")
    LiveData<List<ClientRowProjection>> observeTopActiveSummaries(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            int limit
    );

    @Query("SELECT id, name, phone, no_show_count, active FROM clients "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "AND (:query = '' "
            + "OR name_normalized LIKE :prefixPattern ESCAPE '\\' "
            + "OR name_normalized LIKE :wordPrefixPattern ESCAPE '\\' "
            + "OR (:phoneContainsPattern != '' AND phone LIKE :phoneContainsPattern ESCAPE '\\')) "
            + "ORDER BY active DESC, name_normalized ASC")
    List<ClientRowProjection> searchSummariesSync(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            boolean includeInactive
    );

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    ClientEntity findByIdSync(String id);

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    LiveData<ClientEntity> observeById(String id);

    @Query("SELECT id, name, phone, no_show_count, active FROM clients WHERE id IN (:ids)")
    LiveData<List<ClientRowProjection>> observeSummariesByIds(List<String> ids);

    @Query("SELECT id, name, phone, no_show_count, active FROM clients WHERE id IN (:ids)")
    List<ClientRowProjection> findSummariesByIdsSync(List<String> ids);

    @Query("SELECT * FROM clients WHERE active = 1 AND phone = :normalizedPhone LIMIT 1")
    ClientEntity findActiveByPhoneSync(String normalizedPhone);

    @Query("SELECT COUNT(*) FROM clients")
    int countSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ClientEntity entity);
}
