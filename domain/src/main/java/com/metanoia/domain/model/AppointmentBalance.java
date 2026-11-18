package com.metanoia.domain.model;

import java.util.Objects;

/**
 * Resumen económico derivado de un turno: total, importe neto pagado y saldo adeudado.
 */
public final class AppointmentBalance {

    private final Money itemsSubtotal;
    private final Money adjustment;
    private final Money total;
    private final Money paid;
    private final Money balance;

    public AppointmentBalance(Money itemsSubtotal, Money adjustment, Money netPaid) {
        this.itemsSubtotal = Objects.requireNonNull(itemsSubtotal, "itemsSubtotal");
        this.adjustment = Objects.requireNonNull(adjustment, "adjustment");
        this.paid = Objects.requireNonNull(netPaid, "netPaid");
        if (netPaid.isNegative()) {
            throw new IllegalArgumentException("El neto pagado no puede ser negativo");
        }
        long totalCents = Math.max(0L, Math.addExact(itemsSubtotal.cents(), adjustment.cents()));
        this.total = Money.ofCents(totalCents);
        this.balance = Money.ofCents(Math.max(0L, totalCents - netPaid.cents()));
    }

    public static AppointmentBalance of(Appointment appointment, Money netPaid) {
        Objects.requireNonNull(appointment, "appointment");
        return new AppointmentBalance(
                appointment.itemsSubtotal(),
                appointment.adjustment(),
                netPaid
        );
    }

    public Money itemsSubtotal() {
        return itemsSubtotal;
    }

    public Money adjustment() {
        return adjustment;
    }

    public Money total() {
        return total;
    }

    public Money paid() {
        return paid;
    }

    public Money balance() {
        return balance;
    }

    public boolean isFullyPaid() {
        return paid.cents() >= total.cents();
    }

    public boolean hasPayments() {
        return paid.cents() > 0L;
    }
}
