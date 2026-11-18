package com.metanoia.app.data.db.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;

import java.util.List;

/**
 * DAO de turnos e ítems con consultas acotadas por rango de fechas y proyecciones livianas.
 */
@Dao
public interface AppointmentDao {

    String PAID_CENTS_EXPR = "COALESCE((SELECT SUM("
            + "CASE WHEN cm.type = 'APPOINTMENT_PAYMENT' THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' THEN -cm.amount_cents ELSE 0 END) "
            + "FROM cash_movements cm WHERE cm.appointment_id = a.id), 0)";

    String CARD_COLUMNS = "a.id, a.client_id, c.name AS client_name, "
            + "a.professional_id, p.name AS professional_name, "
            + "a.day, a.start_min, a.end_min, a.status, "
            + "COALESCE(GROUP_CONCAT(ai.service_name_snapshot, ' + '), '') AS services_summary, "
            + "COALESCE(SUM(ai.price_cents_snapshot), 0) AS items_subtotal_cents, "
            + "a.adjustment_cents, " + PAID_CENTS_EXPR + " AS paid_cents, a.recurrence_rule_id";

    @Query("SELECT " + CARD_COLUMNS + " "
            + "FROM appointments a "
            + "INNER JOIN clients c ON c.id = a.client_id "
            + "INNER JOIN professionals p ON p.id = a.professional_id "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.day = :dayEpoch "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id "
            + "ORDER BY a.start_min ASC")
    LiveData<List<AppointmentCardProjection>> observeDayCards(
            long dayEpoch,
            @Nullable String professionalId
    );

    @Query("SELECT " + CARD_COLUMNS + " "
            + "FROM appointments a "
            + "INNER JOIN clients c ON c.id = a.client_id "
            + "INNER JOIN professionals p ON p.id = a.professional_id "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.day BETWEEN :startDayEpoch AND :endDayEpoch "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id "
            + "ORDER BY a.day ASC, a.start_min ASC")
    LiveData<List<AppointmentCardProjection>> observeRangeCards(
            long startDayEpoch,
            long endDayEpoch,
            @Nullable String professionalId
    );

    @Query("SELECT " + CARD_COLUMNS + " "
            + "FROM appointments a "
            + "INNER JOIN clients c ON c.id = a.client_id "
            + "INNER JOIN professionals p ON p.id = a.professional_id "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.client_id = :clientId AND a.status IN ('COMPLETED', 'PAID') "
            + "GROUP BY a.id "
            + "ORDER BY a.day DESC, a.start_min DESC")
    LiveData<List<AppointmentCardProjection>> observeClientVisitCards(String clientId);

    @Query("SELECT " + CARD_COLUMNS + " "
            + "FROM appointments a "
            + "INNER JOIN clients c ON c.id = a.client_id "
            + "INNER JOIN professionals p ON p.id = a.professional_id "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.status = 'COMPLETED' "
            + "AND MAX(0, COALESCE((SELECT SUM(ai2.price_cents_snapshot) "
            + "FROM appointment_items ai2 WHERE ai2.appointment_id = a.id), 0) "
            + "+ a.adjustment_cents) > " + PAID_CENTS_EXPR + " "
            + "GROUP BY a.id "
            + "ORDER BY a.day ASC, a.start_min ASC")
    LiveData<List<AppointmentCardProjection>> observeReceivableCards();

    @Query("SELECT COUNT(*) FROM appointments a "
            + "WHERE a.status = 'COMPLETED' "
            + "AND MAX(0, COALESCE((SELECT SUM(ai.price_cents_snapshot) "
            + "FROM appointment_items ai WHERE ai.appointment_id = a.id), 0) "
            + "+ a.adjustment_cents) > " + PAID_CENTS_EXPR)
    LiveData<Integer> observeReceivablesCount();

    @Query("SELECT COALESCE(SUM(MAX(0, MAX(0, "
            + "COALESCE((SELECT SUM(ai.price_cents_snapshot) FROM appointment_items ai "
            + "WHERE ai.appointment_id = a.id), 0) + a.adjustment_cents) - "
            + PAID_CENTS_EXPR + ")), 0) "
            + "FROM appointments a "
            + "WHERE a.client_id = :clientId AND a.status = 'COMPLETED'")
    LiveData<Long> observeClientPendingBalanceCents(String clientId);

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    LiveData<AppointmentWithItems> observeByIdWithItems(String id);

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    AppointmentWithItems findByIdWithItemsSync(String id);

    @Transaction
    @Query("SELECT * FROM appointments WHERE day = :dayEpoch AND professional_id = :professionalId "
            + "ORDER BY start_min ASC")
    List<AppointmentWithItems> findByDayAndProfessionalSync(long dayEpoch, String professionalId);

    @Transaction
    @Query("SELECT * FROM appointments WHERE day BETWEEN :startDayEpoch AND :endDayEpoch "
            + "ORDER BY day ASC, start_min ASC")
    List<AppointmentWithItems> findByDayRangeSync(long startDayEpoch, long endDayEpoch);

    @Transaction
    @Query("SELECT * FROM appointments "
            + "WHERE recurrence_rule_id = :recurrenceRuleId "
            + "AND status IN ('PENDING', 'CONFIRMED') "
            + "AND (day > :fromDayEpoch OR (day = :fromDayEpoch AND start_min >= :fromStartMin)) "
            + "ORDER BY day ASC, start_min ASC")
    List<AppointmentWithItems> findFutureActiveFromSeriesSync(
            String recurrenceRuleId,
            long fromDayEpoch,
            int fromStartMin
    );

    @Query("SELECT EXISTS(SELECT 1 FROM appointments "
            + "WHERE professional_id = :professionalId "
            + "AND day >= :fromDayEpoch "
            + "AND status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'PAID'))")
    boolean existsFutureOccupyingForProfessionalSync(String professionalId, long fromDayEpoch);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAppointment(AppointmentEntity entity);

    @Query("DELETE FROM appointment_items WHERE appointment_id = :appointmentId")
    void deleteItemsForAppointment(String appointmentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(List<AppointmentItemEntity> items);

    @Transaction
    default void saveAppointmentWithItems(
            AppointmentEntity entity,
            List<AppointmentItemEntity> items
    ) {
        upsertAppointment(entity);
        deleteItemsForAppointment(entity.id);
        if (!items.isEmpty()) {
            insertItems(items);
        }
    }

    @Transaction
    default void saveAllAppointmentsWithItems(
            List<AppointmentEntity> entities,
            List<AppointmentItemEntity> items
    ) {
        for (AppointmentEntity entity : entities) {
            upsertAppointment(entity);
            deleteItemsForAppointment(entity.id);
        }
        if (!items.isEmpty()) {
            insertItems(items);
        }
    }
}
