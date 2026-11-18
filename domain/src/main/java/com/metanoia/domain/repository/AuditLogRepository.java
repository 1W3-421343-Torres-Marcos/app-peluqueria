package com.metanoia.domain.repository;

import com.metanoia.domain.model.AuditRecord;

/**
 * Repositorio de registro de auditoría inmutable.
 */
public interface AuditLogRepository {

    void record(AuditRecord record);
}
