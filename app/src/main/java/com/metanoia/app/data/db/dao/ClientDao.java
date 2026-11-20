package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.projection.ClientRowProjection;

import java.util.List;

/**
 * Acceso a datos de clientes con búsqueda por prefijo normalizado/teléfono, filtros y proyección liviana.
 */
@Dao
public interface ClientDao {

    String ROW_COLUMNS = "id, name, phone, birth_day, birth_month, no_show_count, "
            + "origin, created_from_contact_at, active";

    String SEARCH_PREDICATE = "(:query = '' "
            + "OR name_normalized LIKE :prefixPattern ESCAPE '\\' "
            + "OR name_normalized LIKE :wordPrefixPattern ESCAPE '\\' "
            + "OR (:phoneContainsPattern != '' AND phone LIKE :phoneContainsPattern ESCAPE '\\'))";

    @Query("SELECT " + ROW_COLUMNS + " FROM clients "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "AND " + SEARCH_PREDICATE + " "
            + "ORDER BY active DESC, name_normalized ASC")
    LiveData<List<ClientRowProjection>> observeSummaries(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            boolean includeInactive
    );

    @Query("SELECT " + ROW_COLUMNS + " FROM clients "
            + "WHERE ("
            + "(:filterMode = 'ALL' AND (:includeInactive = 1 OR active = 1)) "
            + "OR (:filterMode = 'MISSING_BIRTHDAY' AND active = 1 AND (birth_day IS NULL OR birth_month IS NULL)) "
            + "OR (:filterMode = 'RECENT_CONTACT' AND active = 1 AND origin = 'CONTACT_SYNC' "
            + "AND created_from_contact_at IS NOT NULL AND created_from_contact_at >= :recentThresholdMillis)"
            + ") "
            + "AND " + SEARCH_PREDICATE + " "
            + "ORDER BY active DESC, "
            + "CASE WHEN :filterMode = 'RECENT_CONTACT' THEN COALESCE(created_from_contact_at, 0) ELSE 0 END DESC, "
            + "name_normalized ASC")
    LiveData<List<ClientRowProjection>> observeSummariesFiltered(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            boolean includeInactive,
            String filterMode,
            long recentThresholdMillis
    );

    @Query("SELECT " + ROW_COLUMNS + " FROM clients "
            + "WHERE active = 1 "
            + "AND " + SEARCH_PREDICATE + " "
            + "ORDER BY name_normalized ASC "
            + "LIMIT :limit")
    LiveData<List<ClientRowProjection>> observeTopActiveSummaries(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            int limit
    );

    @Query("SELECT " + ROW_COLUMNS + " FROM clients "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "AND " + SEARCH_PREDICATE + " "
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

    @Query("SELECT * FROM clients")
    List<ClientEntity> findAllSync();

    @Query("SELECT " + ROW_COLUMNS + " FROM clients WHERE id IN (:ids)")
    LiveData<List<ClientRowProjection>> observeSummariesByIds(List<String> ids);

    @Query("SELECT " + ROW_COLUMNS + " FROM clients WHERE id IN (:ids)")
    List<ClientRowProjection> findSummariesByIdsSync(List<String> ids);

    @Query("SELECT * FROM clients WHERE active = 1 AND phone = :normalizedPhone LIMIT 1")
    ClientEntity findActiveByPhoneSync(String normalizedPhone);

    @Query("SELECT COUNT(*) FROM clients WHERE active = 1 AND (birth_day IS NULL OR birth_month IS NULL)")
    LiveData<Integer> observeMissingBirthdayCount();

    @Query("SELECT COUNT(*) FROM clients WHERE active = 1")
    LiveData<Integer> observeActiveCount();

    @Query("SELECT COUNT(*) FROM clients")
    int countSync();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(ClientEntity entity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAllIgnore(List<ClientEntity> entities);

    @Update
    void update(ClientEntity entity);

    @Transaction
    default void upsert(ClientEntity entity) {
        if (insertIgnore(entity) == -1L) {
            update(entity);
        }
    }
}
