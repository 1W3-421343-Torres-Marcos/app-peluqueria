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
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.ColorComponentUnit;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.ColorFormulaRules;
import com.metanoia.domain.rules.ColorFormulaRules.ComponentDraft;

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
import java.util.concurrent.atomic.AtomicInteger;

public class SaveColorFormulaUseCaseTest {

    private static final long TODAY = LocalDate.of(2026, 11, 18).toEpochDay();

    private FakeColorFormulaRepo formulaRepo;
    private FakeAppointmentRepo appointmentRepo;
    private List<AuditRecord> auditLogs;
    private long nowMillis = 5000L;
    private SaveColorFormulaUseCase saveUseCase;
    private DuplicateColorFormulaUseCase duplicateUseCase;
    private DeleteColorFormulaUseCase deleteUseCase;

    @Before
    public void setUp() {
        formulaRepo = new FakeColorFormulaRepo();
        appointmentRepo = new FakeAppointmentRepo();
        auditLogs = new ArrayList<>();

        ClientRepository clientRepo = new ClientRepository() {
            private final Client cli = new Client.Builder()
                    .id("cli-1").name("Sofía").nameNormalized("sofia")
                    .phone("5491122334455").active(true).build();

            @Override
            public Optional<Client> findById(String id) {
                return "cli-1".equals(id) ? Optional.of(cli) : Optional.empty();
            }

            @Override
            public Optional<Client> findActiveByPhone(String phone) {
                return Optional.empty();
            }

            @Override
            public List<ClientSummary> searchSummaries(String query, boolean includeInactive) {
                return Collections.emptyList();
            }

            @Override
            public void save(Client client) {
            }
        };

        ProfessionalRepository proRepo = new ProfessionalRepository() {
            private final Professional pro = new Professional.Builder()
                    .id("pro-1").name("Ana").active(true).build();

            @Override
            public List<Professional> findAll(boolean includeInactive) {
                return Collections.singletonList(pro);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(pro) : Optional.empty();
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

        AuditLogRepository auditRepo = auditLogs::add;
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return nowMillis;
            }

            @Override
            public long todayEpochDay() {
                return TODAY;
            }

            @Override
            public int currentMinuteOfDay() {
                return 600;
            }

            @Override
            public ZoneId zoneId() {
                return Clock.ARGENTINA_ZONE;
            }
        };
        AtomicInteger seq = new AtomicInteger(1);
        IdGenerator ids = () -> "gen-" + seq.getAndIncrement();

        saveUseCase = new SaveColorFormulaUseCase(
                formulaRepo, clientRepo, proRepo, appointmentRepo, settingsRepo,
                auditRepo, clock, ids);
        duplicateUseCase = new DuplicateColorFormulaUseCase(
                formulaRepo, settingsRepo, auditRepo, clock, ids);
        deleteUseCase = new DeleteColorFormulaUseCase(
                formulaRepo, appointmentRepo, settingsRepo, auditRepo, clock, ids);
    }

    @Test
    public void execute_altaYEdicionBorrador_guardaYAutorizaCambios() {
        Result<ColorFormula> created = saveUseCase.execute(new SaveColorFormulaUseCase.Request(
                null,
                "cli-1",
                null,
                "pro-1",
                TODAY - 5,
                35,
                "20 vol",
                "Raíces + largos",
                "Tono parejo",
                Collections.singletonList(
                        new ComponentDraft("Wella", "Koleston", "7.1", 600, ColorComponentUnit.GRAMS))
        ));

        assertTrue(created.isSuccess());
        ColorFormula formula = created.getValue();
        assertEquals("7.1 (60 g)", formula.mainShadesSummary());

        Result<ColorFormula> updated = saveUseCase.execute(new SaveColorFormulaUseCase.Request(
                formula.id(),
                "cli-1",
                null,
                "pro-1",
                TODAY - 5,
                40,
                "30 vol",
                "Ajuste tiempo",
                "Brillo óptimo",
                Collections.singletonList(
                        new ComponentDraft("Wella", "Koleston", "8.1", 450, ColorComponentUnit.GRAMS))
        ));

        assertTrue(updated.isSuccess());
        assertEquals(40, updated.getValue().exposureMin());
        assertEquals("8.1 (45 g)", updated.getValue().mainShadesSummary());
    }

    @Test
    public void execute_formulaEnTurnoCompletadoOPagado_bloqueaEdicionYPermiteDuplicar() {
        Appointment closedApt = new Appointment.Builder()
                .id("apt-paid")
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(TODAY - 1)
                .startMin(600)
                .endMin(660)
                .status(AppointmentStatus.PAID)
                .items(Collections.singletonList(new AppointmentItem.Builder()
                        .id("item-1")
                        .appointmentId("apt-paid")
                        .serviceId("svc-color")
                        .serviceNameSnapshot("Color")
                        .priceSnapshot(Money.ofCents(3_000_000L))
                        .durationMinSnapshot(60)
                        .position(0)
                        .build()))
                .build();
        appointmentRepo.save(closedApt);

        Result<ColorFormula> created = saveUseCase.execute(new SaveColorFormulaUseCase.Request(
                null,
                "cli-1",
                "apt-paid",
                "pro-1",
                TODAY - 1,
                35,
                "20 vol",
                "",
                null,
                Collections.singletonList(
                        new ComponentDraft("L'Oréal", null, "7.0", 300, ColorComponentUnit.GRAMS))
        ));
        assertTrue(created.isSuccess());

        Result<ColorFormula> blockedEdit = saveUseCase.execute(new SaveColorFormulaUseCase.Request(
                created.getValue().id(),
                "cli-1",
                "apt-paid",
                "pro-1",
                TODAY - 1,
                45,
                "30 vol",
                "Intento editar cerrada",
                null,
                Collections.singletonList(
                        new ComponentDraft("L'Oréal", null, "8.0", 300, ColorComponentUnit.GRAMS))
        ));
        assertTrue(blockedEdit.isFailure());
        assertEquals(DomainError.CONFLICT, blockedEdit.getError());

        Result<Boolean> blockedDelete = deleteUseCase.execute(created.getValue().id());
        assertTrue(blockedDelete.isFailure());
        assertEquals(DomainError.CONFLICT, blockedDelete.getError());

        nowMillis = 9000L;
        Result<ColorFormula> duplicated = duplicateUseCase.execute(
                new DuplicateColorFormulaUseCase.Request(created.getValue().id()));
        assertTrue(duplicated.isSuccess());
        ColorFormula copy = duplicated.getValue();
        assertFalse(copy.appointmentId().isPresent());
        assertEquals(TODAY, copy.day());
        assertEquals(created.getValue().components().get(0).shade(),
                copy.components().get(0).shade());
    }

    @Test
    public void deleteColorFormula_formulaEditable_marcaDeletedAtYOcultaEnActivas() {
        ColorFormula formula = saveUseCase.execute(new SaveColorFormulaUseCase.Request(
                null,
                "cli-1",
                null,
                null,
                TODAY,
                30,
                null,
                "",
                null,
                Collections.singletonList(
                        new ComponentDraft("Igora", null, "6.1", 500, ColorComponentUnit.GRAMS))
        )).getValue();

        nowMillis = 12_345L;
        Result<Boolean> deleted = deleteUseCase.execute(formula.id());
        assertTrue(deleted.isSuccess());
        assertTrue(formulaRepo.findActiveByClientId("cli-1").isEmpty());
        assertTrue(formulaRepo.findById(formula.id()).get().isDeleted());
    }

    private static final class FakeColorFormulaRepo implements ColorFormulaRepository {
        private final Map<String, ColorFormula> storage = new LinkedHashMap<>();

        @Override
        public Optional<ColorFormula> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<ColorFormula> findActiveByClientId(String clientId) {
            List<ColorFormula> out = new ArrayList<>();
            for (ColorFormula f : storage.values()) {
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
            storage.put(formula.id(), formula);
        }

        @Override
        public void softDelete(String formulaId, long deletedAtMillis) {
            ColorFormula existing = storage.get(formulaId);
            if (existing != null) {
                storage.put(formulaId, existing.toBuilder().deletedAt(deletedAtMillis).build());
            }
        }
    }

    private static final class FakeAppointmentRepo implements AppointmentRepository {
        private final Map<String, Appointment> storage = new LinkedHashMap<>();

        @Override
        public Optional<Appointment> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findFutureActiveFromSeries(String r, long d, int s) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasFutureActiveForProfessional(String p, long d) {
            return false;
        }

        @Override
        public void save(Appointment appointment) {
            storage.put(appointment.id(), appointment);
        }

        @Override
        public void saveAll(List<Appointment> appointments) {
        }

        @Override
        public void saveRecurrenceRule(RecurrenceRule rule) {
        }
    }
}
