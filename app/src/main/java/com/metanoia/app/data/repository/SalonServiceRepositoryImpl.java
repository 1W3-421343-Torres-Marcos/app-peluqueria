package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.SalonServiceDao;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.mapper.SalonServiceMapper;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.repository.SalonServiceRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link SalonServiceRepository}.
 */
public final class SalonServiceRepositoryImpl implements SalonServiceRepository {

    private final SalonServiceDao dao;

    public SalonServiceRepositoryImpl(SalonServiceDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public List<SalonService> findAll(boolean includeInactive) {
        return SalonServiceMapper.toDomainList(dao.findAllSync(includeInactive));
    }

    @Override
    public Optional<SalonService> findById(String id) {
        SalonServiceEntity entity = dao.findByIdSync(id);
        return entity == null ? Optional.empty() : Optional.of(SalonServiceMapper.toDomain(entity));
    }

    @Override
    public Optional<SalonService> findActiveByCategoryAndNormalizedName(
            String normalizedCategory, String normalizedName) {
        SalonServiceEntity entity = dao.findActiveByCategoryAndNormalizedName(
                normalizedCategory, normalizedName);
        return entity == null ? Optional.empty() : Optional.of(SalonServiceMapper.toDomain(entity));
    }

    @Override
    public void save(SalonService service) {
        dao.upsert(SalonServiceMapper.toEntity(service));
    }
}
