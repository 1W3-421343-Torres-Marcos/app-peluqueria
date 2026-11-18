package com.metanoia.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppointmentBalanceTest {

    @Test
    public void balance_partialAndFullPayment_calculatesDerivedBalance() {
        AppointmentBalance partial = new AppointmentBalance(
                Money.ofCents(3_000_000L),
                Money.ofCents(500_000L),
                Money.ofCents(2_000_000L)
        );
        assertEquals(Money.ofCents(3_500_000L), partial.total());
        assertEquals(Money.ofCents(2_000_000L), partial.paid());
        assertEquals(Money.ofCents(1_500_000L), partial.balance());
        assertFalse(partial.isFullyPaid());
        assertTrue(partial.hasPayments());

        AppointmentBalance settled = new AppointmentBalance(
                Money.ofCents(3_000_000L),
                Money.ofCents(-500_000L),
                Money.ofCents(2_500_000L)
        );
        assertEquals(Money.ofCents(2_500_000L), settled.total());
        assertEquals(Money.zero(), settled.balance());
        assertTrue(settled.isFullyPaid());
    }
}
