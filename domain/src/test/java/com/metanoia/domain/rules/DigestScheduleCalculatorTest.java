package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;

import com.metanoia.domain.common.Clock;

import org.junit.Test;

import java.time.ZonedDateTime;

public class DigestScheduleCalculatorTest {

    @Test
    public void sameDayFutureHour_schedulesLaterToday() {
        long nowMillis = ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, Clock.ARGENTINA_ZONE)
                .toInstant().toEpochMilli();

        long delay = DigestScheduleCalculator.initialDelayMillis(
                nowMillis, Clock.ARGENTINA_ZONE, 18, 0);

        assertEquals(8L * 3_600_000L, delay);
    }

    @Test
    public void exactOrPastHour_rollsOverToNextDay() {
        long exactMillis = ZonedDateTime.of(2026, 11, 18, 18, 0, 0, 0, Clock.ARGENTINA_ZONE)
                .toInstant().toEpochMilli();
        assertEquals(24L * 3_600_000L,
                DigestScheduleCalculator.initialDelayMillis(
                        exactMillis, Clock.ARGENTINA_ZONE, 18, 0));

        long pastMillis = ZonedDateTime.of(2026, 11, 18, 20, 30, 0, 0, Clock.ARGENTINA_ZONE)
                .toInstant().toEpochMilli();
        assertEquals(21L * 3_600_000L + 30L * 60_000L,
                DigestScheduleCalculator.initialDelayMillis(
                        pastMillis, Clock.ARGENTINA_ZONE, 18, 0));
    }
}
