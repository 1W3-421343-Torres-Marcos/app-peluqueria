package com.metanoia.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Money;

import org.junit.Test;

import java.time.LocalDate;

public class FormatsTest {

    @Test
    public void formatMoney_esAr_formatsThousandsDotAndDecimalComma() {
        assertEquals("$ 12.345,50", Formats.formatMoney(1_234_550L));
        assertEquals("$ 12.345,50", Formats.formatMoney(Money.ofCents(1_234_550L)));
        assertEquals("$ 0,00", Formats.formatMoney(0L));
        assertEquals("$ 0,05", Formats.formatMoney(5L));
        assertEquals("- $ 12.345,50", Formats.formatMoney(-1_234_550L));
    }

    @Test
    public void formatAndParseDate_ddMmYyyy_roundTripsStrictly() {
        long epochDay = LocalDate.of(2026, 3, 5).toEpochDay();

        assertEquals("05/03/2026", Formats.formatDate(epochDay));
        assertEquals(epochDay, Formats.parseDate("05/03/2026").orElseThrow().longValue());
        assertFalse(Formats.parseDate("31/02/2026").isPresent());
        assertFalse(Formats.parseDate("2026-03-05").isPresent());
        assertFalse(Formats.parseDate(null).isPresent());
    }

    @Test
    public void formatAndParseTime_24Hours_roundTripsStrictly() {
        assertEquals("00:00", Formats.formatTime(0));
        assertEquals("09:05", Formats.formatTime(9 * 60 + 5));
        assertEquals("14:30", Formats.formatTime(14 * 60 + 30));
        assertEquals("23:59", Formats.formatTime(23 * 60 + 59));

        assertEquals(Integer.valueOf(14 * 60 + 30), Formats.parseTime("14:30").orElseThrow());
        assertFalse(Formats.parseTime("24:00").isPresent());
        assertFalse(Formats.parseTime("2:30 PM").isPresent());
        assertTrue(Formats.parseTime("00:00").isPresent());

        assertThrows(IllegalArgumentException.class, () -> Formats.formatTime(-1));
        assertThrows(IllegalArgumentException.class, () -> Formats.formatTime(1440));
    }
}
