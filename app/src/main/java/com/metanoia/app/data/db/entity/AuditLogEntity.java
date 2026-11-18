package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Registro de auditoría inmutable para operaciones sensibles (caja, stock, agenda, configuración).
 */
@Entity(
        tableName = "audit_logs",
        indices = {
                @Index("timestamp"),
                @Index(value = {"entity_type", "entity_id"})
        }
)
public final class AuditLogEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    public final long timestamp;

    @NonNull
    public final String operator;

    @NonNull
    public final String action;

    @ColumnInfo(name = "entity_type")
    @NonNull
    public final String entityType;

    @ColumnInfo(name = "entity_id")
    @NonNull
    public final String entityId;

    @ColumnInfo(name = "amount_cents")
    @Nullable
    public final Long amountCents;

    @NonNull
    public final String concept;

    public AuditLogEntity(
            @NonNull String id,
            long timestamp,
            @NonNull String operator,
            @NonNull String action,
            @NonNull String entityType,
            @NonNull String entityId,
            @Nullable Long amountCents,
            @NonNull String concept
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.operator = operator;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.amountCents = amountCents;
        this.concept = concept;
    }
}
