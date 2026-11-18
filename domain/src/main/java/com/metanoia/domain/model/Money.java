package com.metanoia.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Representa un importe monetario inmutable expresado en centavos enteros.
 */
public final class Money implements Comparable<Money> {

    private static final Money ZERO = new Money(0L);
    private static final BigDecimal BASIS_POINTS_DIVISOR = BigDecimal.valueOf(10_000L);

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    public static Money ofCents(long cents) {
        return cents == 0L ? ZERO : new Money(cents);
    }

    public static Money zero() {
        return ZERO;
    }

    public long cents() {
        return cents;
    }

    public boolean isZero() {
        return cents == 0L;
    }

    public boolean isPositive() {
        return cents > 0L;
    }

    public boolean isNegative() {
        return cents < 0L;
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "other");
        return ofCents(Math.addExact(this.cents, other.cents));
    }

    public Money minus(Money other) {
        Objects.requireNonNull(other, "other");
        return ofCents(Math.subtractExact(this.cents, other.cents));
    }

    public Money multiply(int factor) {
        return ofCents(Math.multiplyExact(this.cents, (long) factor));
    }

    /**
     * Calcula un porcentaje expresado en puntos básicos ({@code 10_000} = 100,00 %)
     * con redondeo half-up al centavo más cercano.
     */
    public Money percentage(int basisPoints) {
        if (cents == 0L || basisPoints == 0) {
            return ZERO;
        }
        BigDecimal result = BigDecimal.valueOf(cents)
                .multiply(BigDecimal.valueOf(basisPoints))
                .divide(BASIS_POINTS_DIVISOR, 0, RoundingMode.HALF_UP);
        return ofCents(result.longValueExact());
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other");
        return Long.compare(this.cents, other.cents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money)) {
            return false;
        }
        Money money = (Money) o;
        return cents == money.cents;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(cents);
    }

    @Override
    public String toString() {
        return "Money{cents=" + cents + '}';
    }
}
