package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ficha técnica de coloración asociada a un cliente y opcionalmente a un turno.
 */
public final class ColorFormula {

    public static final int MIN_EXPOSURE_MIN = 0;
    public static final int MAX_EXPOSURE_MIN = 240;
    public static final int MAX_DEVELOPER_VOLUME_LENGTH = 40;
    public static final int MAX_NOTES_LENGTH = 500;
    public static final int MAX_RESULT_LENGTH = 500;

    private final String id;
    private final String clientId;
    private final String appointmentId;
    private final String professionalId;
    private final long day;
    private final int exposureMin;
    private final String developerVolume;
    private final String notes;
    private final String result;
    private final Long deletedAt;
    private final long createdAt;
    private final List<ColorFormulaComponent> components;

    private ColorFormula(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.clientId = requireNonBlank(builder.clientId, "clientId");
        this.appointmentId = normalizeNullableId(builder.appointmentId);
        this.professionalId = normalizeNullableId(builder.professionalId);
        this.day = builder.day;
        if (builder.exposureMin < MIN_EXPOSURE_MIN || builder.exposureMin > MAX_EXPOSURE_MIN) {
            throw new IllegalArgumentException("exposureMin fuera de rango [0, 240]: "
                    + builder.exposureMin);
        }
        this.exposureMin = builder.exposureMin;
        this.developerVolume = normalizeOptional(
                builder.developerVolume, MAX_DEVELOPER_VOLUME_LENGTH, "developerVolume");
        this.notes = normalizeNotes(builder.notes);
        this.result = normalizeOptional(builder.result, MAX_RESULT_LENGTH, "result");
        this.deletedAt = builder.deletedAt;
        this.createdAt = builder.createdAt;
        if (builder.components == null || builder.components.isEmpty()) {
            throw new IllegalArgumentException("La fórmula debe tener al menos un componente");
        }
        List<ColorFormulaComponent> sorted = new ArrayList<>(builder.components);
        sorted.sort(Comparator.comparingInt(ColorFormulaComponent::position));
        this.components = Collections.unmodifiableList(sorted);
    }

    public String id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public Optional<String> appointmentId() {
        return Optional.ofNullable(appointmentId);
    }

    public Optional<String> professionalId() {
        return Optional.ofNullable(professionalId);
    }

    public long day() {
        return day;
    }

    public int exposureMin() {
        return exposureMin;
    }

    public Optional<String> developerVolume() {
        return Optional.ofNullable(developerVolume);
    }

    public String notes() {
        return notes;
    }

    public Optional<String> result() {
        return Optional.ofNullable(result);
    }

    public Optional<Long> deletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public long createdAt() {
        return createdAt;
    }

    public List<ColorFormulaComponent> components() {
        return components;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public String mainShadesSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(components.get(i).summaryPart());
        }
        return sb.toString();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .clientId(clientId)
                .appointmentId(appointmentId)
                .professionalId(professionalId)
                .day(day)
                .exposureMin(exposureMin)
                .developerVolume(developerVolume)
                .notes(notes)
                .result(result)
                .deletedAt(deletedAt)
                .createdAt(createdAt)
                .components(components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColorFormula)) {
            return false;
        }
        ColorFormula that = (ColorFormula) o;
        return day == that.day
                && exposureMin == that.exposureMin
                && createdAt == that.createdAt
                && id.equals(that.id)
                && clientId.equals(that.clientId)
                && Objects.equals(appointmentId, that.appointmentId)
                && Objects.equals(professionalId, that.professionalId)
                && Objects.equals(developerVolume, that.developerVolume)
                && notes.equals(that.notes)
                && Objects.equals(result, that.result)
                && Objects.equals(deletedAt, that.deletedAt)
                && components.equals(that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clientId, appointmentId, professionalId, day, exposureMin,
                developerVolume, notes, result, deletedAt, createdAt, components);
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

    private static String normalizeNotes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("notes excede " + MAX_NOTES_LENGTH + " caracteres");
        }
        return trimmed;
    }

    private static String normalizeOptional(String value, int maxLen, String field) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLen) {
            throw new IllegalArgumentException(field + " excede " + maxLen + " caracteres");
        }
        return trimmed;
    }

    public static final class Builder {
        private String id;
        private String clientId;
        private String appointmentId;
        private String professionalId;
        private long day;
        private int exposureMin;
        private String developerVolume;
        private String notes = "";
        private String result;
        private Long deletedAt;
        private long createdAt;
        private List<ColorFormulaComponent> components = Collections.emptyList();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder appointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
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

        public Builder exposureMin(int exposureMin) {
            this.exposureMin = exposureMin;
            return this;
        }

        public Builder developerVolume(String developerVolume) {
            this.developerVolume = developerVolume;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder result(String result) {
            this.result = result;
            return this;
        }

        public Builder deletedAt(Long deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder components(List<ColorFormulaComponent> components) {
            this.components = components == null
                    ? Collections.emptyList()
                    : new ArrayList<>(components);
            return this;
        }

        public ColorFormula build() {
            return new ColorFormula(this);
        }
    }
}
