package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Movimiento monetario inmutable en caja ({@code amount > 0}; el signo lo define el tipo).
 */
public final class CashMovement {

    private final String id;
    private final long day;
    private final long timestamp;
    private final CashMovementType type;
    private final PaymentMethod method;
    private final Money amount;
    private final String concept;
    private final String appointmentId;
    private final String clientId;
    private final String productId;
    private final Integer quantity;
    private final String reversesMovementId;
    private final String operator;
    private final String closingId;

    private CashMovement(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.day = builder.day;
        this.timestamp = builder.timestamp;
        this.type = Objects.requireNonNull(builder.type, "type");
        this.method = Objects.requireNonNull(builder.method, "method");
        this.amount = Objects.requireNonNull(builder.amount, "amount");
        if (!this.amount.isPositive()) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        this.concept = requireNonBlank(builder.concept, "concept");
        this.appointmentId = normalizeOptional(builder.appointmentId);
        this.clientId = normalizeOptional(builder.clientId);
        this.productId = normalizeOptional(builder.productId);
        this.quantity = builder.quantity;
        if (this.quantity != null && this.quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        this.reversesMovementId = normalizeOptional(builder.reversesMovementId);
        if (this.type == CashMovementType.REVERSAL && this.reversesMovementId == null) {
            throw new IllegalArgumentException(
                    "Un movimiento REVERSAL requiere reversesMovementId");
        }
        if (this.type != CashMovementType.REVERSAL && this.reversesMovementId != null) {
            throw new IllegalArgumentException(
                    "Solo un movimiento REVERSAL puede referenciar reversesMovementId");
        }
        this.operator = requireNonBlank(builder.operator, "operator");
        this.closingId = normalizeOptional(builder.closingId);
    }

    public String id() {
        return id;
    }

    public long day() {
        return day;
    }

    public long timestamp() {
        return timestamp;
    }

    public CashMovementType type() {
        return type;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money amount() {
        return amount;
    }

    public String concept() {
        return concept;
    }

    public Optional<String> appointmentId() {
        return Optional.ofNullable(appointmentId);
    }

    public Optional<String> clientId() {
        return Optional.ofNullable(clientId);
    }

    public Optional<String> productId() {
        return Optional.ofNullable(productId);
    }

    public Optional<Integer> quantity() {
        return Optional.ofNullable(quantity);
    }

    public Optional<String> reversesMovementId() {
        return Optional.ofNullable(reversesMovementId);
    }

    public String operator() {
        return operator;
    }

    public Optional<String> closingId() {
        return Optional.ofNullable(closingId);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " obligatorio");
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
        private long day;
        private long timestamp;
        private CashMovementType type;
        private PaymentMethod method;
        private Money amount;
        private String concept;
        private String appointmentId;
        private String clientId;
        private String productId;
        private Integer quantity;
        private String reversesMovementId;
        private String operator;
        private String closingId;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder day(long day) {
            this.day = day;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(CashMovementType type) {
            this.type = type;
            return this;
        }

        public Builder method(PaymentMethod method) {
            this.method = method;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder concept(String concept) {
            this.concept = concept;
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

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder reversesMovementId(String reversesMovementId) {
            this.reversesMovementId = reversesMovementId;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder closingId(String closingId) {
            this.closingId = closingId;
            return this;
        }

        public CashMovement build() {
            return new CashMovement(this);
        }
    }
}
