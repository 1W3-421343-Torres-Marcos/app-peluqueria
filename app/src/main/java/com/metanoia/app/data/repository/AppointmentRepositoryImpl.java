package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.db.dao.AppointmentDao;
import com.metanoia.app.data.db.dao.RecurrenceRuleDao;
import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.app.data.mapper.AppointmentMapper;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.repository.AppointmentRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link AppointmentRepository}.
 */
public final class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppDatabase database;
    private final AppointmentDao appointmentDao;
    private final RecurrenceRuleDao recurrenceRuleDao;

    public AppointmentRepositoryImpl(
            AppDatabase database,
            AppointmentDao appointmentDao,
            RecurrenceRuleDao recurrenceRuleDao
    ) {
        this.database = Objects.requireNonNull(database, "database");
        this.appointmentDao = Objects.requireNonNull(appointmentDao, "appointmentDao");
        this.recurrenceRuleDao = Objects.requireNonNull(recurrenceRuleDao, "recurrenceRuleDao");
    }

    @Override
    public Optional<Appointment> findById(String id) {
        AppointmentWithItems row = appointmentDao.findByIdWithItemsSync(id);
        return row == null ? Optional.empty() : Optional.of(AppointmentMapper.toDomain(row));
    }

    @Override
    public List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId) {
        return AppointmentMapper.toDomainList(
                appointmentDao.findByDayAndProfessionalSync(dayEpoch, professionalId));
    }

    @Override
    public List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch) {
        return AppointmentMapper.toDomainList(
                appointmentDao.findByDayRangeSync(startDayEpoch, endDayEpoch));
    }

    @Override
    public List<Appointment> findByProfessionalAndDayRange(
            String professionalId,
            long startDayEpoch,
            long endDayEpoch
    ) {
        if (professionalId == null || professionalId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return AppointmentMapper.toDomainList(
                appointmentDao.findByProfessionalAndDayRangeSync(
                        professionalId.trim(), startDayEpoch, endDayEpoch));
    }

    @Override
    public List<Appointment> findByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cleanIds = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (id != null && !id.trim().isEmpty()) {
                cleanIds.add(id.trim());
            }
        }
        if (cleanIds.isEmpty()) {
            return Collections.emptyList();
        }
        return AppointmentMapper.toDomainList(appointmentDao.findByIdsWithItemsSync(cleanIds));
    }

    @Override
    public List<Appointment> findFutureActiveFromSeries(
            String recurrenceRuleId,
            long fromDayEpoch,
            int fromStartMin
    ) {
        return AppointmentMapper.toDomainList(
                appointmentDao.findFutureActiveFromSeriesSync(
                        recurrenceRuleId, fromDayEpoch, fromStartMin));
    }

    @Override
    public boolean hasFutureActiveForProfessional(String professionalId, long fromDayEpoch) {
        return appointmentDao.existsFutureOccupyingForProfessionalSync(
                professionalId, fromDayEpoch);
    }

    @Override
    public void save(Appointment appointment) {
        appointmentDao.saveAppointmentWithItems(
                AppointmentMapper.toEntity(appointment),
                AppointmentMapper.toItemEntities(appointment)
        );
    }

    @Override
    public void saveAll(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return;
        }
        List<AppointmentEntity> entities = new ArrayList<>(appointments.size());
        List<AppointmentItemEntity> allItems = new ArrayList<>();
        for (Appointment appointment : appointments) {
            entities.add(AppointmentMapper.toEntity(appointment));
            allItems.addAll(AppointmentMapper.toItemEntities(appointment));
        }
        appointmentDao.saveAllAppointmentsWithItems(entities, allItems);
    }

    @Override
    public void saveRecurrenceRule(RecurrenceRule rule) {
        recurrenceRuleDao.upsert(AppointmentMapper.toRecurrenceEntity(rule));
    }

    @Override
    public void runInTransaction(Runnable action) {
        database.runInTransaction(action);
    }
}
