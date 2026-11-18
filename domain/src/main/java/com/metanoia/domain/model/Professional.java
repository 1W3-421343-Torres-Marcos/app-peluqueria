package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Profesional del salón con su esquema de comisión, horario semanal, servicios y días libres.
 */
public final class Professional {

    private final String id;
    private final String name;
    private final String phone;
    private final String colorTag;
    private final int commissionBasisPoints;
    private final boolean active;
    private final List<TimeRange> workSchedules;
    private final Set<String> serviceIds;
    private final Set<Long> daysOff;
    private final long createdAt;
    private final long updatedAt;

    private Professional(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = Objects.requireNonNull(builder.name, "name");
        this.phone = builder.phone;
        this.colorTag = builder.colorTag;
        this.commissionBasisPoints = builder.commissionBasisPoints;
        this.active = builder.active;
        this.workSchedules = Collections.unmodifiableList(new ArrayList<>(builder.workSchedules));
        this.serviceIds = Collections.unmodifiableSet(new LinkedHashSet<>(builder.serviceIds));
        this.daysOff = Collections.unmodifiableSet(new LinkedHashSet<>(builder.daysOff));
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String phone() {
        return phone;
    }

    public String colorTag() {
        return colorTag;
    }

    public int commissionBasisPoints() {
        return commissionBasisPoints;
    }

    public boolean isActive() {
        return active;
    }

    public List<TimeRange> workSchedules() {
        return workSchedules;
    }

    public Set<String> serviceIds() {
        return serviceIds;
    }

    public Set<Long> daysOff() {
        return daysOff;
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
                .phone(phone)
                .colorTag(colorTag)
                .commissionBasisPoints(commissionBasisPoints)
                .active(active)
                .workSchedules(workSchedules)
                .serviceIds(serviceIds)
                .daysOff(daysOff)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    public static final class Builder {
        private String id;
        private String name;
        private String phone = "";
        private String colorTag = "";
        private int commissionBasisPoints;
        private boolean active = true;
        private List<TimeRange> workSchedules = Collections.emptyList();
        private Set<String> serviceIds = Collections.emptySet();
        private Set<Long> daysOff = Collections.emptySet();
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

        public Builder phone(String phone) {
            this.phone = phone == null ? "" : phone;
            return this;
        }

        public Builder colorTag(String colorTag) {
            this.colorTag = colorTag == null ? "" : colorTag;
            return this;
        }

        public Builder commissionBasisPoints(int commissionBasisPoints) {
            this.commissionBasisPoints = commissionBasisPoints;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder workSchedules(List<TimeRange> workSchedules) {
            this.workSchedules = new ArrayList<>(
                    Objects.requireNonNull(workSchedules, "workSchedules"));
            return this;
        }

        public Builder serviceIds(Set<String> serviceIds) {
            this.serviceIds = new LinkedHashSet<>(
                    Objects.requireNonNull(serviceIds, "serviceIds"));
            return this;
        }

        public Builder daysOff(Set<Long> daysOff) {
            this.daysOff = new LinkedHashSet<>(Objects.requireNonNull(daysOff, "daysOff"));
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

        public Professional build() {
            return new Professional(this);
        }
    }
}
