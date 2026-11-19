package com.metanoia.domain.model;

import java.util.Objects;

/**
 * Renglón inmutable que vincula una liquidación con un turno liquidado o un ajuste por reverso.
 */
public final class CommissionSettlementEntry {

    private final String id;
    private final String settlementId;
    private final String appointmentId;
    private final CommissionSettlementEntryType type;
    private final long baseCents;
    private final long commissionCents;

    private CommissionSettlementEntry(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.settlementId = requireNonBlank(builder.settlementId, "settlementId");
        this.appointmentId = requireNonBlank(builder.appointmentId, "appointmentId");
        this.type = Objects.requireNonNull(builder.type, "type");
        this.baseCents = builder.baseCents;
        this.commissionCents = builder.commissionCents;
        if (this.type == CommissionSettlementEntryType.SETTLED) {
            if (this.baseCents < 0L || this.commissionCents < 0L) {
                throw new IllegalArgumentException(
                        "Una entrada SETTLED no puede tener importes negativos");
            }
        } else {
            if (this.baseCents > 0L || this.commissionCents >= 0L) {
                throw new IllegalArgumentException(
                        "Un ajuste REVERSAL_ADJUSTMENT requiere comisión negativa y base <= 0");
            }
        }
    }

    public String id() {
        return id;
    }

    public String settlementId() {
        return settlementId;
    }

    public String appointmentId() {
        return appointmentId;
    }

    public CommissionSettlementEntryType type() {
        return type;
    }

    public long baseCents() {
        return baseCents;
    }

    public long commissionCents() {
        return commissionCents;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " obligatorio");
        }
        return value.trim();
    }

    public static final class Builder {
        private String id;
        private String settlementId;
        private String appointmentId;
        private CommissionSettlementEntryType type;
        private long baseCents;
        private long commissionCents;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder settlementId(String settlementId) {
            this.settlementId = settlementId;
            return this;
        }

        public Builder appointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
            return this;
        }

        public Builder type(CommissionSettlementEntryType type) {
            this.type = type;
            return this;
        }

        public Builder baseCents(long baseCents) {
            this.baseCents = baseCents;
            return this;
        }

        public Builder commissionCents(long commissionCents) {
            this.commissionCents = commissionCents;
            return this;
        }

        public CommissionSettlementEntry build() {
            return new CommissionSettlementEntry(this);
        }
    }
}
