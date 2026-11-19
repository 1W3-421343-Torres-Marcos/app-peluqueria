package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reporte inmutable de ocupación horaria, por día de semana y franjas de menor demanda.
 */
public final class OccupancyReport {

    public static final class OccupiedBucket {
        private final int dayOfWeek;
        private final int slotHour;
        private final long occupiedMinutes;
        private final int appointmentCount;

        public OccupiedBucket(
                int dayOfWeek,
                int slotHour,
                long occupiedMinutes,
                int appointmentCount
        ) {
            this.dayOfWeek = dayOfWeek;
            this.slotHour = slotHour;
            this.occupiedMinutes = occupiedMinutes;
            this.appointmentCount = appointmentCount;
        }

        public int dayOfWeek() {
            return dayOfWeek;
        }

        public int slotHour() {
            return slotHour;
        }

        public long occupiedMinutes() {
            return occupiedMinutes;
        }

        public int appointmentCount() {
            return appointmentCount;
        }
    }

    public static final class HourSlotRow {
        private final int slotHour;
        private final long occupiedMinutes;
        private final long workingMinutes;
        private final int occupancyBasisPoints;
        private final int appointmentCount;

        public HourSlotRow(
                int slotHour,
                long occupiedMinutes,
                long workingMinutes,
                int occupancyBasisPoints,
                int appointmentCount
        ) {
            this.slotHour = slotHour;
            this.occupiedMinutes = occupiedMinutes;
            this.workingMinutes = workingMinutes;
            this.occupancyBasisPoints = occupancyBasisPoints;
            this.appointmentCount = appointmentCount;
        }

        public int slotHour() {
            return slotHour;
        }

        public long occupiedMinutes() {
            return occupiedMinutes;
        }

        public long workingMinutes() {
            return workingMinutes;
        }

        public int occupancyBasisPoints() {
            return occupancyBasisPoints;
        }

        public int appointmentCount() {
            return appointmentCount;
        }
    }

    public static final class DayOfWeekRow {
        private final int dayOfWeek;
        private final long occupiedMinutes;
        private final long workingMinutes;
        private final int occupancyBasisPoints;
        private final int appointmentCount;

        public DayOfWeekRow(
                int dayOfWeek,
                long occupiedMinutes,
                long workingMinutes,
                int occupancyBasisPoints,
                int appointmentCount
        ) {
            this.dayOfWeek = dayOfWeek;
            this.occupiedMinutes = occupiedMinutes;
            this.workingMinutes = workingMinutes;
            this.occupancyBasisPoints = occupancyBasisPoints;
            this.appointmentCount = appointmentCount;
        }

        public int dayOfWeek() {
            return dayOfWeek;
        }

        public long occupiedMinutes() {
            return occupiedMinutes;
        }

        public long workingMinutes() {
            return workingMinutes;
        }

        public int occupancyBasisPoints() {
            return occupancyBasisPoints;
        }

        public int appointmentCount() {
            return appointmentCount;
        }
    }

    public static final class WindowRow {
        private final int dayOfWeek;
        private final int slotHour;
        private final long occupiedMinutes;
        private final long workingMinutes;
        private final int occupancyBasisPoints;
        private final int appointmentCount;

        public WindowRow(
                int dayOfWeek,
                int slotHour,
                long occupiedMinutes,
                long workingMinutes,
                int occupancyBasisPoints,
                int appointmentCount
        ) {
            this.dayOfWeek = dayOfWeek;
            this.slotHour = slotHour;
            this.occupiedMinutes = occupiedMinutes;
            this.workingMinutes = workingMinutes;
            this.occupancyBasisPoints = occupancyBasisPoints;
            this.appointmentCount = appointmentCount;
        }

        public int dayOfWeek() {
            return dayOfWeek;
        }

        public int slotHour() {
            return slotHour;
        }

        public long occupiedMinutes() {
            return occupiedMinutes;
        }

        public long workingMinutes() {
            return workingMinutes;
        }

        public int occupancyBasisPoints() {
            return occupancyBasisPoints;
        }

        public int appointmentCount() {
            return appointmentCount;
        }
    }

    private final long totalOccupiedMinutes;
    private final long totalWorkingMinutes;
    private final int overallOccupancyBasisPoints;
    private final List<HourSlotRow> byHourSlot;
    private final List<DayOfWeekRow> byDayOfWeek;
    private final List<WindowRow> lowestDemandWindows;

    public OccupancyReport(
            long totalOccupiedMinutes,
            long totalWorkingMinutes,
            int overallOccupancyBasisPoints,
            List<HourSlotRow> byHourSlot,
            List<DayOfWeekRow> byDayOfWeek,
            List<WindowRow> lowestDemandWindows
    ) {
        this.totalOccupiedMinutes = totalOccupiedMinutes;
        this.totalWorkingMinutes = totalWorkingMinutes;
        this.overallOccupancyBasisPoints = overallOccupancyBasisPoints;
        this.byHourSlot = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byHourSlot, "byHourSlot")));
        this.byDayOfWeek = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byDayOfWeek, "byDayOfWeek")));
        this.lowestDemandWindows = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(lowestDemandWindows, "lowestDemandWindows")));
    }

    public long totalOccupiedMinutes() {
        return totalOccupiedMinutes;
    }

    public long totalWorkingMinutes() {
        return totalWorkingMinutes;
    }

    public int overallOccupancyBasisPoints() {
        return overallOccupancyBasisPoints;
    }

    public List<HourSlotRow> byHourSlot() {
        return byHourSlot;
    }

    public List<DayOfWeekRow> byDayOfWeek() {
        return byDayOfWeek;
    }

    public List<WindowRow> lowestDemandWindows() {
        return lowestDemandWindows;
    }
}
