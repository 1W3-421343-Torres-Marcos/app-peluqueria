package com.metanoia.domain.model;

import java.util.Objects;

/**
 * Tramo horario semanal inmutable para un día de la semana (1 = lunes a 7 = domingo).
 * Los intervalos son semiabiertos {@code [startMin, endMin)} expresados en minutos desde 00:00.
 */
public final class TimeRange implements Comparable<TimeRange> {

    private final int dayOfWeek;
    private final int startMin;
    private final int endMin;

    public TimeRange(int dayOfWeek, int startMin, int endMin) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek fuera de rango [1, 7]: " + dayOfWeek);
        }
        if (startMin < 0 || endMin > 1440 || startMin >= endMin) {
            throw new IllegalArgumentException(
                    "Tramo inválido [" + startMin + ", " + endMin + ")");
        }
        this.dayOfWeek = dayOfWeek;
        this.startMin = startMin;
        this.endMin = endMin;
    }

    public int dayOfWeek() {
        return dayOfWeek;
    }

    public int startMin() {
        return startMin;
    }

    public int endMin() {
        return endMin;
    }

    public int durationMinutes() {
        return endMin - startMin;
    }

    public boolean overlaps(TimeRange other) {
        Objects.requireNonNull(other, "other");
        return this.dayOfWeek == other.dayOfWeek
                && this.startMin < other.endMin
                && other.startMin < this.endMin;
    }

    public boolean contains(TimeRange other) {
        Objects.requireNonNull(other, "other");
        return this.dayOfWeek == other.dayOfWeek
                && this.startMin <= other.startMin
                && other.endMin <= this.endMin;
    }

    @Override
    public int compareTo(TimeRange other) {
        int dayCmp = Integer.compare(this.dayOfWeek, other.dayOfWeek);
        if (dayCmp != 0) {
            return dayCmp;
        }
        int startCmp = Integer.compare(this.startMin, other.startMin);
        if (startCmp != 0) {
            return startCmp;
        }
        return Integer.compare(this.endMin, other.endMin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeRange)) {
            return false;
        }
        TimeRange timeRange = (TimeRange) o;
        return dayOfWeek == timeRange.dayOfWeek
                && startMin == timeRange.startMin
                && endMin == timeRange.endMin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, startMin, endMin);
    }
}
