package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.ScheduleBlockDao;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.mapper.ScheduleBlockMapper;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.repository.ScheduleBlockRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link ScheduleBlockRepository}.
 */
public final class ScheduleBlockRepositoryImpl implements ScheduleBlockRepository {

    private final ScheduleBlockDao dao;

    public ScheduleBlockRepositoryImpl(ScheduleBlockDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    @Override
    public Optional<ScheduleBlock> findById(String id) {
        ScheduleBlockEntity entity = dao.findByIdSync(id);
        return entity == null ? Optional.empty() : Optional.of(ScheduleBlockMapper.toDomain(entity));
    }

    @Override
    public List<ScheduleBlock> findActiveOverlappingRange(long startDayEpoch, long endDayEpoch) {
        return ScheduleBlockMapper.toDomainList(
                dao.findActiveOverlappingRangeSync(startDayEpoch, endDayEpoch));
    }

    @Override
    public void save(ScheduleBlock block) {
        dao.upsert(ScheduleBlockMapper.toEntity(block));
    }
}
