package com.metanoia.domain.rules;

import java.util.Locale;

/**
 * Normaliza teléfonos argentinos al formato E.164 sin {@code +} (ej. {@code 5491122334455})
 * apto para enlaces {@code wa.me} y detección de duplicados.
 */
public final class PhoneNormalizer {

    private static final int MIN_E164_DIGITS = 10;
    private static final int MAX_E164_DIGITS = 15;
    private static final int AR_NATIONAL_DIGITS = 10;
    private static final int AR_MOBILE_WITH_15_DIGITS = 12;

    private PhoneNormalizer() {
    }

    /**
     * Normaliza un teléfono libre. Los números locales argentinos de 10 dígitos (o con {@code 0}
     * de larga distancia y/o prefijo móvil {@code 15}) quedan como {@code 549 + 10 dígitos}.
     */
    public static String normalize(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String trimmed = rawPhone.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String digits = extractDigits(trimmed);
        if (digits.isEmpty()) {
            return "";
        }

        boolean hadPlus = trimmed.startsWith("+");
        boolean hadDoubleZero = digits.startsWith("00");
        if (hadDoubleZero) {
            digits = digits.substring(2);
        }
        if (digits.isEmpty()) {
            return "";
        }

        if (digits.startsWith("54")) {
            return normalizeWithArgentinaPrefix(digits);
        }

        if (hadPlus || hadDoubleZero) {
            return digits;
        }

        String national = normalizeArgentinaNational(digits);
        if (national.length() == AR_NATIONAL_DIGITS) {
            return "549" + national;
        }
        return digits;
    }

    /**
     * Indica si un teléfono ya normalizado tiene cantidad válida de dígitos E.164.
     */
    public static boolean isValid(String normalizedPhone) {
        if (normalizedPhone == null) {
            return false;
        }
        int len = normalizedPhone.length();
        if (len < MIN_E164_DIGITS || len > MAX_E164_DIGITS) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char c = normalizedPhone.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Extrae únicamente los dígitos ASCII del texto recibido (útil para búsqueda parcial en UI).
     */
    public static String extractDigits(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Formatea un teléfono normalizado para visualización legible en pantalla.
     */
    public static String formatForDisplay(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.isEmpty()) {
            return "";
        }
        if (normalizedPhone.startsWith("549") && normalizedPhone.length() == 13) {
            String national = normalizedPhone.substring(3);
            if (national.startsWith("11")) {
                return String.format(Locale.ROOT, "+54 9 11 %s-%s",
                        national.substring(2, 6), national.substring(6));
            }
            return String.format(Locale.ROOT, "+54 9 %s %s",
                    national.substring(0, 3), national.substring(3));
        }
        return "+" + normalizedPhone;
    }

    private static String normalizeWithArgentinaPrefix(String digitsWith54) {
        String after54 = digitsWith54.substring(2);
        if (after54.startsWith("9")) {
            String after9 = after54.substring(1);
            String national = normalizeArgentinaNational(after9);
            if (national.length() == AR_NATIONAL_DIGITS) {
                return "549" + national;
            }
            return "549" + after9;
        }
        String national = normalizeArgentinaNational(after54);
        if (national.length() == AR_NATIONAL_DIGITS) {
            return "549" + national;
        }
        return digitsWith54;
    }

    private static String normalizeArgentinaNational(String rawNational) {
        String s = rawNational;
        while (s.startsWith("0")) {
            s = s.substring(1);
        }
        if (s.length() == AR_MOBILE_WITH_15_DIGITS) {
            if (s.startsWith("1115")) {
                return "11" + s.substring(4);
            }
            if (!s.startsWith("11")) {
                if (s.startsWith("15", 3)) {
                    return s.substring(0, 3) + s.substring(5);
                }
                if (s.startsWith("15", 4)) {
                    return s.substring(0, 4) + s.substring(6);
                }
            }
        }
        return s;
    }
}
