package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Regla de repetición periódica cada {@code intervalWeeks} semanas hasta fecha o cantidad límite.
 */
public final class RecurrenceRule {

    public static final int MAX_OCCURRENCES_LIMIT = 52;

    private final String id;
    private final int intervalWeeks;
    private final Long untilDay;
    private final Integer maxOccurrences;
    private final long createdAt;

    public RecurrenceRule(
            String id,
            int intervalWeeks,
            Long untilDay,
            Integer maxOccurrences,
            long createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        if (intervalWeeks < 1 || intervalWeeks > 12) {
            throw new IllegalArgumentException(
                    "intervalWeeks debe estar entre 1 y 12: " + intervalWeeks);
        }
        if (untilDay == null && maxOccurrences == null) {
            throw new IllegalArgumentException(
                    "Debe definirse untilDay o maxOccurrences para la recurrencia");
        }
        if (maxOccurrences != null
                && (maxOccurrences < 1 || maxOccurrences > MAX_OCCURRENCES_LIMIT)) {
            throw new IllegalArgumentException(
                    "maxOccurrences fuera de rango [1, 52]: " + maxOccurrences);
        }
        this.intervalWeeks = intervalWeeks;
        this.untilDay = untilDay;
        this.maxOccurrences = maxOccurrences;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public int intervalWeeks() {
        return intervalWeeks;
    }

    public Optional<Long> untilDay() {
        return Optional.ofNullable(untilDay);
    }

    public Optional<Integer> maxOccurrences() {
        return Optional.ofNullable(maxOccurrences);
    }

    public long createdAt() {
        return createdAt;
    }
}
