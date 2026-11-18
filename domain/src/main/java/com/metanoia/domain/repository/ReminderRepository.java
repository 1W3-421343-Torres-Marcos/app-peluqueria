package com.metanoia.domain.repository;

import com.metanoia.domain.model.Reminder;

import java.util.Optional;

/**
 * Repositorio para consultar, registrar y deshacer envíos de recordatorios y saludos.
 */
public interface ReminderRepository {

    Optional<Reminder> findAppointmentReminder(String appointmentId);

    Optional<Reminder> findBirthdayReminder(String clientId, int year);

    void save(Reminder reminder);

    void deleteById(String reminderId);
}
