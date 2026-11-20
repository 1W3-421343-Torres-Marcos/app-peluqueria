package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.ContactBirthday;

import org.junit.Test;

import java.util.Optional;

public class ContactBirthdayParserTest {

    @Test
    public void parse_fullDatesAndIsoTimestamp_returnDayMonthAndYear() {
        Optional<ContactBirthday> iso = ContactBirthdayParser.parse("1994-07-19", 2026);
        assertTrue(iso.isPresent());
        assertEquals(19, iso.get().day());
        assertEquals(7, iso.get().month());
        assertEquals(Integer.valueOf(1994), iso.get().year().orElse(null));

        Optional<ContactBirthday> zoned = ContactBirthdayParser.parse("1989-12-03T00:00:00.000Z", 2026);
        assertTrue(zoned.isPresent());
        assertEquals(3, zoned.get().day());
        assertEquals(12, zoned.get().month());
        assertEquals(Integer.valueOf(1989), zoned.get().year().orElse(null));

        Optional<ContactBirthday> local = ContactBirthdayParser.parse("25/05/1990", 2026);
        assertTrue(local.isPresent());
        assertEquals(25, local.get().day());
        assertEquals(5, local.get().month());
        assertEquals(Integer.valueOf(1990), local.get().year().orElse(null));
    }

    @Test
    public void parse_withoutYearFormats_returnDayMonthWithEmptyYear() {
        Optional<ContactBirthday> dashed = ContactBirthdayParser.parse("--08-21", 2026);
        assertTrue(dashed.isPresent());
        assertEquals(21, dashed.get().day());
        assertEquals(8, dashed.get().month());
        assertFalse(dashed.get().year().isPresent());

        Optional<ContactBirthday> zeroYear = ContactBirthdayParser.parse("0000-02-29", 2026);
        assertTrue(zeroYear.isPresent());
        assertEquals(29, zeroYear.get().day());
        assertEquals(2, zeroYear.get().month());
        assertFalse(zeroYear.get().year().isPresent());

        Optional<ContactBirthday> compactNoYear = ContactBirthdayParser.parse("--1109", 2026);
        assertTrue(compactNoYear.isPresent());
        assertEquals(9, compactNoYear.get().day());
        assertEquals(11, compactNoYear.get().month());
        assertFalse(compactNoYear.get().year().isPresent());
    }

    @Test
    public void parse_invalidDates_returnEmpty() {
        assertFalse(ContactBirthdayParser.parse("2025-02-29", 2026).isPresent());
        assertFalse(ContactBirthdayParser.parse("--04-31", 2026).isPresent());
        assertFalse(ContactBirthdayParser.parse("1850-01-01", 2026).isPresent());
        assertFalse(ContactBirthdayParser.parse("sin fecha", 2026).isPresent());
        assertFalse(ContactBirthdayParser.parse(null, 2026).isPresent());
    }
}
