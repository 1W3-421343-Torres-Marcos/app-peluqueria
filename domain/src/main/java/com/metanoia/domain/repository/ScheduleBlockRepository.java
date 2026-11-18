package com.metanoia.domain.repository;

import com.metanoia.domain.model.ScheduleBlock;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de bloqueos de agenda (vacaciones, almuerzos, feriados u otros).
 */
public interface ScheduleBlockRepository {

    Optional<ScheduleBlock> findById(String id);

    List<ScheduleBlock> findActiveOverlappingRange(long startDayEpoch, long endDayEpoch);

    void save(ScheduleBlock block);
}
