package com.metanoia.domain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Servicio del catálogo del salón con precio base, duración estimada y bandera de coloración.
 */
public final class SalonService {

    public static final List<String> SUGGESTED_CATEGORIES = Collections.unmodifiableList(
            Arrays.asList(
                    "Corte",
                    "Color",
                    "Mechas",
                    "Alisado",
                    "Tratamiento",
                    "Peinado",
                    "Barba"
            ));

    private final String id;
    private final String name;
    private final String category;
    private final Money price;
    private final int durationMin;
    private final boolean colorService;
    private final boolean active;
    private final int sortOrder;
    private final long createdAt;
    private final long updatedAt;

    private SalonService(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = Objects.requireNonNull(builder.name, "name");
        this.category = Objects.requireNonNull(builder.category, "category");
        this.price = Objects.requireNonNull(builder.price, "price");
        this.durationMin = builder.durationMin;
        this.colorService = builder.colorService;
        this.active = builder.active;
        this.sortOrder = builder.sortOrder;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String category() {
        return category;
    }

    public Money price() {
        return price;
    }

    public int durationMin() {
        return durationMin;
    }

    public boolean isColorService() {
        return colorService;
    }

    public boolean isActive() {
        return active;
    }

    public int sortOrder() {
        return sortOrder;
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
                .category(category)
                .price(price)
                .durationMin(durationMin)
                .colorService(colorService)
                .active(active)
                .sortOrder(sortOrder)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    public static final class Builder {
        private String id;
        private String name;
        private String category;
        private Money price = Money.zero();
        private int durationMin;
        private boolean colorService;
        private boolean active = true;
        private int sortOrder;
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

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder price(Money price) {
            this.price = price;
            return this;
        }

        public Builder durationMin(int durationMin) {
            this.durationMin = durationMin;
            return this;
        }

        public Builder colorService(boolean colorService) {
            this.colorService = colorService;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
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

        public SalonService build() {
            return new SalonService(this);
        }
    }
}
