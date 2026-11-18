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
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.usecase.CancelRecurringSeriesUseCase.Scope;
import com.metanoia.domain.usecase.GenerateRecurringAppointmentsUseCase.RecurringResult;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GenerateRecurringAppointmentsUseCaseTest {

    // Lunes 16/11/2026
    private static final long START_MONDAY = LocalDate.of(2026, 11, 16).toEpochDay();

    private FakeAppointmentRepo appointmentRepo;
    private GenerateRecurringAppointmentsUseCase generateUseCase;
    private CancelRecurringSeriesUseCase cancelUseCase;

    @Before
    public void setUp() {
        appointmentRepo = new FakeAppointmentRepo();
        ClientRepository clientRepo = new ClientRepository() {
            private final Client cli = new Client.Builder()
                    .id("cli-1").name("Sofía").nameNormalized("sofia")
                    .phone("5491122334455").active(true).build();

            @Override
            public Optional<Client> findById(String id) {
                return "cli-1".equals(id) ? Optional.of(cli) : Optional.empty();
            }

            @Override
            public Optional<Client> findActiveByPhone(String p) {
                return Optional.empty();
            }

            @Override
            public List<ClientSummary> searchSummaries(String q, boolean inc) {
                return Collections.emptyList();
            }

            @Override
            public void save(Client client) {
            }
        };

        ProfessionalRepository proRepo = new ProfessionalRepository() {
            // Día libre exactamente en la 2da ocurrencia (+14 días = START_MONDAY + 14)
            private final Professional pro = new Professional.Builder()
                    .id("pro-1")
                    .name("Ana")
                    .active(true)
                    .workSchedules(Collections.singletonList(new TimeRange(1, 540, 1140)))
                    .serviceIds(Collections.singleton("svc-1"))
                    .daysOff(Collections.singleton(START_MONDAY + 14L))
                    .build();

            @Override
            public List<Professional> findAll(boolean inc) {
                return Collections.singletonList(pro);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(pro) : Optional.empty();
            }

            @Override
            public void save(Professional p) {
            }

            @Override
            public boolean hasFutureActiveAppointments(String p, long d) {
                return false;
            }
        };

        SalonServiceRepository serviceRepo = new SalonServiceRepository() {
            private final SalonService svc = new SalonService.Builder()
                    .id("svc-1").name("Color").category("Color")
                    .price(Money.ofCents(3_000_000L)).durationMin(60).active(true).build();

            @Override
            public List<SalonService> findAll(boolean inc) {
                return Collections.singletonList(svc);
            }

            @Override
            public Optional<SalonService> findById(String id) {
                return "svc-1".equals(id) ? Optional.of(svc) : Optional.empty();
            }

            @Override
            public Optional<SalonService> findActiveByCategoryAndNormalizedName(String c, String n) {
                return Optional.empty();
            }

            @Override
            public void save(SalonService s) {
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
                return 20_000L;
            }

            @Override
            public long todayEpochDay() {
                return START_MONDAY;
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
                return "rec-" + seq++;
            }
        };

        generateUseCase = new GenerateRecurringAppointmentsUseCase(
                appointmentRepo, clientRepo, proRepo, serviceRepo,
                blockRepo, settingsRepo, r -> { }, clock, ids);
        cancelUseCase = new CancelRecurringSeriesUseCase(
                appointmentRepo, settingsRepo, r -> { }, clock, ids);
    }

    @Test
    public void execute_skipsConflictedOccurrenceAndCreatesValidOnes() {
        Result<RecurringResult> result = generateUseCase.execute(
                new GenerateRecurringAppointmentsUseCase.Request(
                        "cli-1", "pro-1", START_MONDAY, 600,
                        Collections.singletonList("svc-1"), "", false, 2, null, 4));

        assertTrue(result.isSuccess());
        RecurringResult res = result.getValue();
        // 4 ocurrencias (+0, +14 día libre, +28, +42) => 3 creados y 1 omitido
        assertEquals(3, res.createdAppointments().size());
        assertEquals(1, res.conflicts().size());
        assertEquals(START_MONDAY + 14L, res.conflicts().get(0).dayEpoch());
        assertTrue(res.createdAppointments().get(0).recurrenceRuleId().isPresent());

        // Cancelar desde la segunda cita creada ("este y siguientes") conserva la primera activa
        String secondCreatedId = res.createdAppointments().get(1).id();
        Result<List<Appointment>> cancelled = cancelUseCase.execute(
                secondCreatedId, Scope.THIS_AND_FOLLOWING, "Vacaciones del cliente");
        assertTrue(cancelled.isSuccess());
        assertEquals(2, cancelled.getValue().size());

        String firstCreatedId = res.createdAppointments().get(0).id();
        assertEquals(AppointmentStatus.PENDING,
                appointmentRepo.findById(firstCreatedId).orElseThrow().status());
        assertEquals(AppointmentStatus.CANCELLED,
                appointmentRepo.findById(secondCreatedId).orElseThrow().status());
    }

    @Test
    public void execute_allOccurrencesConflicted_returnsConflictAndSavesNothing() {
        long busyDay = START_MONDAY + 14L;
        Result<RecurringResult> result = generateUseCase.execute(
                new GenerateRecurringAppointmentsUseCase.Request(
                        "cli-1", "pro-1", busyDay, 600,
                        Collections.singletonList("svc-1"), "", false, 1, busyDay, null));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.CONFLICT, result.getError());
        assertEquals(0, appointmentRepo.store.size());
    }

    private static final class FakeAppointmentRepo implements AppointmentRepository {
        final Map<String, Appointment> store = new LinkedHashMap<>();
        final List<RecurrenceRule> rules = new ArrayList<>();

        @Override
        public Optional<Appointment> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Appointment> findByDayAndProfessional(long d, String p) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findByDayRange(long startDay, long endDay) {
            List<Appointment> out = new ArrayList<>();
            for (Appointment a : store.values()) {
                if (a.day() >= startDay && a.day() <= endDay) {
                    out.add(a);
                }
            }
            return out;
        }

        @Override
        public List<Appointment> findFutureActiveFromSeries(
                String recurrenceRuleId, long fromDayEpoch, int fromStartMin) {
            List<Appointment> out = new ArrayList<>();
            for (Appointment a : store.values()) {
                if (a.recurrenceRuleId().isPresent()
                        && a.recurrenceRuleId().get().equals(recurrenceRuleId)
                        && a.status().isEditable()
                        && (a.day() > fromDayEpoch
                        || (a.day() == fromDayEpoch && a.startMin() >= fromStartMin))) {
                    out.add(a);
                }
            }
            out.sort(Comparator.comparingLong(Appointment::day).thenComparingInt(Appointment::startMin));
            return out;
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
            for (Appointment a : appointments) {
                save(a);
            }
        }

        @Override
        public void saveRecurrenceRule(RecurrenceRule rule) {
            rules.add(rule);
        }
    }
}
