package com.metanoia.app.data.db.dao;

import com.metanoia.domain.rules.PhoneNormalizer;
import com.metanoia.domain.rules.TextNormalizer;

/**
 * Parametriza una búsqueda SQL sobre clientes (prefijo de palabra en nombre normalizado o teléfono).
 */
public final class ClientSearchFilter {

    public final String query;
    public final String prefixPattern;
    public final String wordPrefixPattern;
    public final String phoneContainsPattern;
    public final boolean includeInactive;

    private ClientSearchFilter(
            String query,
            String prefixPattern,
            String wordPrefixPattern,
            String phoneContainsPattern,
            boolean includeInactive
    ) {
        this.query = query;
        this.prefixPattern = prefixPattern;
        this.wordPrefixPattern = wordPrefixPattern;
        this.phoneContainsPattern = phoneContainsPattern;
        this.includeInactive = includeInactive;
    }

    public static ClientSearchFilter fromRawQuery(String rawQuery, boolean includeInactive) {
        String normalizedText = TextNormalizer.normalize(rawQuery);
        if (normalizedText.isEmpty()) {
            return new ClientSearchFilter("", "", "", "", includeInactive);
        }
        String escapedText = escapeLike(normalizedText);
        String prefix = escapedText + "%";
        String wordPrefix = "% " + escapedText + "%";

        String normalizedPhone = PhoneNormalizer.normalize(rawQuery);
        String digits = PhoneNormalizer.extractDigits(rawQuery);
        while (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        String phonePattern = "";
        if (PhoneNormalizer.isValid(normalizedPhone)) {
            phonePattern = "%" + escapeLike(normalizedPhone) + "%";
        } else if (!digits.isEmpty()) {
            phonePattern = "%" + escapeLike(digits) + "%";
        }
        return new ClientSearchFilter(
                normalizedText,
                prefix,
                wordPrefix,
                phonePattern,
                includeInactive
        );
    }

    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
