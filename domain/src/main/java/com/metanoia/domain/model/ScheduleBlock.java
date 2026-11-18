package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Bloqueo de agenda para un profesional específico o para todo el salón (día completo o parcial).
 */
public final class ScheduleBlock {

    private static final int MINUTES_PER_DAY = 1440;

    private final String id;
    private final String professionalId;
    private final long startDay;
    private final long endDay;
    private final Integer startMin;
    private final Integer endMin;
    private final ScheduleBlockType type;
    private final String reason;
    private final boolean active;
    private final long createdAt;
    private final long updatedAt;

    private ScheduleBlock(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.professionalId = (builder.professionalId == null
                || builder.professionalId.trim().isEmpty())
                ? null
                : builder.professionalId.trim();
        if (builder.startDay > builder.endDay) {
            throw new IllegalArgumentException(
                    "startDay > endDay: " + builder.startDay + " > " + builder.endDay);
        }
        this.startDay = builder.startDay;
        this.endDay = builder.endDay;
        if ((builder.startMin == null) != (builder.endMin == null)) {
            throw new IllegalArgumentException(
                    "startMin y endMin deben informarse juntos o quedar ambos vacíos");
        }
        if (builder.startMin != null
                && (builder.startMin < 0 || builder.endMin > MINUTES_PER_DAY
                || builder.startMin >= builder.endMin)) {
            throw new IllegalArgumentException(
                    "Rango horario de bloqueo inválido [" + builder.startMin
                            + ", " + builder.endMin + ")");
        }
        this.startMin = builder.startMin;
        this.endMin = builder.endMin;
        this.type = Objects.requireNonNull(builder.type, "type");
        this.reason = Objects.requireNonNull(builder.reason, "reason").trim();
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String id() {
        return id;
    }

    public Optional<String> professionalId() {
        return Optional.ofNullable(professionalId);
    }

    public long startDay() {
        return startDay;
    }

    public long endDay() {
        return endDay;
    }

    public Optional<Integer> startMin() {
        return Optional.ofNullable(startMin);
    }

    public Optional<Integer> endMin() {
        return Optional.ofNullable(endMin);
    }

    public boolean isFullDay() {
        return startMin == null;
    }

    public ScheduleBlockType type() {
        return type;
    }

    public String reason() {
        return reason;
    }

    public boolean isActive() {
        return active;
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public boolean appliesToProfessional(String targetProfessionalId) {
        return professionalId == null || professionalId.equals(targetProfessionalId);
    }

    public boolean appliesToDay(long epochDay) {
        return active && epochDay >= startDay && epochDay <= endDay;
    }

    public boolean overlapsTimeRange(int candidateStartMin, int candidateEndMin) {
        if (isFullDay()) {
            return true;
        }
        return startMin < candidateEndMin && candidateStartMin < endMin;
    }

    public boolean blocksAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        return appliesToDay(appointment.day())
                && appliesToProfessional(appointment.professionalId())
                && overlapsTimeRange(appointment.startMin(), appointment.endMin());
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .professionalId(professionalId)
                .startDay(startDay)
                .endDay(endDay)
                .startMin(startMin)
                .endMin(endMin)
                .type(type)
                .reason(reason)
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    public static final class Builder {
        private String id;
        private String professionalId;
        private long startDay;
        private long endDay;
        private Integer startMin;
        private Integer endMin;
        private ScheduleBlockType type = ScheduleBlockType.OTHER;
        private String reason = "";
        private boolean active = true;
        private long createdAt;
        private long updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder professionalId(String professionalId) {
            this.professionalId = professionalId;
            return this;
        }

        public Builder startDay(long startDay) {
            this.startDay = startDay;
            return this;
        }

        public Builder endDay(long endDay) {
            this.endDay = endDay;
            return this;
        }

        public Builder startMin(Integer startMin) {
            this.startMin = startMin;
            return this;
        }

        public Builder endMin(Integer endMin) {
            this.endMin = endMin;
            return this;
        }

        public Builder type(ScheduleBlockType type) {
            this.type = type;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ScheduleBlock build() {
            return new ScheduleBlock(this);
        }
    }
}
