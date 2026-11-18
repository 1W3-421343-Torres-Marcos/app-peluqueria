package com.metanoia.domain.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Unidades válidas para las cantidades de componentes de una fórmula de coloración.
 */
public enum ColorComponentUnit {
    GRAMS("g"),
    MILLILITERS("ml"),
    PARTS("partes");

    private final String label;

    ColorComponentUnit(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ColorComponentUnit> fromString(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        for (ColorComponentUnit unit : values()) {
            if (unit.name().toLowerCase(Locale.ROOT).equals(normalized)
                    || unit.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(unit);
            }
        }
        return Optional.empty();
    }
}
