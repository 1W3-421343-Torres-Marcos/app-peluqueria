package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.metanoia.app.data.db.entity.ReminderEntity;
import com.metanoia.app.data.db.projection.BirthdayReminderProjection;
import com.metanoia.app.data.db.projection.TomorrowReminderProjection;

import java.util.List;

/**
 * DAO para consultar recordatorios de mañana, cumpleaños del día y registrar envíos.
 */
@Dao
public interface ReminderDao {

    @Query("SELECT a.id AS appointment_id, a.client_id, c.name AS client_name, "
            + "c.phone AS client_phone, p.name AS professional_name, "
            + "a.day, a.start_min, a.status, "
            + "COALESCE((SELECT GROUP_CONCAT(ai.service_name_snapshot, ', ') "
            + "FROM appointment_items ai WHERE ai.appointment_id = a.id), '') AS services_summary, "
            + "(SELECT r.id FROM reminders r "
            + "WHERE r.appointment_id = a.id AND r.type = 'APPOINTMENT' LIMIT 1) AS reminder_id, "
            + "(SELECT r.sent_at FROM reminders r "
            + "WHERE r.appointment_id = a.id AND r.type = 'APPOINTMENT' LIMIT 1) AS sent_at "
            + "FROM appointments a "
            + "INNER JOIN clients c ON c.id = a.client_id "
            + "INNER JOIN professionals p ON p.id = a.professional_id "
            + "WHERE a.day = :tomorrowEpochDay "
            + "AND a.status IN ('PENDING', 'CONFIRMED') "
            + "ORDER BY (EXISTS (SELECT 1 FROM reminders r "
            + "WHERE r.appointment_id = a.id AND r.type = 'APPOINTMENT')) ASC, "
            + "a.start_min ASC")
    LiveData<List<TomorrowReminderProjection>> observeTomorrowReminders(long tomorrowEpochDay);

    @Query("SELECT COUNT(*) FROM appointments a "
            + "WHERE a.day = :tomorrowEpochDay "
            + "AND a.status IN ('PENDING', 'CONFIRMED') "
            + "AND NOT EXISTS (SELECT 1 FROM reminders r "
            + "WHERE r.appointment_id = a.id AND r.type = 'APPOINTMENT')")
    int countPendingTomorrowSync(long tomorrowEpochDay);

    @Query("SELECT c.id AS client_id, c.name AS client_name, c.phone AS client_phone, "
            + "c.birth_day, c.birth_month, c.birth_year, "
            + "(SELECT r.id FROM reminders r "
            + "WHERE r.client_id = c.id AND r.type = 'BIRTHDAY' AND r.year = :year LIMIT 1) AS reminder_id, "
            + "(SELECT r.sent_at FROM reminders r "
            + "WHERE r.client_id = c.id AND r.type = 'BIRTHDAY' AND r.year = :year LIMIT 1) AS sent_at "
            + "FROM clients c "
            + "WHERE c.active = 1 AND c.birth_day = :dayOfMonth AND c.birth_month = :month "
            + "ORDER BY (EXISTS (SELECT 1 FROM reminders r "
            + "WHERE r.client_id = c.id AND r.type = 'BIRTHDAY' AND r.year = :year)) ASC, "
            + "c.name_normalized ASC")
    LiveData<List<BirthdayReminderProjection>> observeTodayBirthdays(
            int dayOfMonth,
            int month,
            int year
    );

    @Query("SELECT COUNT(*) FROM clients c "
            + "WHERE c.active = 1 AND c.birth_day = :dayOfMonth AND c.birth_month = :month "
            + "AND NOT EXISTS (SELECT 1 FROM reminders r "
            + "WHERE r.client_id = c.id AND r.type = 'BIRTHDAY' AND r.year = :year)")
    int countPendingBirthdaysSync(int dayOfMonth, int month, int year);

    @Query("SELECT * FROM reminders "
            + "WHERE appointment_id = :appointmentId AND type = 'APPOINTMENT' LIMIT 1")
    LiveData<ReminderEntity> observeByAppointmentId(String appointmentId);

    @Query("SELECT * FROM reminders "
            + "WHERE appointment_id = :appointmentId AND type = 'APPOINTMENT' LIMIT 1")
    ReminderEntity findByAppointmentIdSync(String appointmentId);

    @Query("SELECT * FROM reminders "
            + "WHERE client_id = :clientId AND type = 'BIRTHDAY' AND year = :year LIMIT 1")
    ReminderEntity findBirthdaySync(String clientId, int year);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(ReminderEntity entity);

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    void deleteById(String reminderId);
}
