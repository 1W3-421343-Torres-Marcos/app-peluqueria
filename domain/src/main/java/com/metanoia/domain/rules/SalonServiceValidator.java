package com.metanoia.domain.rules;

import com.metanoia.domain.model.Money;

/**
 * Validaciones puras de servicios del catálogo.
 */
public final class SalonServiceValidator {

    private static final int MIN_NAME_LEN = 2;
    private static final int MAX_NAME_LEN = 80;
    private static final int MIN_CATEGORY_LEN = 2;
    private static final int MAX_CATEGORY_LEN = 40;

    private SalonServiceValidator() {
    }

    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= MIN_NAME_LEN && trimmed.length() <= MAX_NAME_LEN;
    }

    public static boolean isValidCategory(String category) {
        if (category == null) {
            return false;
        }
        String trimmed = category.trim();
        return trimmed.length() >= MIN_CATEGORY_LEN && trimmed.length() <= MAX_CATEGORY_LEN;
    }

    public static boolean isValidPrice(Money price) {
        return price != null && price.cents() >= 0L;
    }

    public static boolean isValidDuration(int durationMin, int slotMinutes) {
        return slotMinutes > 0 && durationMin > 0 && durationMin % slotMinutes == 0;
    }
}
