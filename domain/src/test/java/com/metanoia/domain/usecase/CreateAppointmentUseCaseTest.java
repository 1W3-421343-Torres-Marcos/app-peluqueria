package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.ColorComponentUnit;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ColorFormulaRules;

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
import java.util.Set;

public class CreateAppointmentUseCaseTest {

    private static final long MONDAY = LocalDate.of(2026, 11, 16).toEpochDay();

    private FakeAppointmentRepo appointmentRepo;
    private FakeServiceRepo serviceRepo;
    private FakeProRepo proRepo;
    private FakeColorFormulaRepo formulaRepo;
    private CreateAppointmentUseCase useCase;

    @Before
    public void setUp() {
        appointmentRepo = new FakeAppointmentRepo();
        serviceRepo = new FakeServiceRepo();
        proRepo = new FakeProRepo();

        FakeClientRepo clientRepo = new FakeClientRepo();
        clientRepo.save(new Client.Builder()
                .id("cli-1")
                .name("Sofía")
                .nameNormalized("sofia")
                .phone("5491122334455")
                .active(true)
                .build());

        serviceRepo.save(new SalonService.Builder()
                .id("svc-cut")
                .name("Corte")
                .category("Corte")
                .price(Money.ofCents(1_500_000L))
                .durationMin(30)
                .active(true)
                .build());
        serviceRepo.save(new SalonService.Builder()
                .id("svc-color")
                .name("Color")
                .category("Color")
                .price(Money.ofCents(3_000_000L))
                .durationMin(50)
                .colorService(true)
                .active(true)
                .build());
        serviceRepo.save(new SalonService.Builder()
                .id("svc-other")
                .name("Alisado")
                .category("Alisado")
                .price(Money.ofCents(5_000_000L))
                .durationMin(90)
                .active(true)
                .build());

        Set<String> enabled = new LinkedHashSet<>(Arrays.asList("svc-cut", "svc-color"));
        proRepo.save(new Professional.Builder()
                .id("pro-1")
                .name("Ana")
                .commissionBasisPoints(4000)
                .active(true)
                .workSchedules(Collections.singletonList(new TimeRange(1, 540, 1140)))
                .serviceIds(enabled)
                .build());

        SettingsRepository settingsRepo = new SettingsRepository() {
            @Override
            public Settings getSettings() {
                return Settings.defaultSettings(); // block 15 min, L-S 09:00-20:00
            }

            @Override
            public void saveSettings(Settings settings) {
            }
        };
        AuditLogRepository auditRepo = record -> { };
        Clock clock = new Clock() {
            private long ms = 1000L;

            @Override
            public long nowMillis() {
                return ms++;
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
        IdGenerator ids = new IdGenerator() {
            private int seq = 1;

            @Override
            public String nextId() {
                return "id-" + seq++;
            }
        };
        ScheduleBlockRepository blockRepo = new ScheduleBlockRepository() {
            @Override
            public Optional<ScheduleBlock> findById(String id) {
                return Optional.empty();
            }

            @Override
            public List<ScheduleBlock> findActiveOverlappingRange(long startDay, long endDay) {
                return Collections.emptyList();
            }

            @Override
            public void save(ScheduleBlock block) {
            }
        };

        formulaRepo = new FakeColorFormulaRepo();
        useCase = new CreateAppointmentUseCase(
                appointmentRepo, clientRepo, proRepo, serviceRepo,
                blockRepo, settingsRepo, auditRepo, formulaRepo, clock, ids);
    }

    @Test
    public void execute_multipleServices_freezesPricesAndKeepsSnapshotAfterCatalogChange() {
        Result<Appointment> result = useCase.execute(new CreateAppointmentUseCase.Request(
                "cli-1", "pro-1", MONDAY, 600,
                Arrays.asList("svc-cut", "svc-color"), "Trae muestra", false));

        assertTrue(result.isSuccess());
        Appointment saved = result.getValue();
        // 30 + 50 = 80 min -> redondeado a múltiplo de 15 min = 90 min (600 + 90 = 690)
        assertEquals(690, saved.endMin());
        assertEquals(AppointmentStatus.PENDING, saved.status());
        assertEquals(Money.ofCents(4_500_000L), saved.totalAmount());

        // Cambiar el catálogo no altera el turno ya guardado
        serviceRepo.save(serviceRepo.findById("svc-cut").orElseThrow().toBuilder()
                .price(Money.ofCents(2_500_000L))
                .durationMin(45)
                .build());
        Appointment stored = appointmentRepo.findById(saved.id()).orElseThrow();
        assertEquals(Money.ofCents(1_500_000L), stored.items().get(0).priceSnapshot());
        assertEquals(30, stored.items().get(0).durationMinSnapshot());
    }

    @Test
    public void execute_serviceNotEnabledForProfessional_failsValidation() {
        Result<Appointment> result = useCase.execute(new CreateAppointmentUseCase.Request(
                "cli-1", "pro-1", MONDAY, 600,
                Collections.singletonList("svc-other"), "", false));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, result.getError());
    }

    @Test
    public void execute_overlappingSlot_isAllowed() {
        useCase.execute(new CreateAppointmentUseCase.Request(
                "cli-1", "pro-1", MONDAY, 600,
                Collections.singletonList("svc-cut"), "", true));

        Result<Appointment> overlap = useCase.execute(new CreateAppointmentUseCase.Request(
                "cli-1", "pro-1", MONDAY, 615,
                Collections.singletonList("svc-cut"), "", false));

        assertTrue(overlap.isSuccess());
    }

    @Test
    public void execute_conRepetirUltimaFormulaEnServicioDeColor_duplicaYVincularFormulaAlItem() {
        ColorFormula previous = new ColorFormula.Builder()
                .id("cf-prev")
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(MONDAY - 30)
                .exposureMin(35)
                .developerVolume("20 vol")
                .notes("Formula base")
                .createdAt(100L)
                .components(Collections.singletonList(new ColorFormulaComponent.Builder()
                        .id("cmp-prev")
                        .formulaId("cf-prev")
                        .brand("Wella")
                        .shade("7.1")
                        .amountTenths(300)
                        .unit(ColorComponentUnit.GRAMS)
                        .position(0)
                        .build()))
                .build();
        formulaRepo.save(previous);

        Result<Appointment> result = useCase.execute(new CreateAppointmentUseCase.Request(
                "cli-1", "pro-1", MONDAY, 600,
                Arrays.asList("svc-cut", "svc-color"), "Repetir mezcla", false, true));

        assertTrue(result.isSuccess());
        Appointment saved = result.getValue();
        assertFalse(saved.items().get(0).colorFormulaId().isPresent());
        assertTrue(saved.items().get(1).colorFormulaId().isPresent());
        String copiedFormulaId = saved.items().get(1).colorFormulaId().get();
        assertFalse("cf-prev".equals(copiedFormulaId));

        ColorFormula copied = formulaRepo.findById(copiedFormulaId).orElseThrow();
        assertEquals(Optional.of(saved.id()), copied.appointmentId());
        assertEquals(MONDAY, copied.day());
        assertEquals("7.1", copied.components().get(0).shade());
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
        public List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findFutureActiveFromSeries(String ruleId, long fromDay, int min) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasFutureActiveForProfessional(String professionalId, long fromEpochDay) {
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
        }
    }

    private static final class FakeClientRepo implements ClientRepository {
        private final Map<String, Client> store = new LinkedHashMap<>();

        @Override
        public Optional<Client> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Client> findActiveByPhone(String normalizedPhone) {
            return Optional.empty();
        }

        @Override
        public List<ClientSummary> searchSummaries(String normalizedQuery, boolean includeInactive) {
            return Collections.emptyList();
        }

        @Override
        public void save(Client client) {
            store.put(client.id(), client);
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

    private static final class FakeProRepo implements ProfessionalRepository {
        private final Map<String, Professional> store = new LinkedHashMap<>();

        @Override
        public List<Professional> findAll(boolean includeInactive) {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<Professional> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public void save(Professional professional) {
            store.put(professional.id(), professional);
        }

        @Override
        public boolean hasFutureActiveAppointments(String professionalId, long fromEpochDay) {
            return false;
        }
    }

    private static final class FakeColorFormulaRepo implements ColorFormulaRepository {
        private final Map<String, ColorFormula> store = new LinkedHashMap<>();

        @Override
        public Optional<ColorFormula> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<ColorFormula> findActiveByClientId(String clientId) {
            List<ColorFormula> out = new ArrayList<>();
            for (ColorFormula f : store.values()) {
                if (clientId.equals(f.clientId()) && !f.isDeleted()) {
                    out.add(f);
                }
            }
            return out;
        }

        @Override
        public Optional<ColorFormula> findLatestActiveByClientId(String clientId) {
            return ColorFormulaRules.selectLatestActiveFormula(findActiveByClientId(clientId));
        }

        @Override
        public boolean isLinkedToClosedAppointment(String formulaId) {
            return false;
        }

        @Override
        public void save(ColorFormula formula) {
            store.put(formula.id(), formula);
        }

        @Override
        public void softDelete(String formulaId, long deletedAtMillis) {
        }
    }
}
