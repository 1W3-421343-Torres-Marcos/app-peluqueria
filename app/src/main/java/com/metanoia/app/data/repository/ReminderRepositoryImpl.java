package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.ReminderDao;
import com.metanoia.app.data.db.entity.ReminderEntity;
import com.metanoia.app.data.mapper.ReminderMapper;
import com.metanoia.domain.model.Reminder;
import com.metanoia.domain.repository.ReminderRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link ReminderRepository}.
 */
public final class ReminderRepositoryImpl implements ReminderRepository {

    private final ReminderDao reminderDao;

    public ReminderRepositoryImpl(ReminderDao reminderDao) {
        this.reminderDao = Objects.requireNonNull(reminderDao, "reminderDao");
    }

    @Override
    public Optional<Reminder> findAppointmentReminder(String appointmentId) {
        ReminderEntity found = reminderDao.findByAppointmentIdSync(appointmentId);
        return found == null ? Optional.empty() : Optional.of(ReminderMapper.toDomain(found));
    }

    @Override
    public Optional<Reminder> findBirthdayReminder(String clientId, int year) {
        ReminderEntity found = reminderDao.findBirthdaySync(clientId, year);
        return found == null ? Optional.empty() : Optional.of(ReminderMapper.toDomain(found));
    }

    @Override
    public void save(Reminder reminder) {
        reminderDao.insert(ReminderMapper.toEntity(reminder));
    }

    @Override
    public void deleteById(String reminderId) {
        reminderDao.deleteById(reminderId);
    }
}
