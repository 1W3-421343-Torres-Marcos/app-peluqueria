package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.ScheduleBlockType;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.rules.AvailabilityChecker.UnavailabilityReason;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AvailabilityCheckerTest {

    // Lunes 16/11/2026 (dayOfWeek = 1)
    private static final long MONDAY = LocalDate.of(2026, 11, 16).toEpochDay();
    private static final long SUNDAY = LocalDate.of(2026, 11, 22).toEpochDay();

    private List<TimeRange> salonSchedule;
    private Professional professional;

    @Before
    public void setUp() {
        salonSchedule = Collections.singletonList(new TimeRange(1, 540, 1200)); // 09:00-20:00
        professional = new Professional.Builder()
                .id("pro-1")
                .name("Ana")
                .workSchedules(Arrays.asList(
                        new TimeRange(1, 540, 780),  // 09:00-13:00
                        new TimeRange(1, 900, 1140)  // 15:00-19:00
                ))
                .serviceIds(Collections.singleton("svc-1"))
                .build();
    }

    @Test
    public void check_adjacentEdgesAllowedAndOverlapsRejected() {
        List<Appointment> existing = Collections.singletonList(
                appointment("apt-1", "pro-1", MONDAY, 600, 660, AppointmentStatus.CONFIRMED)
        );

        // Bordes exactos [09:00-10:00) y [11:00-12:00) permitidos
        assertTrue(AvailabilityChecker.check(
                professional, MONDAY, 540, 600, salonSchedule, existing, null, null).isAvailable());
        assertTrue(AvailabilityChecker.check(
                professional, MONDAY, 660, 720, salonSchedule, existing, null, null).isAvailable());

        // Solapamiento parcial, contenido e idéntico rechazados
        assertEquals(UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT, AvailabilityChecker.check(
                professional, MONDAY, 570, 630, salonSchedule, existing, null, null));
        assertEquals(UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT, AvailabilityChecker.check(
                professional, MONDAY, 615, 645, salonSchedule, existing, null, null));
        assertEquals(UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT, AvailabilityChecker.check(
                professional, MONDAY, 590, 670, salonSchedule, existing, null, null));
        assertEquals(UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT, AvailabilityChecker.check(
                professional, MONDAY, 600, 660, salonSchedule, existing, null, null));

        // Mismo ID excluido permite editar/reprogramar en su lugar
        assertTrue(AvailabilityChecker.check(
                professional, MONDAY, 600, 660, salonSchedule, existing, null, "apt-1").isAvailable());
    }

    @Test
    public void check_completedAndPaidOccupyWhileCancelledAndNoShowFreeSlot() {
        List<Appointment> occupying = Arrays.asList(
                appointment("a-comp", "pro-1", MONDAY, 600, 660, AppointmentStatus.COMPLETED),
                appointment("a-paid", "pro-1", MONDAY, 660, 720, AppointmentStatus.PAID),
                appointment("a-canc", "pro-1", MONDAY, 900, 960, AppointmentStatus.CANCELLED),
                appointment("a-nosh", "pro-1", MONDAY, 960, 1020, AppointmentStatus.NO_SHOW)
        );

        assertEquals(UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT, AvailabilityChecker.check(
                professional, MONDAY, 600, 630, salonSchedule, occupying, null, null));
        assertEquals(UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT, AvailabilityChecker.check(
                professional, MONDAY, 660, 690, salonSchedule, occupying, null, null));

        assertTrue(AvailabilityChecker.check(
                professional, MONDAY, 900, 960, salonSchedule, occupying, null, null).isAvailable());
        assertTrue(AvailabilityChecker.check(
                professional, MONDAY, 960, 1020, salonSchedule, occupying, null, null).isAvailable());
    }

    @Test
    public void check_outsideHoursDayOffAndBlocks_rejectedWithExactReasons() {
        // Fuera de horario del salón (domingo cerrado)
        assertEquals(UnavailabilityReason.OUTSIDE_SALON_HOURS, AvailabilityChecker.check(
                professional, SUNDAY, 600, 660, salonSchedule, null, null, null));

        // Dentro de horario del salón pero en pausa del profesional (13:00-15:00)
        assertEquals(UnavailabilityReason.OUTSIDE_PROFESSIONAL_HOURS, AvailabilityChecker.check(
                professional, MONDAY, 780, 840, salonSchedule, null, null, null));

        // Día libre puntual del profesional
        Professional onDayOff = professional.toBuilder()
                .daysOff(Collections.singleton(MONDAY))
                .build();
        assertEquals(UnavailabilityReason.PROFESSIONAL_DAY_OFF, AvailabilityChecker.check(
                onDayOff, MONDAY, 600, 660, salonSchedule, null, null, null));

        // Bloqueos: general, parcial del profesional, inactivo y de otro profesional
        ScheduleBlock partialOwn = new ScheduleBlock.Builder()
                .id("blk-1")
                .professionalId("pro-1")
                .startDay(MONDAY)
                .endDay(MONDAY)
                .startMin(600)
                .endMin(660)
                .type(ScheduleBlockType.LUNCH)
                .reason("Almuerzo")
                .active(true)
                .build();
        ScheduleBlock fullOtherPro = new ScheduleBlock.Builder()
                .id("blk-2")
                .professionalId("pro-2")
                .startDay(MONDAY)
                .endDay(MONDAY)
                .type(ScheduleBlockType.VACATION)
                .reason("Otro")
                .active(true)
                .build();
        ScheduleBlock inactiveGeneral = new ScheduleBlock.Builder()
                .id("blk-3")
                .startDay(MONDAY)
                .endDay(MONDAY)
                .type(ScheduleBlockType.HOLIDAY)
                .reason("Anulado")
                .active(false)
                .build();

        List<ScheduleBlock> blocks = Arrays.asList(partialOwn, fullOtherPro, inactiveGeneral);
        assertEquals(UnavailabilityReason.BLOCKED_BY_SCHEDULE_BLOCK, AvailabilityChecker.check(
                professional, MONDAY, 630, 690, salonSchedule, null, blocks, null));
        assertTrue(AvailabilityChecker.check(
                professional, MONDAY, 660, 720, salonSchedule, null, blocks, null).isAvailable());

        ScheduleBlock generalFullDay = new ScheduleBlock.Builder()
                .id("blk-4")
                .startDay(MONDAY)
                .endDay(MONDAY)
                .type(ScheduleBlockType.HOLIDAY)
                .reason("Feriado")
                .active(true)
                .build();
        assertEquals(UnavailabilityReason.BLOCKED_BY_SCHEDULE_BLOCK, AvailabilityChecker.check(
                professional, MONDAY, 900, 960, salonSchedule, null,
                Collections.singletonList(generalFullDay), null));
    }

    @Test
    public void suggestFreeStartMinutes_returnsOnlyAvailableSlots() {
        Professional simplePro = new Professional.Builder()
                .id("pro-1")
                .name("Ana")
                .workSchedules(Collections.singletonList(new TimeRange(1, 540, 660))) // 09:00-11:00
                .build();
        List<Appointment> existing = Collections.singletonList(
                appointment("apt-1", "pro-1", MONDAY, 570, 600, AppointmentStatus.PENDING)
        );
        List<Integer> slots = AvailabilityChecker.suggestFreeStartMinutes(
                simplePro, MONDAY, 30, 30, salonSchedule, existing, Collections.emptyList(), null);

        // Los horarios superpuestos siguen disponibles y quedan marcados para la UI.
        assertEquals(Arrays.asList(540, 570, 600, 630), slots);
    }

    private static Appointment appointment(
            String id,
            String proId,
            long day,
            int startMin,
            int endMin,
            AppointmentStatus status
    ) {
        AppointmentItem item = new AppointmentItem.Builder()
                .id("item-" + id)
                .appointmentId(id)
                .serviceId("svc-1")
                .serviceNameSnapshot("Corte")
                .priceSnapshot(Money.ofCents(1_500_000L))
                .durationMinSnapshot(endMin - startMin)
                .position(0)
                .build();
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId(proId)
                .day(day)
                .startMin(startMin)
                .endMin(endMin)
                .status(status)
                .items(Collections.singletonList(item))
                .createdAt(1000L)
                .updatedAt(1000L)
                .createdBy("Dueño")
                .build();
    }
}
