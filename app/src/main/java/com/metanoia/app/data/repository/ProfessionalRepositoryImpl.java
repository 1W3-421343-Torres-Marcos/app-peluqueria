package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.AppointmentDao;
import com.metanoia.app.data.db.dao.ProfessionalDao;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.mapper.ProfessionalMapper;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.repository.ProfessionalRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link ProfessionalRepository}.
 */
public final class ProfessionalRepositoryImpl implements ProfessionalRepository {

    private final ProfessionalDao dao;
    private final AppointmentDao appointmentDao;

    public ProfessionalRepositoryImpl(ProfessionalDao dao, AppointmentDao appointmentDao) {
        this.dao = Objects.requireNonNull(dao, "dao");
        this.appointmentDao = Objects.requireNonNull(appointmentDao, "appointmentDao");
    }

    @Override
    public List<Professional> findAll(boolean includeInactive) {
        List<ProfessionalWithDetails> rows = dao.findAllWithDetailsSync(includeInactive);
        List<Professional> result = new ArrayList<>(rows.size());
        for (ProfessionalWithDetails row : rows) {
            result.add(ProfessionalMapper.toDomain(row));
        }
        return result;
    }

    @Override
    public Optional<Professional> findById(String id) {
        ProfessionalWithDetails row = dao.findByIdWithDetailsSync(id);
        return row == null ? Optional.empty() : Optional.of(ProfessionalMapper.toDomain(row));
    }

    @Override
    public void save(Professional professional) {
        dao.saveComplete(
                ProfessionalMapper.toEntity(professional),
                ProfessionalMapper.toScheduleEntities(professional),
                ProfessionalMapper.toServiceRefs(professional),
                ProfessionalMapper.toDayOffEntities(professional)
        );
    }

    @Override
    public boolean hasFutureActiveAppointments(String professionalId, long fromDayEpoch) {
        return appointmentDao.existsFutureOccupyingForProfessionalSync(
                professionalId, fromDayEpoch);
    }
}
