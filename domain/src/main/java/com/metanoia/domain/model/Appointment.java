package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Turno agendado para un cliente con un profesional en una fecha y rango horario semiabierto.
 */
public final class Appointment {

    private static final int MINUTES_PER_DAY = 1440;

    private final String id;
    private final String clientId;
    private final String professionalId;
    private final long day;
    private final int startMin;
    private final int endMin;
    private final AppointmentStatus status;
    private final Money adjustment;
    private final String notes;
    private final String recurrenceRuleId;
    private final String cancelReason;
    private final List<AppointmentItem> items;
    private final long createdAt;
    private final long updatedAt;
    private final String createdBy;

    private Appointment(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.clientId = Objects.requireNonNull(builder.clientId, "clientId");
        this.professionalId = Objects.requireNonNull(builder.professionalId, "professionalId");
        this.day = builder.day;
        if (builder.startMin < 0 || builder.endMin > MINUTES_PER_DAY
                || builder.startMin >= builder.endMin) {
            throw new IllegalArgumentException(
                    "Rango horario inválido [" + builder.startMin + ", " + builder.endMin + ")");
        }
        this.startMin = builder.startMin;
        this.endMin = builder.endMin;
        this.status = Objects.requireNonNull(builder.status, "status");
        this.adjustment = Objects.requireNonNull(builder.adjustment, "adjustment");
        this.notes = builder.notes == null ? "" : builder.notes;
        this.recurrenceRuleId = blankToNull(builder.recurrenceRuleId);
        this.cancelReason = blankToNull(builder.cancelReason);
        if (builder.items.isEmpty()) {
            throw new IllegalArgumentException("Un turno debe contener al menos un ítem");
        }
        this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = Objects.requireNonNull(builder.createdBy, "createdBy");
    }

    /**
     * Suma las duraciones congeladas de todos los ítems en minutos.
     */
    public static int rawItemsDurationMin(List<AppointmentItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un ítem para calcular duración");
        }
        int total = 0;
        for (AppointmentItem item : items) {
            total += item.durationMinSnapshot();
        }
        return total;
    }

    /**
     * Calcula la duración total de los ítems redondeada hacia arriba al bloque mínimo configurado.
     */
    public static int calculateRoundedDurationMin(List<AppointmentItem> items, int slotMinutes) {
        if (slotMinutes <= 0) {
            throw new IllegalArgumentException("slotMinutes debe ser positivo: " + slotMinutes);
        }
        int raw = rawItemsDurationMin(items);
        return ((raw + slotMinutes - 1) / slotMinutes) * slotMinutes;
    }

    /**
     * Calcula el minuto de finalización sumando la duración redondeada a {@code startMin}.
     */
    public static int calculateEndMin(int startMin, List<AppointmentItem> items, int slotMinutes) {
        return startMin + calculateRoundedDurationMin(items, slotMinutes);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public String professionalId() {
        return professionalId;
    }

    public long day() {
        return day;
    }

    public int startMin() {
        return startMin;
    }

    public int endMin() {
        return endMin;
    }

    public int totalDurationMin() {
        return endMin - startMin;
    }

    public AppointmentStatus status() {
        return status;
    }

    public Money adjustment() {
        return adjustment;
    }

    public String notes() {
        return notes;
    }

    public Optional<String> recurrenceRuleId() {
        return Optional.ofNullable(recurrenceRuleId);
    }

    public Optional<String> cancelReason() {
        return Optional.ofNullable(cancelReason);
    }

    public List<AppointmentItem> items() {
        return items;
    }

    public Money itemsSubtotal() {
        Money sum = Money.zero();
        for (AppointmentItem item : items) {
            sum = sum.plus(item.priceSnapshot());
        }
        return sum;
    }

    public Money totalAmount() {
        long rawTotal = Math.addExact(itemsSubtotal().cents(), adjustment.cents());
        return Money.ofCents(Math.max(0L, rawTotal));
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public String createdBy() {
        return createdBy;
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .clientId(clientId)
                .professionalId(professionalId)
                .day(day)
                .startMin(startMin)
                .endMin(endMin)
                .status(status)
                .adjustment(adjustment)
                .notes(notes)
                .recurrenceRuleId(recurrenceRuleId)
                .cancelReason(cancelReason)
                .items(items)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .createdBy(createdBy);
    }

    public static final class Builder {
        private String id;
        private String clientId;
        private String professionalId;
        private long day;
        private int startMin;
        private int endMin;
        private AppointmentStatus status = AppointmentStatus.PENDING;
        private Money adjustment = Money.zero();
        private String notes = "";
        private String recurrenceRuleId;
        private String cancelReason;
        private List<AppointmentItem> items = Collections.emptyList();
        private long createdAt;
        private long updatedAt;
        private String createdBy = "Dueño";

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder professionalId(String professionalId) {
            this.professionalId = professionalId;
            return this;
        }

        public Builder day(long day) {
            this.day = day;
            return this;
        }

        public Builder startMin(int startMin) {
            this.startMin = startMin;
            return this;
        }

        public Builder endMin(int endMin) {
            this.endMin = endMin;
            return this;
        }

        public Builder status(AppointmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder adjustment(Money adjustment) {
            this.adjustment = adjustment;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes == null ? "" : notes;
            return this;
        }

        public Builder recurrenceRuleId(String recurrenceRuleId) {
            this.recurrenceRuleId = recurrenceRuleId;
            return this;
        }

        public Builder cancelReason(String cancelReason) {
            this.cancelReason = cancelReason;
            return this;
        }

        public Builder items(List<AppointmentItem> items) {
            this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
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

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }
}
