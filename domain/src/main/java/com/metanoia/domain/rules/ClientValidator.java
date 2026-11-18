package com.metanoia.domain.rules;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Validaciones de negocio para alta y edición de clientes.
 */
public final class ClientValidator {

    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 80;
    public static final int MAX_NOTES_LENGTH = 500;
    public static final int MIN_BIRTH_YEAR = 1900;

    private ClientValidator() {
    }

    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= MIN_NAME_LENGTH && trimmed.length() <= MAX_NAME_LENGTH;
    }

    public static boolean isValidPhone(String normalizedPhone) {
        return PhoneNormalizer.isValid(normalizedPhone);
    }

    public static boolean isValidBirthday(
            Integer day,
            Integer month,
            Integer year,
            int maxYear
    ) {
        if (day == null && month == null && year == null) {
            return true;
        }
        if (day == null || month == null) {
            return false;
        }
        try {
            if (year == null) {
                MonthDay.of(month, day);
                return true;
            }
            if (year < MIN_BIRTH_YEAR || year > maxYear) {
                return false;
            }
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }

    public static boolean isValidNotes(String notes) {
        return notes == null || notes.trim().length() <= MAX_NOTES_LENGTH;
    }

    public static boolean isValidNoShowCount(int noShowCount) {
        return noShowCount >= 0;
    }
}
