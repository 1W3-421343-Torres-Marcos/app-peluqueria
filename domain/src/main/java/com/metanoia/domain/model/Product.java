package com.metanoia.domain.model;

import com.metanoia.domain.rules.LowStockRule;
import com.metanoia.domain.rules.TextNormalizer;

import java.util.Objects;
import java.util.Optional;

/**
 * Producto de uso interno y/o venta del salón con stock cacheado desde movimientos inmutables.
 */
public final class Product {

    private final String id;
    private final String name;
    private final String nameNormalized;
    private final String brand;
    private final ProductType type;
    private final ProductUnit unit;
    private final Money salePrice;
    private final int minStock;
    private final int currentStock;
    private final boolean active;
    private final long createdAt;
    private final long updatedAt;

    private Product(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.name = requireNonBlank(builder.name, "name");
        String normalized = builder.nameNormalized == null || builder.nameNormalized.trim().isEmpty()
                ? TextNormalizer.normalize(this.name)
                : builder.nameNormalized.trim();
        this.nameNormalized = requireNonBlank(normalized, "nameNormalized");
        this.brand = normalizeOptional(builder.brand);
        this.type = Objects.requireNonNull(builder.type, "type");
        this.unit = Objects.requireNonNull(builder.unit, "unit");
        if (this.type.isForSale()) {
            if (builder.salePrice == null || !builder.salePrice.isPositive()) {
                throw new IllegalArgumentException(
                        "Un producto de venta requiere un precio mayor a cero");
            }
            this.salePrice = builder.salePrice;
        } else {
            this.salePrice = null;
        }
        if (builder.minStock < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        this.minStock = builder.minStock;
        this.currentStock = builder.currentStock;
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

    public Optional<String> brand() {
        return Optional.ofNullable(brand);
    }

    public ProductType type() {
        return type;
    }

    public ProductUnit unit() {
        return unit;
    }

    public Optional<Money> salePrice() {
        return Optional.ofNullable(salePrice);
    }

    public int minStock() {
        return minStock;
    }

    public int currentStock() {
        return currentStock;
    }

    public boolean active() {
        return active;
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public boolean isLowStock() {
        return LowStockRule.isLowStock(this);
    }

    public boolean isNegativeStock() {
        return LowStockRule.isNegativeStock(this);
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .name(name)
                .nameNormalized(nameNormalized)
                .brand(brand)
                .type(type)
                .unit(unit)
                .salePrice(salePrice)
                .minStock(minStock)
                .currentStock(currentStock)
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " obligatorio");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String nameNormalized;
        private String brand;
        private ProductType type;
        private ProductUnit unit;
        private Money salePrice;
        private int minStock;
        private int currentStock;
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

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder type(ProductType type) {
            this.type = type;
            return this;
        }

        public Builder unit(ProductUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder salePrice(Money salePrice) {
            this.salePrice = salePrice;
            return this;
        }

        public Builder minStock(int minStock) {
            this.minStock = minStock;
            return this;
        }

        public Builder currentStock(int currentStock) {
            this.currentStock = currentStock;
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

        public Product build() {
            return new Product(this);
        }
    }
}
