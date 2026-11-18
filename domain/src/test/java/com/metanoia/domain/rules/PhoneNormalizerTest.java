package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhoneNormalizerTest {

    @Test
    public void normalize_ambaFormats_returnCanonical54911() {
        String expected = "5491122334455";
        assertEquals(expected, PhoneNormalizer.normalize("11 2233-4455"));
        assertEquals(expected, PhoneNormalizer.normalize("011 15-2233-4455"));
        assertEquals(expected, PhoneNormalizer.normalize("+54 9 11 2233-4455"));
        assertEquals(expected, PhoneNormalizer.normalize("5491122334455"));
        assertEquals(expected, PhoneNormalizer.normalize("01122334455"));
        assertEquals(expected, PhoneNormalizer.normalize("111522334455"));
        assertEquals(expected, PhoneNormalizer.normalize("+54 11 15 2233-4455"));
    }

    @Test
    public void normalize_interiorArgentinaFormats_stripZeroAndFifteen() {
        assertEquals("5493515556677", PhoneNormalizer.normalize("0351 15 5556677"));
        assertEquals("5493515556677", PhoneNormalizer.normalize("351 555-6677"));
        assertEquals("5492966223344", PhoneNormalizer.normalize("02966 15-223344"));
        assertEquals("5492966223344", PhoneNormalizer.normalize("+54 9 2966 223344"));
    }

    @Test
    public void isValid_validAndInvalidPhones_checkedCorrectly() {
        assertTrue(PhoneNormalizer.isValid(PhoneNormalizer.normalize("11 2233-4455")));
        assertTrue(PhoneNormalizer.isValid(PhoneNormalizer.normalize("+598 99 123 456")));

        assertFalse(PhoneNormalizer.isValid(PhoneNormalizer.normalize("12345")));
        assertFalse(PhoneNormalizer.isValid(PhoneNormalizer.normalize("")));
        assertFalse(PhoneNormalizer.isValid(PhoneNormalizer.normalize("abc")));
        assertFalse(PhoneNormalizer.isValid(null));
    }
}
