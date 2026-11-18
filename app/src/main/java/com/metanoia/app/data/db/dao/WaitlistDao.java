package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.metanoia.app.data.db.entity.WaitlistEntryEntity;
import com.metanoia.app.data.db.entity.WaitlistEntryServiceCrossRef;
import com.metanoia.app.data.db.pojo.WaitlistEntryWithServices;

import java.util.List;

/**
 * DAO de lista de espera con vencimiento automático y actualización segura ante claves RESTRICT.
 */
@Dao
public interface WaitlistDao {

    @Transaction
    @Query("SELECT * FROM waitlist_entries WHERE id = :id LIMIT 1")
    WaitlistEntryWithServices findByIdWithServicesSync(String id);

    @Transaction
    @Query("SELECT * FROM waitlist_entries WHERE status IN (:statuses) ORDER BY created_at ASC")
    List<WaitlistEntryWithServices> findByStatusesWithServicesSync(List<String> statuses);

    @Transaction
    @Query("SELECT * FROM waitlist_entries WHERE client_id = :clientId "
            + "AND status IN ('ACTIVE', 'OFFERED') ORDER BY created_at ASC")
    List<WaitlistEntryWithServices> findOpenByClientIdWithServicesSync(String clientId);

    @Transaction
    @Query("SELECT * FROM waitlist_entries WHERE status IN (:statuses) ORDER BY created_at ASC")
    LiveData<List<WaitlistEntryWithServices>> observeByStatusesWithServices(List<String> statuses);

    @Transaction
    @Query("SELECT * FROM waitlist_entries ORDER BY "
            + "CASE status WHEN 'ACTIVE' THEN 0 WHEN 'OFFERED' THEN 1 ELSE 2 END, "
            + "created_at DESC")
    LiveData<List<WaitlistEntryWithServices>> observeAllWithServices();

    @Query("SELECT COUNT(*) FROM waitlist_entries WHERE status IN ('ACTIVE', 'OFFERED')")
    LiveData<Integer> observeOpenCount();

    @Query("UPDATE waitlist_entries SET status = 'EXPIRED' "
            + "WHERE status IN ('ACTIVE', 'OFFERED') AND valid_until_day < :todayEpochDay")
    int expireOpenBefore(long todayEpochDay);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertEntryIgnore(WaitlistEntryEntity entity);

    @Update
    void updateEntry(WaitlistEntryEntity entity);

    @Query("DELETE FROM waitlist_entry_services WHERE entry_id = :entryId")
    void deleteServicesForEntry(String entryId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertServiceRefs(List<WaitlistEntryServiceCrossRef> refs);

    @Transaction
    default void saveEntryWithServices(
            WaitlistEntryEntity entity,
            List<WaitlistEntryServiceCrossRef> refs
    ) {
        if (insertEntryIgnore(entity) == -1L) {
            updateEntry(entity);
        }
        deleteServicesForEntry(entity.id);
        if (!refs.isEmpty()) {
            insertServiceRefs(refs);
        }
    }
}
