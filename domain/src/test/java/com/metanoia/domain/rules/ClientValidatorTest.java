package com.metanoia.domain.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClientValidatorTest {

    @Test
    public void isValidName_lengthBoundaries_checkedStrictly() {
        assertTrue(ClientValidator.isValidName("Ana"));
        assertTrue(ClientValidator.isValidName("Lu"));
        assertFalse(ClientValidator.isValidName("A"));
        assertFalse(ClientValidator.isValidName("  "));
        assertFalse(ClientValidator.isValidName(null));
        assertFalse(ClientValidator.isValidName(new String(new char[81]).replace('\0', 'a')));
    }

    @Test
    public void isValidBirthday_calendarAndOptionalYear_checkedStrictly() {
        assertTrue(ClientValidator.isValidBirthday(null, null, null, 2026));
        assertTrue(ClientValidator.isValidBirthday(29, 2, null, 2026));
        assertTrue(ClientValidator.isValidBirthday(29, 2, 2024, 2026));
        assertTrue(ClientValidator.isValidBirthday(15, 8, 1995, 2026));

        assertFalse(ClientValidator.isValidBirthday(30, 2, null, 2026));
        assertFalse(ClientValidator.isValidBirthday(29, 2, 2025, 2026));
        assertFalse(ClientValidator.isValidBirthday(31, 4, null, 2026));
        assertFalse(ClientValidator.isValidBirthday(10, null, null, 2026));
        assertFalse(ClientValidator.isValidBirthday(null, 5, 1990, 2026));
        assertFalse(ClientValidator.isValidBirthday(10, 5, 1899, 2026));
        assertFalse(ClientValidator.isValidBirthday(10, 5, 2027, 2026));
    }
}
