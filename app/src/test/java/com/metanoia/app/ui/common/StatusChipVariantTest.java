package com.metanoia.app.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StatusChipVariantTest {

    @Test
    public void variants_coverAllSixAppointmentStatesPlusLowStock() {
        StatusChip.Variant[] variants = StatusChip.Variant.values();
        assertEquals(7, variants.length);
        assertEquals(StatusChip.Variant.PENDING, variants[0]);
        assertEquals(StatusChip.Variant.CONFIRMED, variants[1]);
        assertEquals(StatusChip.Variant.COMPLETED, variants[2]);
        assertEquals(StatusChip.Variant.PAID, variants[3]);
        assertEquals(StatusChip.Variant.CANCELLED, variants[4]);
        assertEquals(StatusChip.Variant.NO_SHOW, variants[5]);
        assertEquals(StatusChip.Variant.LOW_STOCK, variants[6]);
    }
}
