package com.metanoia.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void plusAndMinus_validAmounts_computesExactCents() {
        Money a = Money.ofCents(12_345_50L);
        Money b = Money.ofCents(1_000_25L);

        assertEquals(Money.ofCents(13_345_75L), a.plus(b));
        assertEquals(Money.ofCents(11_345_25L), a.minus(b));
        assertEquals(Money.zero(), a.minus(a));
    }

    @Test
    public void multiply_positiveAndZero_computesExactCents() {
        Money price = Money.ofCents(2_500_00L);

        assertEquals(Money.ofCents(7_500_00L), price.multiply(3));
        assertEquals(Money.zero(), price.multiply(0));
    }

    @Test
    public void percentage_halfUpRounding_roundsAwayFromZeroAtHalfCent() {
        // 101 centavos * 50,00 % (5000 bp) = 50,5 -> 51 centavos
        assertEquals(Money.ofCents(51L), Money.ofCents(101L).percentage(5_000));
        // 101 centavos * 49,00 % (4900 bp) = 49,49 -> 49 centavos
        assertEquals(Money.ofCents(49L), Money.ofCents(101L).percentage(4_900));
        // 10.000 centavos * 33,33 % (3333 bp) = 3.333 centavos
        assertEquals(Money.ofCents(3_333L), Money.ofCents(10_000L).percentage(3_333));
        // Cero o 0 %
        assertEquals(Money.zero(), Money.ofCents(999L).percentage(0));
        assertEquals(Money.zero(), Money.zero().percentage(2_500));
    }

    @Test
    public void signsAndComparison_workCorrectly() {
        Money negative = Money.ofCents(-500L);
        Money zero = Money.zero();
        Money positive = Money.ofCents(500L);

        assertTrue(negative.isNegative());
        assertFalse(negative.isPositive());
        assertTrue(zero.isZero());
        assertTrue(positive.isPositive());
        assertTrue(negative.compareTo(zero) < 0);
        assertTrue(positive.compareTo(zero) > 0);
    }

    @Test
    public void plus_overflow_throwsArithmeticException() {
        Money max = Money.ofCents(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> max.plus(Money.ofCents(1L)));
    }
}
