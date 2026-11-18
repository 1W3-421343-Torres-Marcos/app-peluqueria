package com.metanoia.domain.rules;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.TimeRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Reglas puras para verificar disponibilidad horaria y sugerir bloques libres sin solapamientos.
 */
public final class AvailabilityChecker {

    private static final int MINUTES_PER_DAY = 1440;

    public enum UnavailabilityReason {
        NONE,
        PROFESSIONAL_INACTIVE,
        INVALID_TIME_RANGE,
        OUTSIDE_SALON_HOURS,
        OUTSIDE_PROFESSIONAL_HOURS,
        PROFESSIONAL_DAY_OFF,
        BLOCKED_BY_SCHEDULE_BLOCK,
        OVERLAPS_EXISTING_APPOINTMENT;

        public boolean isAvailable() {
            return this == NONE;
        }
    }

    private AvailabilityChecker() {
    }

    /**
     * Evalúa si el intervalo semiabierto {@code [startMin, endMin)} está disponible para el
     * profesional indicado en la fecha {@code dayEpoch}.
     */
    public static UnavailabilityReason check(
            Professional professional,
            long dayEpoch,
            int startMin,
            int endMin,
            List<TimeRange> salonSchedule,
            List<Appointment> existingAppointmentsOnDay,
            List<ScheduleBlock> blocksOnDay,
            String excludedAppointmentId
    ) {
        if (professional == null || !professional.isActive()) {
            return UnavailabilityReason.PROFESSIONAL_INACTIVE;
        }
        if (startMin < 0 || endMin > MINUTES_PER_DAY || startMin >= endMin) {
            return UnavailabilityReason.INVALID_TIME_RANGE;
        }
        if (professional.daysOff().contains(dayEpoch)) {
            return UnavailabilityReason.PROFESSIONAL_DAY_OFF;
        }

        int dayOfWeek = LocalDate.ofEpochDay(dayEpoch).getDayOfWeek().getValue();
        TimeRange requested = new TimeRange(dayOfWeek, startMin, endMin);
        List<TimeRange> requestedSingleton = Collections.singletonList(requested);

        if (salonSchedule == null
                || !ScheduleValidator.isWithinSchedule(requestedSingleton, salonSchedule)) {
            return UnavailabilityReason.OUTSIDE_SALON_HOURS;
        }
        if (!ScheduleValidator.isWithinSchedule(requestedSingleton, professional.workSchedules())) {
            return UnavailabilityReason.OUTSIDE_PROFESSIONAL_HOURS;
        }

        if (blocksOnDay != null) {
            for (ScheduleBlock block : blocksOnDay) {
                if (block != null
                        && block.appliesToDay(dayEpoch)
                        && block.appliesToProfessional(professional.id())
                        && block.overlapsTimeRange(startMin, endMin)) {
                    return UnavailabilityReason.BLOCKED_BY_SCHEDULE_BLOCK;
                }
            }
        }

        if (existingAppointmentsOnDay != null) {
            for (Appointment existing : existingAppointmentsOnDay) {
                if (existing == null || !existing.status().occupiesSchedule()) {
                    continue;
                }
                if (excludedAppointmentId != null && excludedAppointmentId.equals(existing.id())) {
                    continue;
                }
                if (existing.day() == dayEpoch
                        && existing.professionalId().equals(professional.id())
                        && existing.startMin() < endMin
                        && startMin < existing.endMin()) {
                    return UnavailabilityReason.OVERLAPS_EXISTING_APPOINTMENT;
                }
            }
        }
        return UnavailabilityReason.NONE;
    }

    /**
     * Devuelve los minutos de inicio disponibles para una duración dada avanzando según {@code slotMinutes}.
     */
    public static List<Integer> suggestFreeStartMinutes(
            Professional professional,
            long dayEpoch,
            int durationMin,
            int slotMinutes,
            List<TimeRange> salonSchedule,
            List<Appointment> existingAppointmentsOnDay,
            List<ScheduleBlock> blocksOnDay,
            String excludedAppointmentId
    ) {
        Objects.requireNonNull(salonSchedule, "salonSchedule");
        if (professional == null || !professional.isActive()
                || durationMin <= 0 || slotMinutes <= 0
                || professional.daysOff().contains(dayEpoch)) {
            return Collections.emptyList();
        }
        int dayOfWeek = LocalDate.ofEpochDay(dayEpoch).getDayOfWeek().getValue();
        List<TimeRange> sortedProRanges = new ArrayList<>();
        for (TimeRange range : professional.workSchedules()) {
            if (range.dayOfWeek() == dayOfWeek) {
                sortedProRanges.add(range);
            }
        }
        Collections.sort(sortedProRanges);

        Set<Integer> freeStarts = new LinkedHashSet<>();
        for (TimeRange range : sortedProRanges) {
            for (int start = range.startMin();
                    start + durationMin <= range.endMin();
                    start += slotMinutes) {
                UnavailabilityReason reason = check(
                        professional,
                        dayEpoch,
                        start,
                        start + durationMin,
                        salonSchedule,
                        existingAppointmentsOnDay,
                        blocksOnDay,
                        excludedAppointmentId
                );
                if (reason.isAvailable()) {
                    freeStarts.add(start);
                }
            }
        }
        return new ArrayList<>(freeStarts);
    }
}
