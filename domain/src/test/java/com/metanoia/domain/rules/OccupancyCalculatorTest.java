package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.OccupancyReport.DayOfWeekRow;
import com.metanoia.domain.model.OccupancyReport.HourSlotRow;
import com.metanoia.domain.model.OccupancyReport.OccupiedBucket;
import com.metanoia.domain.model.OccupancyReport.WindowRow;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.ScheduleBlockType;
import com.metanoia.domain.model.TimeRange;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class OccupancyCalculatorTest {

    @Test
    public void calculate_intersectsSalonScheduleAndIdentifiesLowestDemandWindows() {
        long monday = LocalDate.of(2026, 1, 5).toEpochDay(); // ISO 1 (lunes)
        Professional ana = new Professional.Builder()
                .id("pro-1")
                .name("Ana")
                .workSchedules(Arrays.asList(
                        new TimeRange(1, 9 * 60, 13 * 60),
                        new TimeRange(1, 14 * 60, 18 * 60)
                ))
                .build();
        List<TimeRange> salonSchedule = Collections.singletonList(
                new TimeRange(1, 10 * 60, 17 * 60));

        // Efectivo: 10-13 (180m) + 14-17 (180m) = 360m en horas 10,11,12,14,15,16 (60m c/u)
        List<OccupiedBucket> occupied = Arrays.asList(
                new OccupiedBucket(1, 10, 30L, 1),
                new OccupiedBucket(1, 14, 60L, 1)
        );

        OccupancyReport report = OccupancyCalculator.calculate(
                monday,
                monday,
                Collections.singletonList(ana),
                salonSchedule,
                Collections.emptyList(),
                occupied
        );

        assertEquals(360L, report.totalWorkingMinutes());
        assertEquals(90L, report.totalOccupiedMinutes());
        assertEquals(2500, report.overallOccupancyBasisPoints());

        assertEquals(1, report.byDayOfWeek().size());
        DayOfWeekRow monRow = report.byDayOfWeek().get(0);
        assertEquals(1, monRow.dayOfWeek());
        assertEquals(2500, monRow.occupancyBasisPoints());

        assertEquals(6, report.byHourSlot().size());
        HourSlotRow hour14 = report.byHourSlot().get(3);
        assertEquals(14, hour14.slotHour());
        assertEquals(10000, hour14.occupancyBasisPoints());

        List<WindowRow> lowest = report.lowestDemandWindows();
        assertEquals(5, lowest.size());
        // Horas 11, 12, 15, 16 están al 0 %, luego hora 10 al 50 %
        assertEquals(0, lowest.get(0).occupancyBasisPoints());
        assertEquals(11, lowest.get(0).slotHour());
        assertEquals(12, lowest.get(1).slotHour());
        assertEquals(15, lowest.get(2).slotHour());
        assertEquals(16, lowest.get(3).slotHour());
        assertEquals(10, lowest.get(4).slotHour());
        assertEquals(5000, lowest.get(4).occupancyBasisPoints());
    }

    @Test
    public void calculate_avoidsDoubleSubtractionBetweenDaysOffAndOverlappingBlocks() {
        long monday = LocalDate.of(2026, 1, 5).toEpochDay();
        long tuesday = LocalDate.of(2026, 1, 6).toEpochDay();

        Professional carla = new Professional.Builder()
                .id("pro-2")
                .name("Carla")
                .workSchedules(Arrays.asList(
                        new TimeRange(1, 9 * 60, 11 * 60),
                        new TimeRange(2, 9 * 60, 11 * 60)
                ))
                .daysOff(new HashSet<>(Collections.singletonList(monday)))
                .build();

        ScheduleBlock mondayGeneralBlock = new ScheduleBlock.Builder()
                .id("blk-1")
                .startDay(monday)
                .endDay(monday)
                .type(ScheduleBlockType.HOLIDAY)
                .reason("Feriado")
                .build();
        ScheduleBlock tuesdayBlockA = new ScheduleBlock.Builder()
                .id("blk-2")
                .professionalId("pro-2")
                .startDay(tuesday)
                .endDay(tuesday)
                .startMin(9 * 60)
                .endMin(10 * 60)
                .type(ScheduleBlockType.LUNCH)
                .reason("Almuerzo")
                .build();
        ScheduleBlock tuesdayBlockB = new ScheduleBlock.Builder()
                .id("blk-3")
                .startDay(tuesday)
                .endDay(tuesday)
                .startMin(9 * 60 + 30)
                .endMin(10 * 60 + 30)
                .type(ScheduleBlockType.OTHER)
                .reason("Curso")
                .build();

        // Martes queda solo [10:30, 11:00) = 30 min laborables; ocupado 15 min => 50 %
        OccupancyReport report = OccupancyCalculator.calculate(
                monday,
                tuesday,
                Collections.singletonList(carla),
                Collections.emptyList(),
                Arrays.asList(mondayGeneralBlock, tuesdayBlockA, tuesdayBlockB),
                Collections.singletonList(new OccupiedBucket(2, 10, 15L, 1))
        );

        assertEquals(30L, report.totalWorkingMinutes());
        assertEquals(15L, report.totalOccupiedMinutes());
        assertEquals(5000, report.overallOccupancyBasisPoints());
        assertEquals(1, report.lowestDemandWindows().size());
        assertEquals(2, report.lowestDemandWindows().get(0).dayOfWeek());
        assertEquals(10, report.lowestDemandWindows().get(0).slotHour());
    }

    @Test
    public void calculate_handlesZeroWorkingMinutesWithoutDivisionByZero() {
        long sunday = LocalDate.of(2026, 1, 11).toEpochDay();
        Professional ana = new Professional.Builder()
                .id("pro-1")
                .name("Ana")
                .workSchedules(Collections.singletonList(new TimeRange(1, 9 * 60, 12 * 60)))
                .build();

        OccupancyReport report = OccupancyCalculator.calculate(
                sunday,
                sunday,
                Collections.singletonList(ana),
                Collections.singletonList(new TimeRange(1, 9 * 60, 18 * 60)),
                Collections.emptyList(),
                Collections.singletonList(new OccupiedBucket(7, 10, 30L, 1))
        );

        assertEquals(0L, report.totalWorkingMinutes());
        assertEquals(30L, report.totalOccupiedMinutes());
        assertEquals(0, report.overallOccupancyBasisPoints());
        assertTrue(report.lowestDemandWindows().isEmpty());
        assertEquals(1, report.byDayOfWeek().size());
        assertEquals(7, report.byDayOfWeek().get(0).dayOfWeek());
    }
}
