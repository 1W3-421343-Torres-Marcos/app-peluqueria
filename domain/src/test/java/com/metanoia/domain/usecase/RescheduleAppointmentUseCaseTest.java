package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RescheduleAppointmentUseCaseTest {

    private static final long MONDAY = LocalDate.of(2026, 11, 16).toEpochDay();
    private static final long TUESDAY = MONDAY + 1L;

    private FakeAppointmentRepo appointmentRepo;
    private RescheduleAppointmentUseCase useCase;

    @Before
    public void setUp() {
        appointmentRepo = new FakeAppointmentRepo();
        ProfessionalRepository proRepo = new ProfessionalRepository() {
            private final Professional pro1 = new Professional.Builder()
                    .id("pro-1")
                    .name("Ana")
                    .active(true)
                    .workSchedules(java.util.Arrays.asList(
                            new TimeRange(1, 540, 1140),
                            new TimeRange(2, 540, 1140)
                    ))
                    .serviceIds(Collections.singleton("svc-1"))
                    .build();
            private final Professional pro2 = new Professional.Builder()
                    .id("pro-2")
                    .name("Leo")
                    .active(true)
                    .workSchedules(Collections.singletonList(new TimeRange(2, 540, 1140)))
                    .serviceIds(Collections.singleton("svc-other"))
                    .build();

            @Override
            public List<Professional> findAll(boolean inc) {
                return java.util.Arrays.asList(pro1, pro2);
            }

            @Override
            public Optional<Professional> findById(String id) {
                if ("pro-1".equals(id)) {
                    return Optional.of(pro1);
                }
                if ("pro-2".equals(id)) {
                    return Optional.of(pro2);
                }
                return Optional.empty();
            }

            @Override
            public void save(Professional p) {
            }

            @Override
            public boolean hasFutureActiveAppointments(String p, long d) {
                return false;
            }
        };

        SettingsRepository settingsRepo = new SettingsRepository() {
            @Override
            public Settings getSettings() {
                return Settings.defaultSettings();
            }

            @Override
            public void saveSettings(Settings settings) {
            }
        };
        ScheduleBlockRepository blockRepo = new ScheduleBlockRepository() {
            @Override
            public Optional<ScheduleBlock> findById(String id) {
                return Optional.empty();
            }

            @Override
            public List<ScheduleBlock> findActiveOverlappingRange(long s, long e) {
                return Collections.emptyList();
            }

            @Override
            public void save(ScheduleBlock block) {
            }
        };
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 9000L;
            }

            @Override
            public long todayEpochDay() {
                return MONDAY;
            }

            @Override
            public int currentMinuteOfDay() {
                return 540;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
        IdGenerator ids = () -> "aud-r";

        appointmentRepo.save(sample("apt-1", MONDAY, 600, 630, AppointmentStatus.CONFIRMED));
        appointmentRepo.save(sample("apt-busy", TUESDAY, 720, 750, AppointmentStatus.CONFIRMED));

        useCase = new RescheduleAppointmentUseCase(
                appointmentRepo, proRepo, blockRepo, settingsRepo, r -> { }, clock, ids);
    }

    @Test
    public void execute_validSlot_movesDayAndTimeKeepingIdAndSnapshots() {
        Result<Appointment> moved = useCase.execute(
                new RescheduleAppointmentUseCase.Request("apt-1", "pro-1", TUESDAY, 660));

        assertTrue(moved.isSuccess());
        assertEquals("apt-1", moved.getValue().id());
        assertEquals(TUESDAY, moved.getValue().day());
        assertEquals(660, moved.getValue().startMin());
        assertEquals(690, moved.getValue().endMin());
        assertEquals(Money.ofCents(1_500_000L), moved.getValue().items().get(0).priceSnapshot());
    }

    @Test
    public void execute_conflictingSlotOrUnsupportedPro_fails() {
        Result<Appointment> overlap = useCase.execute(
                new RescheduleAppointmentUseCase.Request("apt-1", "pro-1", TUESDAY, 720));
        assertTrue(overlap.isSuccess());

        Result<Appointment> wrongPro = useCase.execute(
                new RescheduleAppointmentUseCase.Request("apt-1", "pro-2", TUESDAY, 600));
        assertFalse(wrongPro.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, wrongPro.getError());
    }

    private static Appointment sample(
            String id, long day, int startMin, int endMin, AppointmentStatus status) {
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(day)
                .startMin(startMin)
                .endMin(endMin)
                .status(status)
                .items(Collections.singletonList(new AppointmentItem.Builder()
                        .id("i-" + id)
                        .appointmentId(id)
                        .serviceId("svc-1")
                        .serviceNameSnapshot("Corte")
                        .priceSnapshot(Money.ofCents(1_500_000L))
                        .durationMinSnapshot(30)
                        .position(0)
                        .build()))
                .createdAt(1000L)
                .updatedAt(1000L)
                .createdBy("Dueño")
                .build();
    }

    private static final class FakeAppointmentRepo implements AppointmentRepository {
        final Map<String, Appointment> store = new LinkedHashMap<>();

        @Override
        public Optional<Appointment> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Appointment> findByDayAndProfessional(long day, String proId) {
            List<Appointment> out = new ArrayList<>();
            for (Appointment a : store.values()) {
                if (a.day() == day && a.professionalId().equals(proId)) {
                    out.add(a);
                }
            }
            return out;
        }

        @Override
        public List<Appointment> findByDayRange(long s, long e) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findFutureActiveFromSeries(String r, long d, int m) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasFutureActiveForProfessional(String p, long d) {
            return false;
        }

        @Override
        public void save(Appointment appointment) {
            store.put(appointment.id(), appointment);
        }

        @Override
        public void saveAll(List<Appointment> appointments) {
        }

        @Override
        public void saveRecurrenceRule(RecurrenceRule rule) {
        }
    }
}
