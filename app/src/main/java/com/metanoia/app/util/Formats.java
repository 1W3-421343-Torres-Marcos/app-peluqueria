package com.metanoia.app.util;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.model.Money;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Formateadores centralizados para Argentina (es-AR, ARS, dd/MM/yyyy, 24 hs).
 */
public final class Formats {

    public static final Locale ES_AR = Locale.forLanguageTag("es-AR");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/uuuu", ES_AR)
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm", ES_AR)
            .withResolverStyle(ResolverStyle.STRICT);

    private static final ThreadLocal<DecimalFormat> MONEY_NUMBER_FORMAT =
            ThreadLocal.withInitial(() -> {
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(ES_AR);
                symbols.setGroupingSeparator('.');
                symbols.setDecimalSeparator(',');
                return new DecimalFormat("#,##0.00", symbols);
            });

    private Formats() {
    }

    public static String formatMoney(Money money) {
        Objects.requireNonNull(money, "money");
        return formatMoney(money.cents());
    }

    public static String formatMoney(long cents) {
        BigDecimal amount = BigDecimal.valueOf(cents).abs().movePointLeft(2);
        String formatted = MONEY_NUMBER_FORMAT.get().format(amount);
        return cents < 0L ? "- $ " + formatted : "$ " + formatted;
    }

    public static String formatPercent(int basisPoints) {
        BigDecimal percent = BigDecimal.valueOf(basisPoints).movePointLeft(2);
        return MONEY_NUMBER_FORMAT.get().format(percent) + " %";
    }

    public static String formatDate(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DATE_FORMATTER);
    }

    public static Optional<Long> parseDate(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(trimmed, DATE_FORMATTER).toEpochDay());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    public static String formatTime(int minutesFromMidnight) {
        if (minutesFromMidnight < 0 || minutesFromMidnight >= 1440) {
            throw new IllegalArgumentException(
                    "minutesFromMidnight fuera de rango [0, 1439]: " + minutesFromMidnight);
        }
        int hours = minutesFromMidnight / 60;
        int minutes = minutesFromMidnight % 60;
        return LocalTime.of(hours, minutes).format(TIME_FORMATTER);
    }

    public static Optional<Integer> parseTime(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            LocalTime time = LocalTime.parse(trimmed, TIME_FORMATTER);
            return Optional.of(time.getHour() * 60 + time.getMinute());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    public static String formatDateTime(long epochMillis) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis), Clock.ARGENTINA_ZONE);
        return zdt.toLocalDate().format(DATE_FORMATTER) + " "
                + zdt.toLocalTime().format(TIME_FORMATTER);
    }
}
