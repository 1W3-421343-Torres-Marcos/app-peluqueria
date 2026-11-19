package com.metanoia.app.data.repository;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.db.dao.CommissionDao;
import com.metanoia.app.data.db.pojo.CommissionSettlementWithEntries;
import com.metanoia.app.data.mapper.AppointmentMapper;
import com.metanoia.app.data.mapper.CommissionMapper;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.repository.CommissionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementación Room de {@link CommissionRepository}.
 */
public final class CommissionRepositoryImpl implements CommissionRepository {

    private final AppDatabase database;
    private final CommissionDao commissionDao;

    public CommissionRepositoryImpl(@NonNull AppDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.commissionDao = database.commissionDao();
    }

    @Override
    public List<CommissionSettlement> findSettlementsByProfessional(String professionalId) {
        List<CommissionSettlementWithEntries> rows =
                commissionDao.findByProfessionalWithEntriesSync(professionalId);
        List<CommissionSettlement> result = new ArrayList<>(rows.size());
        for (CommissionSettlementWithEntries row : rows) {
            result.add(CommissionMapper.toDomain(row));
        }
        return result;
    }

    @Override
    public boolean hasOverlappingSettlement(String professionalId, long fromDay, long toDay) {
        return commissionDao.hasOverlappingSettlementSync(professionalId, fromDay, toDay);
    }

    @Override
    public List<Appointment> findPaidAppointmentsInPreviousSettledPeriods(
            String professionalId,
            long beforeDayEpoch
    ) {
        return AppointmentMapper.toDomainList(
                commissionDao.findPaidInPreviousSettledPeriodsSync(professionalId, beforeDayEpoch));
    }

    @Override
    public void saveSettlement(CommissionSettlement settlement) {
        commissionDao.saveSettlementWithEntries(
                CommissionMapper.toEntity(settlement),
                CommissionMapper.toEntryEntities(settlement)
        );
    }

    @Override
    public void runInTransaction(Runnable action) {
        database.runInTransaction(action);
    }
}
