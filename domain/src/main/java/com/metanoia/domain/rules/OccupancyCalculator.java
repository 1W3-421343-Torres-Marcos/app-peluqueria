package com.metanoia.domain.rules;

import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.OccupancyReport.DayOfWeekRow;
import com.metanoia.domain.model.OccupancyReport.HourSlotRow;
import com.metanoia.domain.model.OccupancyReport.OccupiedBucket;
import com.metanoia.domain.model.OccupancyReport.WindowRow;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.TimeRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculadora pura de ocupación horaria: intersecta horario del profesional con horario del salón,
 * descuenta días libres y bloqueos sin doble resta y detecta franjas de menor demanda.
 */
public final class OccupancyCalculator {

    private static final int DAYS_PER_WEEK = 7;
    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_DAY = 1440;
    private static final int LOWEST_DEMAND_LIMIT = 5;
    private static final BigDecimal BASIS_POINTS_MAX = BigDecimal.valueOf(10_000L);

    private OccupancyCalculator() {
    }

    public static OccupancyReport calculate(
            long fromDay,
            long toDay,
            List<Professional> professionals,
            List<TimeRange> salonSchedule,
            List<ScheduleBlock> activeBlocks,
            List<OccupiedBucket> occupiedBuckets
    ) {
        if (fromDay > toDay) {
            throw new IllegalArgumentException(
                    "Rango de días inválido: " + fromDay + " > " + toDay);
        }

        long[] dowCounts = countWeekdaysInRange(fromDay, toDay);
        List<TimeRange> mergedSalon = salonSchedule == null
                ? Collections.emptyList()
                : ScheduleValidator.mergeAdjacentByDay(salonSchedule);

        long[][] working = new long[DAYS_PER_WEEK + 1][HOURS_PER_DAY];
        if (professionals != null) {
            for (Professional pro : professionals) {
                if (pro != null) {
                    accumulateProfessionalWorking(
                            pro, fromDay, toDay, dowCounts, mergedSalon, activeBlocks, working);
                }
            }
        }

        long[][] occupied = new long[DAYS_PER_WEEK + 1][HOURS_PER_DAY];
        int[][] appointments = new int[DAYS_PER_WEEK + 1][HOURS_PER_DAY];
        accumulateOccupiedBuckets(occupiedBuckets, occupied, appointments);

        return buildReport(working, occupied, appointments);
    }

    private static long[] countWeekdaysInRange(long fromDay, long toDay) {
        long totalDays = toDay - fromDay + 1L;
        long fullWeeks = totalDays / DAYS_PER_WEEK;
        int remainder = (int) (totalDays % DAYS_PER_WEEK);

        long[] counts = new long[DAYS_PER_WEEK + 1];
        for (int dow = 1; dow <= DAYS_PER_WEEK; dow++) {
            counts[dow] = fullWeeks;
        }
        int startDow = LocalDate.ofEpochDay(fromDay).getDayOfWeek().getValue();
        for (int i = 0; i < remainder; i++) {
            int dow = ((startDow - 1 + i) % DAYS_PER_WEEK) + 1;
            counts[dow]++;
        }
        return counts;
    }

    private static void accumulateProfessionalWorking(
            Professional pro,
            long fromDay,
            long toDay,
            long[] dowCounts,
            List<TimeRange> mergedSalon,
            List<ScheduleBlock> activeBlocks,
            long[][] working
    ) {
        List<List<int[]>> effectiveByDow = resolveEffectiveSchedule(pro.workSchedules(), mergedSalon);
        for (int dow = 1; dow <= DAYS_PER_WEEK; dow++) {
            long occ = dowCounts[dow];
            if (occ > 0L) {
                addDayIntervals(working[dow], effectiveByDow.get(dow), occ);
            }
        }

        Set<Long> daysOff = collectDaysOffInRange(pro.daysOff(), fromDay, toDay);
        for (long dayOff : daysOff) {
            int dow = LocalDate.ofEpochDay(dayOff).getDayOfWeek().getValue();
            addDayIntervals(working[dow], effectiveByDow.get(dow), -1L);
        }

        Map<Long, List<int[]>> blockedByDay = collectBlockedIntervals(
                pro.id(), fromDay, toDay, activeBlocks, daysOff);
        for (Map.Entry<Long, List<int[]>> entry : blockedByDay.entrySet()) {
            int dow = LocalDate.ofEpochDay(entry.getKey()).getDayOfWeek().getValue();
            List<int[]> effective = effectiveByDow.get(dow);
            if (!effective.isEmpty()) {
                List<int[]> mergedBlocks = mergeIntervals(entry.getValue());
                List<int[]> overlapped = intersectIntervalLists(effective, mergedBlocks);
                addDayIntervals(working[dow], overlapped, -1L);
            }
        }
    }

    private static List<List<int[]>> resolveEffectiveSchedule(
            List<TimeRange> rawProSchedule,
            List<TimeRange> mergedSalon
    ) {
        List<TimeRange> mergedPro = ScheduleValidator.mergeAdjacentByDay(rawProSchedule);
        List<List<int[]>> proByDow = groupByDow(mergedPro);
        if (mergedSalon.isEmpty()) {
            return proByDow;
        }
        List<List<int[]>> salonByDow = groupByDow(mergedSalon);
        List<List<int[]>> effective = new ArrayList<>(DAYS_PER_WEEK + 1);
        effective.add(Collections.emptyList());
        for (int dow = 1; dow <= DAYS_PER_WEEK; dow++) {
            effective.add(intersectIntervalLists(proByDow.get(dow), salonByDow.get(dow)));
        }
        return effective;
    }

    private static List<List<int[]>> groupByDow(List<TimeRange> ranges) {
        List<List<int[]>> byDow = new ArrayList<>(DAYS_PER_WEEK + 1);
        for (int dow = 0; dow <= DAYS_PER_WEEK; dow++) {
            byDow.add(new ArrayList<>());
        }
        for (TimeRange range : ranges) {
            byDow.get(range.dayOfWeek()).add(new int[]{range.startMin(), range.endMin()});
        }
        return byDow;
    }

    private static Set<Long> collectDaysOffInRange(Set<Long> daysOff, long fromDay, long toDay) {
        if (daysOff == null || daysOff.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> inRange = new HashSet<>();
        for (Long day : daysOff) {
            if (day != null && day >= fromDay && day <= toDay) {
                inRange.add(day);
            }
        }
        return inRange;
    }

    private static Map<Long, List<int[]>> collectBlockedIntervals(
            String professionalId,
            long fromDay,
            long toDay,
            List<ScheduleBlock> activeBlocks,
            Set<Long> daysOff
    ) {
        if (activeBlocks == null || activeBlocks.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<int[]>> byDay = new HashMap<>();
        for (ScheduleBlock block : activeBlocks) {
            if (block == null || !block.isActive() || !block.appliesToProfessional(professionalId)) {
                continue;
            }
            long start = Math.max(fromDay, block.startDay());
            long end = Math.min(toDay, block.endDay());
            if (start > end) {
                continue;
            }
            int minStart = block.isFullDay() ? 0 : block.startMin().orElse(0);
            int minEnd = block.isFullDay() ? MINUTES_PER_DAY : block.endMin().orElse(MINUTES_PER_DAY);
            for (long day = start; day <= end; day++) {
                if (!daysOff.contains(day)) {
                    byDay.computeIfAbsent(day, k -> new ArrayList<>())
                            .add(new int[]{minStart, minEnd});
                }
            }
        }
        return byDay;
    }

    private static List<int[]> mergeIntervals(List<int[]> raw) {
        if (raw.size() <= 1) {
            return raw;
        }
        List<int[]> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt(a -> a[1]));
        List<int[]> merged = new ArrayList<>(sorted.size());
        for (int[] cur : sorted) {
            if (merged.isEmpty()) {
                merged.add(new int[]{cur[0], cur[1]});
                continue;
            }
            int[] last = merged.get(merged.size() - 1);
            if (cur[0] <= last[1]) {
                last[1] = Math.max(last[1], cur[1]);
            } else {
                merged.add(new int[]{cur[0], cur[1]});
            }
        }
        return merged;
    }

    private static List<int[]> intersectIntervalLists(List<int[]> first, List<int[]> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return Collections.emptyList();
        }
        List<int[]> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < first.size() && j < second.size()) {
            int[] a = first.get(i);
            int[] b = second.get(j);
            int start = Math.max(a[0], b[0]);
            int end = Math.min(a[1], b[1]);
            if (start < end) {
                result.add(new int[]{start, end});
            }
            if (a[1] < b[1]) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }

    private static void addDayIntervals(long[] hourBuckets, List<int[]> intervals, long deltaSign) {
        for (int[] interval : intervals) {
            int startMin = interval[0];
            int endMin = interval[1];
            int firstHour = startMin / 60;
            int lastHour = (endMin - 1) / 60;
            for (int hour = firstHour; hour <= lastHour; hour++) {
                int hStart = hour * 60;
                int hEnd = hStart + 60;
                int overlap = Math.min(endMin, hEnd) - Math.max(startMin, hStart);
                if (overlap > 0) {
                    hourBuckets[hour] = Math.max(0L, hourBuckets[hour] + overlap * deltaSign);
                }
            }
        }
    }

    private static void accumulateOccupiedBuckets(
            List<OccupiedBucket> occupiedBuckets,
            long[][] occupied,
            int[][] appointments
    ) {
        if (occupiedBuckets == null) {
            return;
        }
        for (OccupiedBucket bucket : occupiedBuckets) {
            if (bucket == null) {
                continue;
            }
            int dow = bucket.dayOfWeek();
            int hour = bucket.slotHour();
            if (dow >= 1 && dow <= DAYS_PER_WEEK && hour >= 0 && hour < HOURS_PER_DAY) {
                occupied[dow][hour] += Math.max(0L, bucket.occupiedMinutes());
                appointments[dow][hour] += Math.max(0, bucket.appointmentCount());
            }
        }
    }

    private static OccupancyReport buildReport(
            long[][] working,
            long[][] occupied,
            int[][] appointments
    ) {
        long totalOccupied = 0L;
        long totalWorking = 0L;

        List<HourSlotRow> hourRows = buildHourRows(working, occupied, appointments);
        List<DayOfWeekRow> dowRows = buildDayOfWeekRows(working, occupied, appointments);
        List<WindowRow> windows = new ArrayList<>();

        for (int dow = 1; dow <= DAYS_PER_WEEK; dow++) {
            for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
                long workMin = working[dow][hour];
                long occMin = occupied[dow][hour];
                totalWorking += workMin;
                totalOccupied += occMin;
                if (workMin > 0L) {
                    windows.add(new WindowRow(
                            dow,
                            hour,
                            occMin,
                            workMin,
                            computeBasisPoints(occMin, workMin),
                            appointments[dow][hour]
                    ));
                }
            }
        }

        windows.sort(Comparator
                .comparingInt(WindowRow::occupancyBasisPoints)
                .thenComparing(Comparator.comparingLong(WindowRow::workingMinutes).reversed())
                .thenComparingInt(WindowRow::dayOfWeek)
                .thenComparingInt(WindowRow::slotHour));
        List<WindowRow> lowest = windows.size() > LOWEST_DEMAND_LIMIT
                ? windows.subList(0, LOWEST_DEMAND_LIMIT)
                : windows;

        return new OccupancyReport(
                totalOccupied,
                totalWorking,
                computeBasisPoints(totalOccupied, totalWorking),
                hourRows,
                dowRows,
                lowest
        );
    }

    private static List<HourSlotRow> buildHourRows(
            long[][] working,
            long[][] occupied,
            int[][] appointments
    ) {
        List<HourSlotRow> rows = new ArrayList<>();
        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            long workMin = 0L;
            long occMin = 0L;
            int appts = 0;
            for (int dow = 1; dow <= DAYS_PER_WEEK; dow++) {
                workMin += working[dow][hour];
                occMin += occupied[dow][hour];
                appts += appointments[dow][hour];
            }
            if (workMin > 0L || occMin > 0L) {
                rows.add(new HourSlotRow(
                        hour, occMin, workMin, computeBasisPoints(occMin, workMin), appts));
            }
        }
        return rows;
    }

    private static List<DayOfWeekRow> buildDayOfWeekRows(
            long[][] working,
            long[][] occupied,
            int[][] appointments
    ) {
        List<DayOfWeekRow> rows = new ArrayList<>();
        for (int dow = 1; dow <= DAYS_PER_WEEK; dow++) {
            long workMin = 0L;
            long occMin = 0L;
            int appts = 0;
            for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
                workMin += working[dow][hour];
                occMin += occupied[dow][hour];
                appts += appointments[dow][hour];
            }
            if (workMin > 0L || occMin > 0L) {
                rows.add(new DayOfWeekRow(
                        dow, occMin, workMin, computeBasisPoints(occMin, workMin), appts));
            }
        }
        return rows;
    }

    public static int computeBasisPoints(long occupiedMinutes, long workingMinutes) {
        if (workingMinutes <= 0L || occupiedMinutes <= 0L) {
            return 0;
        }
        return BigDecimal.valueOf(occupiedMinutes)
                .multiply(BASIS_POINTS_MAX)
                .divide(BigDecimal.valueOf(workingMinutes), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
