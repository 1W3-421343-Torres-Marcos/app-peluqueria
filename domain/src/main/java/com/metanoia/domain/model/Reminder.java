package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Registro inmutable de un recordatorio o saludo enviado por WhatsApp.
 */
public final class Reminder {

    private final String id;
    private final ReminderType type;
    private final String appointmentId;
    private final String clientId;
    private final Integer year;
    private final long sentAt;
    private final String sentBy;

    private Reminder(Builder builder) {
        if (builder.id == null || builder.id.trim().isEmpty()) {
            throw new IllegalArgumentException("id obligatorio");
        }
        if (builder.clientId == null || builder.clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId obligatorio");
        }
        if (builder.sentBy == null || builder.sentBy.trim().isEmpty()) {
            throw new IllegalArgumentException("sentBy obligatorio");
        }
        this.id = builder.id.trim();
        this.type = Objects.requireNonNull(builder.type, "type");
        this.appointmentId = normalizeOptional(builder.appointmentId);
        this.clientId = builder.clientId.trim();
        this.year = builder.year;
        this.sentAt = builder.sentAt;
        this.sentBy = builder.sentBy.trim();
    }

    public String id() {
        return id;
    }

    public ReminderType type() {
        return type;
    }

    public Optional<String> appointmentId() {
        return Optional.ofNullable(appointmentId);
    }

    public String clientId() {
        return clientId;
    }

    public Optional<Integer> year() {
        return Optional.ofNullable(year);
    }

    public long sentAt() {
        return sentAt;
    }

    public String sentBy() {
        return sentBy;
    }

    private static String normalizeOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Builder {
        private String id;
        private ReminderType type;
        private String appointmentId;
        private String clientId;
        private Integer year;
        private long sentAt;
        private String sentBy;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(ReminderType type) {
            this.type = type;
            return this;
        }

        public Builder appointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder sentAt(long sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder sentBy(String sentBy) {
            this.sentBy = sentBy;
            return this;
        }

        public Reminder build() {
            return new Reminder(this);
        }
    }
}
