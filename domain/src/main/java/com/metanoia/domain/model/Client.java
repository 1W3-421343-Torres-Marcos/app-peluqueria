package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Cliente registrado en el salón con teléfono normalizado, cumpleaños opcional y ausencias.
 */
public final class Client {

    private final String id;
    private final String name;
    private final String nameNormalized;
    private final String phone;
    private final Integer birthDay;
    private final Integer birthMonth;
    private final Integer birthYear;
    private final String notes;
    private final int noShowCount;
    private final boolean active;
    private final long createdAt;
    private final long updatedAt;

    private Client(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = Objects.requireNonNull(builder.name, "name");
        this.nameNormalized = Objects.requireNonNull(builder.nameNormalized, "nameNormalized");
        this.phone = Objects.requireNonNull(builder.phone, "phone");
        this.birthDay = builder.birthDay;
        this.birthMonth = builder.birthMonth;
        this.birthYear = builder.birthYear;
        this.notes = builder.notes == null ? "" : builder.notes;
        this.noShowCount = builder.noShowCount;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String nameNormalized() {
        return nameNormalized;
    }

    public String phone() {
        return phone;
    }

    public Optional<Integer> birthDay() {
        return Optional.ofNullable(birthDay);
    }

    public Optional<Integer> birthMonth() {
        return Optional.ofNullable(birthMonth);
    }

    public Optional<Integer> birthYear() {
        return Optional.ofNullable(birthYear);
    }

    public String notes() {
        return notes;
    }

    public int noShowCount() {
        return noShowCount;
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

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .name(name)
                .nameNormalized(nameNormalized)
                .phone(phone)
                .birthDay(birthDay)
                .birthMonth(birthMonth)
                .birthYear(birthYear)
                .notes(notes)
                .noShowCount(noShowCount)
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    public static final class Builder {
        private String id;
        private String name;
        private String nameNormalized = "";
        private String phone = "";
        private Integer birthDay;
        private Integer birthMonth;
        private Integer birthYear;
        private String notes = "";
        private int noShowCount;
        private boolean active = true;
        private long createdAt;
        private long updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder nameNormalized(String nameNormalized) {
            this.nameNormalized = nameNormalized;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder birthDay(Integer birthDay) {
            this.birthDay = birthDay;
            return this;
        }

        public Builder birthMonth(Integer birthMonth) {
            this.birthMonth = birthMonth;
            return this;
        }

        public Builder birthYear(Integer birthYear) {
            this.birthYear = birthYear;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder noShowCount(int noShowCount) {
            this.noShowCount = noShowCount;
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

        public Client build() {
            return new Client(this);
        }
    }
}
