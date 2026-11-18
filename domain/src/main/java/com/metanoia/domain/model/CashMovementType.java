package com.metanoia.domain.model;

/**
 * Tipos de movimiento contable inmutable registrados en la caja diaria.
 */
public enum CashMovementType {
    APPOINTMENT_PAYMENT(true),
    PRODUCT_SALE(true),
    OTHER_INCOME(true),
    EXPENSE(false),
    COMMISSION_PAYOUT(false),
    REVERSAL(false);

    private final boolean standardIncome;

    CashMovementType(boolean standardIncome) {
        this.standardIncome = standardIncome;
    }

    public boolean isStandardIncome() {
        return standardIncome;
    }

    public boolean isStandardOutflow() {
        return this == EXPENSE || this == COMMISSION_PAYOUT;
    }
}
