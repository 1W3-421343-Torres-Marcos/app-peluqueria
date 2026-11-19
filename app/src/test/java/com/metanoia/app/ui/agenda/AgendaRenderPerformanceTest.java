package com.metanoia.app.ui.agenda;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark en JVM para garantizar que la preparación de filas diarias y semanales en la agenda
 * se mantenga por debajo de 100 ms incluso con miles de registros.
 */
public class AgendaRenderPerformanceTest {

    private static final long BASE_DAY = 20_500L;

    @Test
    public void dayTimelineBuildRows_handlesFiveHundredRowsUnderHundredMillis() {
        List<AppointmentCardProjection> appointments = buildSampleAppointments(500, 1);
        List<ScheduleBlockEntity> blocks = buildSampleBlocks(20, BASE_DAY, 1);
        Map<String, String> proNames = buildProfessionalNames();

        DayTimelineAdapter.buildRows(blocks, appointments, null, proNames);

        long startNs = System.nanoTime();
        List<DayTimelineAdapter.TimelineRow> rows =
                DayTimelineAdapter.buildRows(blocks, appointments, null, proNames);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertEquals(520, rows.size());
        assertTrue("buildRows superó 100 ms: " + elapsedMs + " ms", elapsedMs < 100L);
    }

    @Test
    public void weekDaysBuildWeek_handlesThreeThousandFiveHundredAppointmentsUnderHundredMillis() {
        List<AppointmentCardProjection> weekAppointments = buildSampleAppointments(3_500, 7);
        List<ScheduleBlockEntity> weekBlocks = buildSampleBlocks(20, BASE_DAY, 7);

        WeekDaysAdapter.buildWeek(
                BASE_DAY, BASE_DAY + 2L, BASE_DAY + 2L, weekAppointments, weekBlocks, null);

        long startNs = System.nanoTime();
        List<WeekDaysAdapter.WeekDaySummary> summaries = WeekDaysAdapter.buildWeek(
                BASE_DAY, BASE_DAY + 2L, BASE_DAY + 2L, weekAppointments, weekBlocks, null);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertEquals(7, summaries.size());
        int totalOccupying = 0;
        for (WeekDaysAdapter.WeekDaySummary summary : summaries) {
            totalOccupying += summary.appointmentCount;
        }
        assertEquals(3_500, totalOccupying);
        assertTrue("buildWeek superó 100 ms: " + elapsedMs + " ms", elapsedMs < 100L);
    }

    private static List<AppointmentCardProjection> buildSampleAppointments(int count, int daySpan) {
        List<AppointmentCardProjection> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long day = BASE_DAY + (i % daySpan);
            int start = 540 + (i % 10) * 30;
            String proId = (i % 2 == 0) ? "pro-1" : "pro-2";
            String proName = (i % 2 == 0) ? "Ana" : "Luis";
            list.add(new AppointmentCardProjection(
                    "apt-" + i,
                    "cli-" + (i % 50),
                    "Cliente " + (i % 50),
                    proId,
                    proName,
                    day,
                    start,
                    start + 30,
                    "CONFIRMED",
                    "Corte",
                    150_000L,
                    0L,
                    0L,
                    null
            ));
        }
        return list;
    }

    private static List<ScheduleBlockEntity> buildSampleBlocks(int count, long startDay, int daySpan) {
        List<ScheduleBlockEntity> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long day = startDay + (i % daySpan);
            String proId = (i % 3 == 0) ? null : "pro-1";
            list.add(new ScheduleBlockEntity(
                    "blk-" + i, proId, day, day, 780, 840, "PARTIAL", "Almuerzo", true, 1000L, 1000L));
        }
        return list;
    }

    private static Map<String, String> buildProfessionalNames() {
        Map<String, String> map = new HashMap<>();
        map.put("pro-1", "Ana");
        map.put("pro-2", "Luis");
        return map;
    }
}
