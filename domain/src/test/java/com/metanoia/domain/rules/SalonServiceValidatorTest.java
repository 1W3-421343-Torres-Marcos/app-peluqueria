package com.metanoia.domain.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Money;

import org.junit.Test;

public class SalonServiceValidatorTest {

    @Test
    public void isValidNameAndCategory_checksLengthBounds() {
        assertTrue(SalonServiceValidator.isValidName("Corte dama"));
        assertFalse(SalonServiceValidator.isValidName("A"));
        assertFalse(SalonServiceValidator.isValidName("   "));
        assertFalse(SalonServiceValidator.isValidName(null));

        assertTrue(SalonServiceValidator.isValidCategory("Color"));
        assertFalse(SalonServiceValidator.isValidCategory("C"));
        assertFalse(SalonServiceValidator.isValidCategory(null));
    }

    @Test
    public void isValidPrice_allowsZeroOrPositiveOnly() {
        assertTrue(SalonServiceValidator.isValidPrice(Money.zero()));
        assertTrue(SalonServiceValidator.isValidPrice(Money.ofCents(1_500_00L)));
        assertFalse(SalonServiceValidator.isValidPrice(Money.ofCents(-1L)));
        assertFalse(SalonServiceValidator.isValidPrice(null));
    }

    @Test
    public void isValidDuration_mustBePositiveMultipleOfSlotMinutes() {
        assertTrue(SalonServiceValidator.isValidDuration(15, 15));
        assertTrue(SalonServiceValidator.isValidDuration(45, 15));
        assertTrue(SalonServiceValidator.isValidDuration(60, 20));

        assertFalse(SalonServiceValidator.isValidDuration(0, 15));
        assertFalse(SalonServiceValidator.isValidDuration(-15, 15));
        assertFalse(SalonServiceValidator.isValidDuration(25, 15));
        assertFalse(SalonServiceValidator.isValidDuration(15, 0));
    }
}
