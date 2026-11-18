package com.metanoia.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cierre de caja diario inmutable con saldos esperados, contados y diferencia por método.
 */
public final class CashClosing {

    private final String id;
    private final long day;
    private final Map<PaymentMethod, Money> expectedByMethod;
    private final Map<PaymentMethod, Money> countedByMethod;
    private final Money difference;
    private final String notes;
    private final long closedAt;
    private final String operator;

    private CashClosing(Builder builder) {
        if (builder.id == null || builder.id.trim().isEmpty()) {
            throw new IllegalArgumentException("id obligatorio");
        }
        if (builder.operator == null || builder.operator.trim().isEmpty()) {
            throw new IllegalArgumentException("operator obligatorio");
        }
        this.id = builder.id.trim();
        this.day = builder.day;
        this.expectedByMethod = normalizeMethodMap(
                Objects.requireNonNull(builder.expectedByMethod, "expectedByMethod"));
        this.countedByMethod = normalizeMethodMap(
                Objects.requireNonNull(builder.countedByMethod, "countedByMethod"));
        this.difference = Objects.requireNonNull(builder.difference, "difference");
        this.notes = builder.notes == null ? "" : builder.notes.trim();
        this.closedAt = builder.closedAt;
        this.operator = builder.operator.trim();
    }

    public String id() {
        return id;
    }

    public long day() {
        return day;
    }

    public Map<PaymentMethod, Money> expectedByMethod() {
        return expectedByMethod;
    }

    public Map<PaymentMethod, Money> countedByMethod() {
        return countedByMethod;
    }

    public Money expectedFor(PaymentMethod method) {
        return expectedByMethod.getOrDefault(method, Money.zero());
    }

    public Money countedFor(PaymentMethod method) {
        return countedByMethod.getOrDefault(method, Money.zero());
    }

    public Money totalExpected() {
        Money sum = Money.zero();
        for (Money m : expectedByMethod.values()) {
            sum = sum.plus(m);
        }
        return sum;
    }

    public Money totalCounted() {
        Money sum = Money.zero();
        for (Money m : countedByMethod.values()) {
            sum = sum.plus(m);
        }
        return sum;
    }

    public Money difference() {
        return difference;
    }

    public String notes() {
        return notes;
    }

    public long closedAt() {
        return closedAt;
    }

    public String operator() {
        return operator;
    }

    private static Map<PaymentMethod, Money> normalizeMethodMap(
            Map<PaymentMethod, Money> source
    ) {
        EnumMap<PaymentMethod, Money> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            Money value = source.get(method);
            map.put(method, value == null ? Money.zero() : value);
        }
        return Collections.unmodifiableMap(map);
    }

    public static final class Builder {
        private String id;
        private long day;
        private Map<PaymentMethod, Money> expectedByMethod = new EnumMap<>(PaymentMethod.class);
        private Map<PaymentMethod, Money> countedByMethod = new EnumMap<>(PaymentMethod.class);
        private Money difference = Money.zero();
        private String notes = "";
        private long closedAt;
        private String operator;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder day(long day) {
            this.day = day;
            return this;
        }

        public Builder expectedByMethod(Map<PaymentMethod, Money> expectedByMethod) {
            this.expectedByMethod = expectedByMethod;
            return this;
        }

        public Builder countedByMethod(Map<PaymentMethod, Money> countedByMethod) {
            this.countedByMethod = countedByMethod;
            return this;
        }

        public Builder difference(Money difference) {
            this.difference = difference;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder closedAt(long closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public CashClosing build() {
            return new CashClosing(this);
        }
    }
}
