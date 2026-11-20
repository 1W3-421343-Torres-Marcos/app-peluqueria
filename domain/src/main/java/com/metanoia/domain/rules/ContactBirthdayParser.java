package com.metanoia.domain.rules;

import com.metanoia.domain.model.ContactBirthday;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsea fechas de cumpleaños provenientes de {@code ContactsContract} aceptando formato con año y {@code --MM-DD}.
 */
public final class ContactBirthdayParser {

    private static final Pattern YEAR_MONTH_DAY = Pattern.compile("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})$");
    private static final Pattern NO_YEAR_DASHED = Pattern.compile("^--(\\d{2})-(\\d{2})$");
    private static final Pattern NO_YEAR_COMPACT = Pattern.compile("^--(\\d{2})(\\d{2})$");
    private static final Pattern COMPACT_FULL = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})$");
    private static final Pattern DAY_MONTH_YEAR = Pattern.compile("^(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})$");

    private ContactBirthdayParser() {
    }

    public static Optional<ContactBirthday> parse(String rawBirthday, int maxBirthYear) {
        if (rawBirthday == null) {
            return Optional.empty();
        }
        String trimmed = stripTimeSuffix(rawBirthday.trim());
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        Matcher noYearDash = NO_YEAR_DASHED.matcher(trimmed);
        if (noYearDash.matches()) {
            return validateAndBuild(
                    parseInt(noYearDash.group(2)), parseInt(noYearDash.group(1)), null, maxBirthYear);
        }

        Matcher noYearCompact = NO_YEAR_COMPACT.matcher(trimmed);
        if (noYearCompact.matches()) {
            return validateAndBuild(
                    parseInt(noYearCompact.group(2)), parseInt(noYearCompact.group(1)), null, maxBirthYear);
        }

        Matcher ymd = YEAR_MONTH_DAY.matcher(trimmed);
        if (ymd.matches()) {
            return buildWithYear(
                    parseInt(ymd.group(3)), parseInt(ymd.group(2)), parseInt(ymd.group(1)), maxBirthYear);
        }

        Matcher compact = COMPACT_FULL.matcher(trimmed);
        if (compact.matches()) {
            return buildWithYear(
                    parseInt(compact.group(3)), parseInt(compact.group(2)), parseInt(compact.group(1)), maxBirthYear);
        }

        Matcher dmy = DAY_MONTH_YEAR.matcher(trimmed);
        if (dmy.matches()) {
            return buildWithYear(
                    parseInt(dmy.group(1)), parseInt(dmy.group(2)), parseInt(dmy.group(3)), maxBirthYear);
        }

        return Optional.empty();
    }

    private static String stripTimeSuffix(String raw) {
        int tIdx = raw.indexOf('T');
        if (tIdx > 0) {
            return raw.substring(0, tIdx).trim();
        }
        int spaceIdx = raw.indexOf(' ');
        if (spaceIdx > 0) {
            return raw.substring(0, spaceIdx).trim();
        }
        return raw;
    }

    private static Optional<ContactBirthday> buildWithYear(int day, int month, int rawYear, int maxBirthYear) {
        Integer year = rawYear == 0 ? null : rawYear;
        return validateAndBuild(day, month, year, maxBirthYear);
    }

    private static Optional<ContactBirthday> validateAndBuild(
            int day,
            int month,
            Integer year,
            int maxBirthYear
    ) {
        if (!ClientValidator.isValidBirthday(day, month, year, maxBirthYear)) {
            return Optional.empty();
        }
        return Optional.of(new ContactBirthday(day, month, year));
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
