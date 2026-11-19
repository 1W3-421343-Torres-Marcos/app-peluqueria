package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Liquidación histórica e inmutable de comisiones de un profesional para un período cerrado.
 */
public final class CommissionSettlement {

    private final String id;
    private final String professionalId;
    private final long fromDay;
    private final long toDay;
    private final Money baseAmount;
    private final Money commissionAmount;
    private final String cashMovementId;
    private final long settledAt;
    private final String operator;
    private final List<CommissionSettlementEntry> entries;

    private CommissionSettlement(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.professionalId = requireNonBlank(builder.professionalId, "professionalId");
        this.fromDay = builder.fromDay;
        this.toDay = builder.toDay;
        if (this.fromDay > this.toDay) {
            throw new IllegalArgumentException(
                    "El período de liquidación es inválido: " + fromDay + " > " + toDay);
        }
        this.baseAmount = Objects.requireNonNull(builder.baseAmount, "baseAmount");
        this.commissionAmount = Objects.requireNonNull(
                builder.commissionAmount, "commissionAmount");
        if (!this.commissionAmount.isPositive()) {
            throw new IllegalArgumentException("El monto liquidado de comisión debe ser positivo");
        }
        this.cashMovementId = requireNonBlank(builder.cashMovementId, "cashMovementId");
        this.settledAt = builder.settledAt;
        this.operator = requireNonBlank(builder.operator, "operator");
        this.entries = Collections.unmodifiableList(new ArrayList<>(builder.entries));
    }

    public String id() {
        return id;
    }

    public String professionalId() {
        return professionalId;
    }

    public long fromDay() {
        return fromDay;
    }

    public long toDay() {
        return toDay;
    }

    public Money baseAmount() {
        return baseAmount;
    }

    public Money commissionAmount() {
        return commissionAmount;
    }

    public String cashMovementId() {
        return cashMovementId;
    }

    public long settledAt() {
        return settledAt;
    }

    public String operator() {
        return operator;
    }

    public List<CommissionSettlementEntry> entries() {
        return entries;
    }

    public boolean overlaps(long otherFromDay, long otherToDay) {
        if (otherFromDay > otherToDay) {
            throw new IllegalArgumentException("Rango inválido");
        }
        return this.fromDay <= otherToDay && this.toDay >= otherFromDay;
    }

    public boolean coversDay(long day) {
        return day >= this.fromDay && day <= this.toDay;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " obligatorio");
        }
        return value.trim();
    }

    public static final class Builder {
        private String id;
        private String professionalId;
        private long fromDay;
        private long toDay;
        private Money baseAmount = Money.zero();
        private Money commissionAmount = Money.zero();
        private String cashMovementId;
        private long settledAt;
        private String operator;
        private List<CommissionSettlementEntry> entries = Collections.emptyList();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder professionalId(String professionalId) {
            this.professionalId = professionalId;
            return this;
        }

        public Builder fromDay(long fromDay) {
            this.fromDay = fromDay;
            return this;
        }

        public Builder toDay(long toDay) {
            this.toDay = toDay;
            return this;
        }

        public Builder baseAmount(Money baseAmount) {
            this.baseAmount = baseAmount;
            return this;
        }

        public Builder commissionAmount(Money commissionAmount) {
            this.commissionAmount = commissionAmount;
            return this;
        }

        public Builder cashMovementId(String cashMovementId) {
            this.cashMovementId = cashMovementId;
            return this;
        }

        public Builder settledAt(long settledAt) {
            this.settledAt = settledAt;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder entries(List<CommissionSettlementEntry> entries) {
            this.entries = new ArrayList<>(Objects.requireNonNull(entries, "entries"));
            return this;
        }

        public CommissionSettlement build() {
            return new CommissionSettlement(this);
        }
    }
}
