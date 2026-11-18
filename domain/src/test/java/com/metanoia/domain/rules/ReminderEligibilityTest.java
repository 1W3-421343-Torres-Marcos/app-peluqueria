package com.metanoia.domain.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.AppointmentStatus;

import org.junit.Test;

public class ReminderEligibilityTest {

    @Test
    public void tomorrowReminder_acceptsOnlyPendingOrConfirmedOnNextDayNotYetReminded() {
        long today = 20_500L;
        long tomorrow = 20_501L;

        assertTrue(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.PENDING, tomorrow, today, false));
        assertTrue(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.CONFIRMED, tomorrow, today, false));

        assertFalse(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.CONFIRMED, tomorrow, today, true));
        assertFalse(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.COMPLETED, tomorrow, today, false));
        assertFalse(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.CANCELLED, tomorrow, today, false));
        assertFalse(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.CONFIRMED, today, today, false));
        assertFalse(ReminderEligibility.isEligibleForTomorrow(
                AppointmentStatus.CONFIRMED, tomorrow + 1L, today, false));
    }

    @Test
    public void birthdayToday_acceptsOnlyActiveMatchingClientsNotGreetedThisYear() {
        assertTrue(ReminderEligibility.isEligibleForBirthdayToday(
                true, 18, 11, 18, 11, false));

        assertFalse(ReminderEligibility.isEligibleForBirthdayToday(
                true, 18, 11, 18, 11, true));
        assertFalse(ReminderEligibility.isEligibleForBirthdayToday(
                false, 18, 11, 18, 11, false));
        assertFalse(ReminderEligibility.isEligibleForBirthdayToday(
                true, null, 11, 18, 11, false));
        assertFalse(ReminderEligibility.isEligibleForBirthdayToday(
                true, 19, 11, 18, 11, false));
    }
}
