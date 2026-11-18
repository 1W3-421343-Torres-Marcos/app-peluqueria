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
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UpdateAppointmentUseCaseTest {

    private static final long MONDAY = LocalDate.of(2026, 11, 16).toEpochDay();

    private FakeAppointmentRepo appointmentRepo;
    private FakeServiceRepo serviceRepo;
    private long currentToday = MONDAY;
    private UpdateAppointmentUseCase useCase;

    @Before
    public void setUp() {
        appointmentRepo = new FakeAppointmentRepo();
        serviceRepo = new FakeServiceRepo();

        serviceRepo.save(new SalonService.Builder()
                .id("svc-cut")
                .name("Corte actualizado")
                .category("Corte")
                .price(Money.ofCents(2_500_000L))
                .durationMin(30)
                .active(true)
                .build());
        serviceRepo.save(new SalonService.Builder()
                .id("svc-brush")
                .name("Brushing")
                .category("Peinado")
                .price(Money.ofCents(1_200_000L))
                .durationMin(30)
                .active(true)
                .build());

        ProfessionalRepository proRepo = new ProfessionalRepository() {
            private final Professional ana = new Professional.Builder()
                    .id("pro-1")
                    .name("Ana")
                    .active(true)
                    .workSchedules(Collections.singletonList(new TimeRange(1, 540, 1140)))
                    .serviceIds(new LinkedHashSet<>(Arrays.asList("svc-cut", "svc-brush")))
                    .build();

            @Override
            public List<Professional> findAll(boolean includeInactive) {
                return Collections.singletonList(ana);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(ana) : Optional.empty();
            }

            @Override
            public void save(Professional professional) {
            }

            @Override
            public boolean hasFutureActiveAppointments(String professionalId, long fromEpochDay) {
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
        AuditLogRepository auditRepo = record -> { };
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 2000L;
            }

            @Override
            public long todayEpochDay() {
                return currentToday;
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
        IdGenerator ids = new IdGenerator() {
            private int seq = 1;

            @Override
            public String nextId() {
                return "upd-" + seq++;
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

        appointmentRepo.save(sampleAppointment("apt-1", 600, 630, AppointmentStatus.CONFIRMED));
        useCase = new UpdateAppointmentUseCase(
                appointmentRepo, proRepo, serviceRepo, blockRepo,
                settingsRepo, auditRepo, clock, ids);
    }

    @Test
    public void execute_keepsOriginalItemSnapshotAndAppendsNewService() {
        Result<Appointment> result = useCase.execute(new UpdateAppointmentUseCase.Request(
                "apt-1", Arrays.asList("svc-cut", "svc-brush"), "Nueva nota"));

        assertTrue(result.isSuccess());
        Appointment updated = result.getValue();
        assertEquals(660, updated.endMin());
        // svc-cut mantiene precio/nombre original congelado ($15.000, no $25.000)
        assertEquals(Money.ofCents(1_500_000L), updated.items().get(0).priceSnapshot());
        assertEquals("Corte", updated.items().get(0).serviceNameSnapshot());
        // svc-brush toma catálogo actual ($12.000)
        assertEquals(Money.ofCents(1_200_000L), updated.items().get(1).priceSnapshot());
        assertEquals(Money.ofCents(2_700_000L), updated.totalAmount());
    }

    @Test
    public void execute_expandedDurationOverlapsNextAppointment_returnsConflict() {
        appointmentRepo.save(sampleAppointment("apt-next", 630, 660, AppointmentStatus.PENDING));

        Result<Appointment> result = useCase.execute(new UpdateAppointmentUseCase.Request(
                "apt-1", Arrays.asList("svc-cut", "svc-brush"), ""));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.CONFLICT, result.getError());
    }

    @Test
    public void execute_completedOrPastAppointment_returnsConflict() {
        appointmentRepo.save(sampleAppointment("apt-done", 700, 730, AppointmentStatus.COMPLETED));
        Result<Appointment> completedEdit = useCase.execute(new UpdateAppointmentUseCase.Request(
                "apt-done", Collections.singletonList("svc-cut"), ""));
        assertEquals(DomainError.CONFLICT, completedEdit.getError());

        currentToday = MONDAY + 1;
        Result<Appointment> pastEdit = useCase.execute(new UpdateAppointmentUseCase.Request(
                "apt-1", Collections.singletonList("svc-cut"), ""));
        assertEquals(DomainError.CONFLICT, pastEdit.getError());
    }

    private static Appointment sampleAppointment(
            String id, int startMin, int endMin, AppointmentStatus status) {
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(MONDAY)
                .startMin(startMin)
                .endMin(endMin)
                .status(status)
                .items(Collections.singletonList(new AppointmentItem.Builder()
                        .id("item-" + id)
                        .appointmentId(id)
                        .serviceId("svc-cut")
                        .serviceNameSnapshot("Corte")
                        .priceSnapshot(Money.ofCents(1_500_000L))
                        .durationMinSnapshot(endMin - startMin)
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
        public List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId) {
            List<Appointment> out = new ArrayList<>();
            for (Appointment a : store.values()) {
                if (a.day() == dayEpoch && a.professionalId().equals(professionalId)) {
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

    private static final class FakeServiceRepo implements SalonServiceRepository {
        private final Map<String, SalonService> store = new LinkedHashMap<>();

        @Override
        public List<SalonService> findAll(boolean includeInactive) {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<SalonService> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<SalonService> findActiveByCategoryAndNormalizedName(String c, String n) {
            return Optional.empty();
        }

        @Override
        public void save(SalonService service) {
            store.put(service.id(), service);
        }
    }
}
