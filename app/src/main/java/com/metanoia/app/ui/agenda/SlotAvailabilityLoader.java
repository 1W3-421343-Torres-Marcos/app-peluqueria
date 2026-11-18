package com.metanoia.app.ui.agenda;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.AppContainer;
import com.metanoia.app.ui.common.TimeSlotGrid.SlotOption;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.rules.AvailabilityChecker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper compartido en UI para calcular bloques libres y ocupados desde los casos de creación
 * y reprogramación de turnos sin duplicar consultas ni lógica.
 */
public final class SlotAvailabilityLoader {

    public static final class SlotLoadResult {
        public final List<SlotOption> options;
        public final List<Integer> freeStartMinutes;

        public SlotLoadResult(List<SlotOption> options, List<Integer> freeStartMinutes) {
            this.options = options;
            this.freeStartMinutes = freeStartMinutes;
        }
    }

    private SlotAvailabilityLoader() {
    }

    @NonNull
    public static SlotLoadResult load(
            @NonNull AppContainer container,
            @NonNull String professionalId,
            long dayEpoch,
            int durationMin,
            @Nullable String excludedAppointmentId
    ) {
        if (durationMin <= 0) {
            return new SlotLoadResult(Collections.emptyList(), Collections.emptyList());
        }
        Optional<Professional> proOpt = container.professionalRepository().findById(professionalId);
        if (!proOpt.isPresent() || !proOpt.get().isActive()) {
            return new SlotLoadResult(Collections.emptyList(), Collections.emptyList());
        }
        Professional professional = proOpt.get();
        Settings settings = container.settingsRepository().getSettings();
        int slotMinutes = settings.slotMinutes();
        List<Appointment> existing = container.appointmentRepository()
                .findByDayAndProfessional(dayEpoch, professionalId);
        List<ScheduleBlock> blocks = container.scheduleBlockRepository()
                .findActiveOverlappingRange(dayEpoch, dayEpoch);

        List<Integer> freeList = AvailabilityChecker.suggestFreeStartMinutes(
                professional,
                dayEpoch,
                durationMin,
                slotMinutes,
                settings.generalSchedule(),
                existing,
                blocks,
                excludedAppointmentId
        );
        Set<Integer> freeSet = new LinkedHashSet<>(freeList);

        int dayOfWeek = LocalDate.ofEpochDay(dayEpoch).getDayOfWeek().getValue();
        List<TimeRange> proDayRanges = new ArrayList<>();
        for (TimeRange range : professional.workSchedules()) {
            if (range.dayOfWeek() == dayOfWeek) {
                proDayRanges.add(range);
            }
        }
        Collections.sort(proDayRanges);

        Set<Integer> seenStarts = new LinkedHashSet<>();
        List<SlotOption> options = new ArrayList<>();
        for (TimeRange range : proDayRanges) {
            for (int start = range.startMin();
                    start + durationMin <= range.endMin();
                    start += slotMinutes) {
                if (seenStarts.add(start)) {
                    options.add(new SlotOption(start, freeSet.contains(start)));
                }
            }
        }
        return new SlotLoadResult(options, freeList);
    }
}
