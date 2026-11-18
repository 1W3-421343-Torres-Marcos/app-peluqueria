package com.metanoia.domain.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.TimeRange;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScheduleValidatorTest {

    @Test
    public void isValidSlotMinutes_allowsOnlyConfiguredIncrements() {
        assertTrue(ScheduleValidator.isValidSlotMinutes(10));
        assertTrue(ScheduleValidator.isValidSlotMinutes(15));
        assertTrue(ScheduleValidator.isValidSlotMinutes(20));
        assertTrue(ScheduleValidator.isValidSlotMinutes(30));

        assertFalse(ScheduleValidator.isValidSlotMinutes(0));
        assertFalse(ScheduleValidator.isValidSlotMinutes(5));
        assertFalse(ScheduleValidator.isValidSlotMinutes(45));
        assertFalse(ScheduleValidator.isValidSlotMinutes(60));
    }

    @Test
    public void validateDayRanges_adjacentRanges_areAllowed() {
        List<TimeRange> ranges = Arrays.asList(
                new TimeRange(1, 540, 780),
                new TimeRange(1, 780, 1200),
                new TimeRange(2, 540, 1200)
        );
        assertTrue(ScheduleValidator.validateDayRanges(ranges));
    }

    @Test
    public void validateDayRanges_overlappingSameDay_returnsFalse() {
        List<TimeRange> ranges = Arrays.asList(
                new TimeRange(1, 540, 800),
                new TimeRange(1, 780, 1200)
        );
        assertFalse(ScheduleValidator.validateDayRanges(ranges));
    }

    @Test
    public void isWithinSchedule_checksContainmentPerDay() {
        List<TimeRange> salon = Arrays.asList(
                new TimeRange(1, 540, 780),
                new TimeRange(1, 780, 1200),
                new TimeRange(2, 600, 1140)
        );

        assertTrue(ScheduleValidator.isWithinSchedule(
                Collections.singletonList(new TimeRange(1, 540, 1200)), salon));
        assertTrue(ScheduleValidator.isWithinSchedule(
                Collections.singletonList(new TimeRange(2, 660, 1080)), salon));

        assertFalse(ScheduleValidator.isWithinSchedule(
                Collections.singletonList(new TimeRange(2, 540, 1080)), salon));
        assertFalse(ScheduleValidator.isWithinSchedule(
                Collections.singletonList(new TimeRange(7, 600, 900)), salon));
    }
}
