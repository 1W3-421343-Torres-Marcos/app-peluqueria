package com.metanoia.domain.rules;

/**
 * Ajusta nombres de contactos del dispositivo al rango 2–80 caracteres o usa el teléfono si falta nombre.
 */
public final class ContactClientNameResolver {

    private ContactClientNameResolver() {
    }

    public static String resolve(String rawDisplayName, String normalizedPhone) {
        String clean = sanitize(rawDisplayName);
        if (clean.length() < ClientValidator.MIN_NAME_LENGTH) {
            return normalizedPhone;
        }
        if (clean.length() > ClientValidator.MAX_NAME_LENGTH) {
            String cut = clean.substring(0, ClientValidator.MAX_NAME_LENGTH).trim();
            return cut.length() >= ClientValidator.MIN_NAME_LENGTH ? cut : normalizedPhone;
        }
        return clean;
    }

    public static boolean hasRealDisplayName(String rawDisplayName) {
        return sanitize(rawDisplayName).length() >= ClientValidator.MIN_NAME_LENGTH;
    }

    private static String sanitize(String rawDisplayName) {
        if (rawDisplayName == null) {
            return "";
        }
        return rawDisplayName.trim().replaceAll("\\s+", " ");
    }
}
