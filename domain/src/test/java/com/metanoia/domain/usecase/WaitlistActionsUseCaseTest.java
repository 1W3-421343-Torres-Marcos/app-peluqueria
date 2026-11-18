package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
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
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;
import com.metanoia.domain.usecase.AssignWaitlistEntryUseCase.AssignResult;
import com.metanoia.domain.usecase.OfferWaitlistEntryUseCase.OfferResult;

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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitlistActionsUseCaseTest {

    // Viernes 20/11/2026 (dayOfWeek = 5)
    private static final long FRIDAY = LocalDate.of(2026, 11, 20).toEpochDay();

    private FakeWaitlistRepo waitlistRepo;
    private FakeAppointmentRepo appointmentRepo;
    private OfferWaitlistEntryUseCase offerUseCase;
    private AssignWaitlistEntryUseCase assignUseCase;

    @Before
    public void setUp() {
        waitlistRepo = new FakeWaitlistRepo();
        appointmentRepo = new FakeAppointmentRepo();

        ClientRepository clientRepo = new ClientRepository() {
            private final Client client = new Client.Builder()
                    .id("cli-1").name("Sofía Martínez").nameNormalized("sofia martinez")
                    .phone("5491122334455").active(true).build();

            @Override
            public Optional<Client> findById(String id) {
                return "cli-1".equals(id) ? Optional.of(client) : Optional.empty();
            }

            @Override
            public Optional<Client> findActiveByPhone(String phone) {
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
            private final Professional ana = new Professional.Builder()
                    .id("pro-1")
                    .name("Ana")
                    .active(true)
                    .workSchedules(Collections.singletonList(new TimeRange(5, 540, 1140)))
                    .serviceIds(Collections.singleton("svc-cut"))
                    .build();

            @Override
            public List<Professional> findAll(boolean inc) {
                return Collections.singletonList(ana);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(ana) : Optional.empty();
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
            private final SalonService cut = new SalonService.Builder()
                    .id("svc-cut").name("Corte").category("Corte")
                    .price(Money.ofCents(1_500_000L)).durationMin(30).active(true).build();

            @Override
            public List<SalonService> findAll(boolean inc) {
                return Collections.singletonList(cut);
            }

            @Override
            public Optional<SalonService> findById(String id) {
                return "svc-cut".equals(id) ? Optional.of(cut) : Optional.empty();
            }

            @Override
            public Optional<SalonService> findActiveByCategoryAndNormalizedName(String c, String n) {
                return Optional.empty();
            }

            @Override
            public void save(SalonService s) {
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
                return 88_000L;
            }

            @Override
            public long todayEpochDay() {
                return FRIDAY;
            }

            @Override
            public int currentMinuteOfDay() {
                return 540;
            }

            @Override
            public ZoneId zoneId() {
                return Clock.ARGENTINA_ZONE;
            }
        };
        AtomicInteger seq = new AtomicInteger(1);
        IdGenerator ids = () -> "gen-" + seq.getAndIncrement();

        CreateAppointmentUseCase createAptUseCase = new CreateAppointmentUseCase(
                appointmentRepo, clientRepo, proRepo, serviceRepo, blockRepo,
                settingsRepo, auditRepo, clock, ids);

        offerUseCase = new OfferWaitlistEntryUseCase(
                waitlistRepo, clientRepo, proRepo, serviceRepo,
                settingsRepo, auditRepo, clock, ids);
        assignUseCase = new AssignWaitlistEntryUseCase(
                waitlistRepo, createAptUseCase, settingsRepo, auditRepo, clock, ids);
    }

    @Test
    public void offerWaitlistEntry_renderizaPlantillaYMarcaOffered() {
        waitlistRepo.save(sampleEntry("wl-1"));
        FreedSlot slot = new FreedSlot("pro-1", FRIDAY, 600, 660);

        Result<OfferResult> res = offerUseCase.execute("wl-1", slot);

        assertTrue(res.isSuccess());
        assertEquals(WaitlistStatus.OFFERED, res.getValue().entry().status());
        assertEquals(Optional.of(88_000L), res.getValue().entry().offeredAt());
        assertTrue(res.getValue().renderedMessage().contains("Sofía"));
        assertTrue(res.getValue().renderedMessage().contains("10:00"));
        assertEquals("5491122334455", res.getValue().clientPhone());
    }

    @Test
    public void assignWaitlistEntry_disponibleCreaTurnoYMarcaFulfilledYConConflictoNoCambia() {
        waitlistRepo.save(sampleEntry("wl-free"));
        FreedSlot freeSlot = new FreedSlot("pro-1", FRIDAY, 600, 660);

        Result<AssignResult> ok = assignUseCase.execute("wl-free", freeSlot);

        assertTrue(ok.isSuccess());
        assertEquals(AppointmentStatus.CONFIRMED, ok.getValue().appointment().status());
        assertEquals(WaitlistStatus.FULFILLED, ok.getValue().entry().status());
        assertEquals(1, appointmentRepo.store.size());

        // Otro turno ya ocupa 10:00-10:30: al intentar asignar otra entrada, falla con CONFLICT y queda ACTIVE
        waitlistRepo.save(sampleEntry("wl-busy"));
        Result<AssignResult> conflict = assignUseCase.execute("wl-busy", freeSlot);

        assertTrue(conflict.isFailure());
        assertEquals(DomainError.CONFLICT, conflict.getError());
        assertEquals(WaitlistStatus.ACTIVE, waitlistRepo.findById("wl-busy").get().status());
    }

    private static WaitlistEntry sampleEntry(String id) {
        return new WaitlistEntry.Builder()
                .id(id)
                .clientId("cli-1")
                .serviceIds(Collections.singletonList("svc-cut"))
                .professionalId("pro-1")
                .specificDay(FRIDAY)
                .fromMin(540)
                .toMin(780)
                .validUntilDay(FRIDAY + 5)
                .status(WaitlistStatus.ACTIVE)
                .createdAt(1000L)
                .build();
    }

    private static final class FakeWaitlistRepo implements WaitlistRepository {
        private final Map<String, WaitlistEntry> store = new LinkedHashMap<>();

        @Override
        public Optional<WaitlistEntry> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<WaitlistEntry> findByStatuses(Set<WaitlistStatus> statuses) {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<WaitlistEntry> findOpenByClientId(String clientId) {
            return Collections.emptyList();
        }

        @Override
        public void save(WaitlistEntry entry) {
            store.put(entry.id(), entry);
        }

        @Override
        public int expireBefore(long todayEpochDay) {
            return 0;
        }
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
            for (Appointment apt : store.values()) {
                if (apt.day() == dayEpoch && apt.professionalId().equals(professionalId)) {
                    out.add(apt);
                }
            }
            return out;
        }

        @Override
        public List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch) {
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
