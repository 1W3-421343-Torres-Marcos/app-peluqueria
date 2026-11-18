package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Movimiento inmutable de stock con cantidad firmada según el tipo de operación.
 */
public final class StockMovement {

    private static final int MIN_REASON_LENGTH = 2;

    private final String id;
    private final String productId;
    private final long timestamp;
    private final StockMovementType type;
    private final int quantity;
    private final String reason;
    private final String cashMovementId;
    private final String operator;

    private StockMovement(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.productId = requireNonBlank(builder.productId, "productId");
        this.timestamp = builder.timestamp;
        this.type = Objects.requireNonNull(builder.type, "type");
        this.quantity = builder.quantity;
        if (this.quantity == 0) {
            throw new IllegalArgumentException("La cantidad del movimiento no puede ser cero");
        }
        if (this.type == StockMovementType.IN && this.quantity < 0) {
            throw new IllegalArgumentException("Un ingreso de stock requiere cantidad positiva");
        }
        if ((this.type == StockMovementType.OUT_USE || this.type == StockMovementType.OUT_SALE)
                && this.quantity > 0) {
            throw new IllegalArgumentException("Un egreso de stock requiere cantidad negativa");
        }
        this.reason = normalizeOptional(builder.reason);
        if (this.type == StockMovementType.ADJUSTMENT
                && (this.reason == null || this.reason.length() < MIN_REASON_LENGTH)) {
            throw new IllegalArgumentException(
                    "Un ajuste de stock requiere un motivo de al menos 2 caracteres");
        }
        this.cashMovementId = normalizeOptional(builder.cashMovementId);
        this.operator = requireNonBlank(builder.operator, "operator");
    }

    public String id() {
        return id;
    }

    public String productId() {
        return productId;
    }

    public long timestamp() {
        return timestamp;
    }

    public StockMovementType type() {
        return type;
    }

    public int quantity() {
        return quantity;
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    public Optional<String> cashMovementId() {
        return Optional.ofNullable(cashMovementId);
    }

    public String operator() {
        return operator;
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
        private String productId;
        private long timestamp;
        private StockMovementType type;
        private int quantity;
        private String reason;
        private String cashMovementId;
        private String operator;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(StockMovementType type) {
            this.type = type;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder cashMovementId(String cashMovementId) {
            this.cashMovementId = cashMovementId;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public StockMovement build() {
            return new StockMovement(this);
        }
    }
}
