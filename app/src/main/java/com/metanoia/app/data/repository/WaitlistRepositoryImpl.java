package com.metanoia.app.data.repository;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.dao.WaitlistDao;
import com.metanoia.app.data.db.pojo.WaitlistEntryWithServices;
import com.metanoia.app.data.mapper.WaitlistMapper;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.WaitlistRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación Room de {@link WaitlistRepository}.
 */
public final class WaitlistRepositoryImpl implements WaitlistRepository {

    private final WaitlistDao waitlistDao;

    public WaitlistRepositoryImpl(@NonNull WaitlistDao waitlistDao) {
        this.waitlistDao = Objects.requireNonNull(waitlistDao, "waitlistDao");
    }

    @Override
    public Optional<WaitlistEntry> findById(String id) {
        WaitlistEntryWithServices row = waitlistDao.findByIdWithServicesSync(id);
        if (row == null || row.entry == null
                || row.serviceRefs == null || row.serviceRefs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(WaitlistMapper.toDomain(row));
    }

    @Override
    public List<WaitlistEntry> findByStatuses(Set<WaitlistStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(statuses.size());
        for (WaitlistStatus status : statuses) {
            names.add(status.name());
        }
        return WaitlistMapper.toDomainList(waitlistDao.findByStatusesWithServicesSync(names));
    }

    @Override
    public List<WaitlistEntry> findOpenByClientId(String clientId) {
        return WaitlistMapper.toDomainList(
                waitlistDao.findOpenByClientIdWithServicesSync(clientId));
    }

    @Override
    public void save(WaitlistEntry entry) {
        waitlistDao.saveEntryWithServices(
                WaitlistMapper.toEntity(entry),
                WaitlistMapper.toServiceRefs(entry)
        );
    }

    @Override
    public int expireBefore(long todayEpochDay) {
        return waitlistDao.expireOpenBefore(todayEpochDay);
    }
}
