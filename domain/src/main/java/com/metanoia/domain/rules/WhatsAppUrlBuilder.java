package com.metanoia.domain.rules;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Construye y codifica enlaces {@code https://wa.me/<tel>?text=...} para abrir WhatsApp
 * sin usar APIs oficiales ni requerir permiso {@code INTERNET}.
 */
public final class WhatsAppUrlBuilder {

    private static final int MIN_DIGITS = 6;

    private WhatsAppUrlBuilder() {
    }

    public static boolean hasValidPhone(String rawPhone) {
        return extractDigits(rawPhone).length() >= MIN_DIGITS;
    }

    public static Optional<String> buildUrl(String rawPhone, String message) {
        String digits = extractDigits(rawPhone);
        if (digits.length() < MIN_DIGITS) {
            return Optional.empty();
        }
        String text = message == null ? "" : message;
        String encoded = encodeQueryValue(text);
        return Optional.of("https://wa.me/" + digits + "?text=" + encoded);
    }

    public static String extractDigits(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(rawPhone.length());
        for (int i = 0; i < rawPhone.length(); i++) {
            char c = rawPhone.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 no disponible", ex);
        }
    }
}
