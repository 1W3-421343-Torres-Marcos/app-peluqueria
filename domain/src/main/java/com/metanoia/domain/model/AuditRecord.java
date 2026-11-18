package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Evento auditable inmutable generado por cambios de dinero, stock o configuración.
 */
public final class AuditRecord {

    private final String id;
    private final long timestamp;
    private final String operator;
    private final String action;
    private final String entityType;
    private final String entityId;
    private final Long amountCents;
    private final String concept;

    private AuditRecord(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.timestamp = builder.timestamp;
        this.operator = Objects.requireNonNull(builder.operator, "operator");
        this.action = Objects.requireNonNull(builder.action, "action");
        this.entityType = Objects.requireNonNull(builder.entityType, "entityType");
        this.entityId = Objects.requireNonNull(builder.entityId, "entityId");
        this.amountCents = builder.amountCents;
        this.concept = Objects.requireNonNull(builder.concept, "concept");
    }

    public String id() {
        return id;
    }

    public long timestamp() {
        return timestamp;
    }

    public String operator() {
        return operator;
    }

    public String action() {
        return action;
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }

    public Optional<Long> amountCents() {
        return Optional.ofNullable(amountCents);
    }

    public String concept() {
        return concept;
    }

    public static final class Builder {
        private String id;
        private long timestamp;
        private String operator;
        private String action;
        private String entityType;
        private String entityId;
        private Long amountCents;
        private String concept;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder amountCents(Long amountCents) {
            this.amountCents = amountCents;
            return this;
        }

        public Builder concept(String concept) {
            this.concept = concept;
            return this;
        }

        public AuditRecord build() {
            return new AuditRecord(this);
        }
    }
}
