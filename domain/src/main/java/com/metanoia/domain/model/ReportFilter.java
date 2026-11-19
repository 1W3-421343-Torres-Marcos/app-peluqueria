package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Filtro inmutable de período (epochDay inclusivo) y profesional opcional para reportes.
 */
public final class ReportFilter {

    private final long fromDay;
    private final long toDay;
    private final String professionalId;

    public ReportFilter(long fromDay, long toDay, String professionalId) {
        if (fromDay > toDay) {
            throw new IllegalArgumentException(
                    "Rango de fechas inválido: " + fromDay + " > " + toDay);
        }
        this.fromDay = fromDay;
        this.toDay = toDay;
        if (professionalId == null || professionalId.trim().isEmpty()) {
            this.professionalId = null;
        } else {
            this.professionalId = professionalId.trim();
        }
    }

    public long fromDay() {
        return fromDay;
    }

    public long toDay() {
        return toDay;
    }

    public Optional<String> professionalId() {
        return Optional.ofNullable(professionalId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportFilter)) {
            return false;
        }
        ReportFilter that = (ReportFilter) o;
        return fromDay == that.fromDay
                && toDay == that.toDay
                && Objects.equals(professionalId, that.professionalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromDay, toDay, professionalId);
    }
}
