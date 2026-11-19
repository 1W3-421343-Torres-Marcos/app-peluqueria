package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinHasherTest {

    @Test
    public void isValidPinFormat_acceptsOnlyFourToSixDigits() {
        assertTrue(PinHasher.isValidPinFormat("1234"));
        assertTrue(PinHasher.isValidPinFormat("009911"));
        assertFalse(PinHasher.isValidPinFormat(null));
        assertFalse(PinHasher.isValidPinFormat("123"));
        assertFalse(PinHasher.isValidPinFormat("1234567"));
        assertFalse(PinHasher.isValidPinFormat("12a4"));
    }

    @Test
    public void hashAndVerify_usesUniqueRandomSaltAndValidatesPin() {
        PinHasher.HashedPin first = PinHasher.hash("2580", 1_000);
        PinHasher.HashedPin second = PinHasher.hash("2580", 1_000);

        assertNotEquals(first.saltBase64(), second.saltBase64());
        assertNotEquals(first.hashBase64(), second.hashBase64());
        assertFalse(first.hashBase64().contains("2580"));

        assertTrue(PinHasher.verify(
                "2580", first.saltBase64(), first.hashBase64(), first.iterations()));
        assertFalse(PinHasher.verify(
                "2581", first.saltBase64(), first.hashBase64(), first.iterations()));
        assertFalse(PinHasher.verify(
                "2580", "not-base64!", first.hashBase64(), first.iterations()));
    }
}
