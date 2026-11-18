package com.metanoia.domain.rules;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza cadenas para búsquedas e índices sin distinción de mayúsculas ni acentos.
 */
public final class TextNormalizer {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String collapsed = MULTI_WHITESPACE.matcher(input.trim()).replaceAll(" ");
        if (collapsed.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD);
        String stripped = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return stripped.toLowerCase(Locale.ROOT);
    }
}
