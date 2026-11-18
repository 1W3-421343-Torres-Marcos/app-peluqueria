package com.metanoia.domain.rules;

import com.metanoia.domain.model.TimeRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validaciones puras de horarios, tramos semanales y duración mínima de bloque.
 */
public final class ScheduleValidator {

    private static final int[] ALLOWED_SLOT_MINUTES = {10, 15, 20, 30};

    private ScheduleValidator() {
    }

    public static boolean isValidSlotMinutes(int slotMinutes) {
        for (int allowed : ALLOWED_SLOT_MINUTES) {
            if (allowed == slotMinutes) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidRange(int dayOfWeek, int startMin, int endMin) {
        return dayOfWeek >= 1 && dayOfWeek <= 7
                && startMin >= 0
                && endMin <= 1440
                && startMin < endMin;
    }

    /**
     * Verifica que los tramos de la lista sean válidos y no se solapen dentro del mismo día.
     * Se permiten tramos contiguos ({@code a.endMin() == b.startMin()}).
     */
    public static boolean validateDayRanges(List<TimeRange> ranges) {
        if (ranges == null) {
            return false;
        }
        List<TimeRange> sorted = new ArrayList<>(ranges);
        Collections.sort(sorted);
        for (int i = 0; i < sorted.size(); i++) {
            TimeRange current = sorted.get(i);
            if (current == null || !isValidRange(
                    current.dayOfWeek(), current.startMin(), current.endMin())) {
                return false;
            }
            if (i + 1 < sorted.size() && current.overlaps(sorted.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica que cada tramo candidato esté contenido dentro del horario padre
     * (uniendo tramos contiguos del padre en cada día).
     */
    public static boolean isWithinSchedule(
            List<TimeRange> candidate, List<TimeRange> parentSchedule) {
        if (candidate == null || parentSchedule == null) {
            return false;
        }
        if (candidate.isEmpty()) {
            return true;
        }
        List<TimeRange> mergedParent = mergeAdjacentByDay(parentSchedule);
        for (TimeRange range : candidate) {
            if (range == null) {
                return false;
            }
            boolean covered = false;
            for (TimeRange parent : mergedParent) {
                if (parent.contains(range)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    private static List<TimeRange> mergeAdjacentByDay(List<TimeRange> ranges) {
        List<TimeRange> sorted = new ArrayList<>(ranges);
        Collections.sort(sorted);
        List<TimeRange> merged = new ArrayList<>();
        for (TimeRange current : sorted) {
            if (merged.isEmpty()) {
                merged.add(current);
                continue;
            }
            TimeRange last = merged.get(merged.size() - 1);
            if (last.dayOfWeek() == current.dayOfWeek() && current.startMin() <= last.endMin()) {
                int newEnd = Math.max(last.endMin(), current.endMin());
                merged.set(merged.size() - 1,
                        new TimeRange(last.dayOfWeek(), last.startMin(), newEnd));
            } else {
                merged.add(current);
            }
        }
        return merged;
    }
}
