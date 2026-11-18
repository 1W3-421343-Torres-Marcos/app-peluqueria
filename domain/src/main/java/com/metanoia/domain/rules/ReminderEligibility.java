package com.metanoia.domain.rules;

import com.metanoia.domain.model.AppointmentStatus;

/**
 * Reglas puras de elegibilidad para recordatorios de mañana y saludos de cumpleaños.
 */
public final class ReminderEligibility {

    private ReminderEligibility() {
    }

    public static boolean isEligibleForTomorrow(
            AppointmentStatus status,
            long appointmentDayEpoch,
            long todayEpochDay,
            boolean alreadyReminded
    ) {
        if (alreadyReminded || status == null) {
            return false;
        }
        if (appointmentDayEpoch != todayEpochDay + 1L) {
            return false;
        }
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }

    public static boolean isEligibleForBirthdayToday(
            boolean clientActive,
            Integer birthDay,
            Integer birthMonth,
            int todayDayOfMonth,
            int todayMonth,
            boolean alreadyGreetedThisYear
    ) {
        if (!clientActive || alreadyGreetedThisYear || birthDay == null || birthMonth == null) {
            return false;
        }
        return birthDay == todayDayOfMonth && birthMonth == todayMonth;
    }
}
