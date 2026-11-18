package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.AuditLogDao;
import com.metanoia.app.data.db.entity.AuditLogEntity;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.repository.AuditLogRepository;

import java.util.Objects;

/**
 * Implementación Room de {@link AuditLogRepository}.
 */
public final class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogDao auditLogDao;

    public AuditLogRepositoryImpl(AuditLogDao auditLogDao) {
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "auditLogDao");
    }

    @Override
    public void record(AuditRecord record) {
        AuditLogEntity entity = new AuditLogEntity(
                record.id(),
                record.timestamp(),
                record.operator(),
                record.action(),
                record.entityType(),
                record.entityId(),
                record.amountCents().orElse(null),
                record.concept()
        );
        auditLogDao.insert(entity);
    }
}
