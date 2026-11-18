package com.metanoia.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Solicitud de un cliente en lista de espera con preferencias de profesional, días y franja horaria.
 */
public final class WaitlistEntry {

    public static final int MAX_NOTES_LENGTH = 300;
    public static final int MINUTES_PER_DAY = 1440;

    private final String id;
    private final String clientId;
    private final List<String> serviceIds;
    private final String professionalId;
    private final Long specificDay;
    private final Set<Integer> preferredDaysOfWeek;
    private final int fromMin;
    private final int toMin;
    private final long validUntilDay;
    private final WaitlistStatus status;
    private final String notes;
    private final long createdAt;
    private final Long offeredAt;

    private WaitlistEntry(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.clientId = requireNonBlank(builder.clientId, "clientId");
        Set<String> deduplicatedServices = new LinkedHashSet<>();
        if (builder.serviceIds != null) {
            for (String rawServiceId : builder.serviceIds) {
                if (rawServiceId != null && !rawServiceId.trim().isEmpty()) {
                    deduplicatedServices.add(rawServiceId.trim());
                }
            }
        }
        if (deduplicatedServices.isEmpty()) {
            throw new IllegalArgumentException("La entrada requiere al menos un servicio");
        }
        this.serviceIds = Collections.unmodifiableList(new ArrayList<>(deduplicatedServices));
        this.professionalId = normalizeNullableId(builder.professionalId);
        this.specificDay = builder.specificDay;
        TreeSet<Integer> normalizedDays = new TreeSet<>();
        if (builder.preferredDaysOfWeek != null) {
            for (Integer dayOfWeek : builder.preferredDaysOfWeek) {
                if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
                    throw new IllegalArgumentException("Día de semana inválido: " + dayOfWeek);
                }
                normalizedDays.add(dayOfWeek);
            }
        }
        this.preferredDaysOfWeek = Collections.unmodifiableSet(normalizedDays);
        if (this.specificDay == null && this.preferredDaysOfWeek.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe definirse fecha puntual o al menos un día de semana");
        }
        if (builder.fromMin < 0 || builder.toMin > MINUTES_PER_DAY
                || builder.fromMin >= builder.toMin) {
            throw new IllegalArgumentException(
                    "Franja horaria inválida: " + builder.fromMin + ".." + builder.toMin);
        }
        this.fromMin = builder.fromMin;
        this.toMin = builder.toMin;
        this.validUntilDay = builder.validUntilDay;
        this.status = Objects.requireNonNull(builder.status, "status");
        String trimmedNotes = builder.notes == null ? "" : builder.notes.trim();
        if (trimmedNotes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException(
                    "notes excede " + MAX_NOTES_LENGTH + " caracteres");
        }
        this.notes = trimmedNotes;
        this.createdAt = builder.createdAt;
        this.offeredAt = builder.offeredAt;
    }

    public String id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public List<String> serviceIds() {
        return serviceIds;
    }

    public Optional<String> professionalId() {
        return Optional.ofNullable(professionalId);
    }

    public Optional<Long> specificDay() {
        return Optional.ofNullable(specificDay);
    }

    public Set<Integer> preferredDaysOfWeek() {
        return preferredDaysOfWeek;
    }

    public int fromMin() {
        return fromMin;
    }

    public int toMin() {
        return toMin;
    }

    public long validUntilDay() {
        return validUntilDay;
    }

    public WaitlistStatus status() {
        return status;
    }

    public String notes() {
        return notes;
    }

    public long createdAt() {
        return createdAt;
    }

    public Optional<Long> offeredAt() {
        return Optional.ofNullable(offeredAt);
    }

    public boolean matchesDay(long epochDay) {
        if (specificDay != null) {
            return specificDay == epochDay;
        }
        int isoDayOfWeek = LocalDate.ofEpochDay(epochDay).getDayOfWeek().getValue();
        return preferredDaysOfWeek.contains(isoDayOfWeek);
    }

    public boolean hasSamePreferenceAs(WaitlistEntry other) {
        if (other == null) {
            return false;
        }
        return clientId.equals(other.clientId)
                && new HashSet<>(serviceIds).equals(new HashSet<>(other.serviceIds))
                && Objects.equals(professionalId, other.professionalId)
                && Objects.equals(specificDay, other.specificDay)
                && preferredDaysOfWeek.equals(other.preferredDaysOfWeek)
                && fromMin == other.fromMin
                && toMin == other.toMin;
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .clientId(clientId)
                .serviceIds(serviceIds)
                .professionalId(professionalId)
                .specificDay(specificDay)
                .preferredDaysOfWeek(preferredDaysOfWeek)
                .fromMin(fromMin)
                .toMin(toMin)
                .validUntilDay(validUntilDay)
                .status(status)
                .notes(notes)
                .createdAt(createdAt)
                .offeredAt(offeredAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WaitlistEntry)) {
            return false;
        }
        WaitlistEntry that = (WaitlistEntry) o;
        return fromMin == that.fromMin
                && toMin == that.toMin
                && validUntilDay == that.validUntilDay
                && createdAt == that.createdAt
                && id.equals(that.id)
                && clientId.equals(that.clientId)
                && serviceIds.equals(that.serviceIds)
                && Objects.equals(professionalId, that.professionalId)
                && Objects.equals(specificDay, that.specificDay)
                && preferredDaysOfWeek.equals(that.preferredDaysOfWeek)
                && status == that.status
                && notes.equals(that.notes)
                && Objects.equals(offeredAt, that.offeredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clientId, serviceIds, professionalId, specificDay,
                preferredDaysOfWeek, fromMin, toMin, validUntilDay, status, notes, createdAt,
                offeredAt);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " obligatorio");
        }
        return value.trim();
    }

    private static String normalizeNullableId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Builder {
        private String id;
        private String clientId;
        private List<String> serviceIds = Collections.emptyList();
        private String professionalId;
        private Long specificDay;
        private Set<Integer> preferredDaysOfWeek = Collections.emptySet();
        private int fromMin;
        private int toMin;
        private long validUntilDay;
        private WaitlistStatus status = WaitlistStatus.ACTIVE;
        private String notes = "";
        private long createdAt;
        private Long offeredAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder serviceIds(List<String> serviceIds) {
            this.serviceIds = serviceIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(serviceIds);
            return this;
        }

        public Builder professionalId(String professionalId) {
            this.professionalId = professionalId;
            return this;
        }

        public Builder specificDay(Long specificDay) {
            this.specificDay = specificDay;
            return this;
        }

        public Builder preferredDaysOfWeek(Set<Integer> preferredDaysOfWeek) {
            this.preferredDaysOfWeek = preferredDaysOfWeek == null
                    ? Collections.emptySet()
                    : new LinkedHashSet<>(preferredDaysOfWeek);
            return this;
        }

        public Builder fromMin(int fromMin) {
            this.fromMin = fromMin;
            return this;
        }

        public Builder toMin(int toMin) {
            this.toMin = toMin;
            return this;
        }

        public Builder validUntilDay(long validUntilDay) {
            this.validUntilDay = validUntilDay;
            return this;
        }

        public Builder status(WaitlistStatus status) {
            this.status = status;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder offeredAt(Long offeredAt) {
            this.offeredAt = offeredAt;
            return this;
        }

        public WaitlistEntry build() {
            return new WaitlistEntry(this);
        }
    }
}
