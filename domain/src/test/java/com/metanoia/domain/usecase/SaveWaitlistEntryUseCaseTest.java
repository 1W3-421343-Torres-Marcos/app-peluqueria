package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;

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
import java.util.concurrent.atomic.AtomicInteger;

public class SaveWaitlistEntryUseCaseTest {

    private static final long TODAY = LocalDate.of(2026, 11, 18).toEpochDay();

    private FakeWaitlistRepo waitlistRepo;
    private List<AuditRecord> auditLogs;
    private long currentToday = TODAY;
    private SaveWaitlistEntryUseCase saveUseCase;
    private CancelWaitlistEntryUseCase cancelUseCase;
    private ExpireWaitlistEntriesUseCase expireUseCase;

    @Before
    public void setUp() {
        waitlistRepo = new FakeWaitlistRepo();
        auditLogs = new ArrayList<>();

        ClientRepository clientRepo = new ClientRepository() {
            private final Client activeClient = new Client.Builder()
                    .id("cli-1").name("Sofía").nameNormalized("sofia")
                    .phone("5491122334455").active(true).build();

            @Override
            public Optional<Client> findById(String id) {
                return "cli-1".equals(id) ? Optional.of(activeClient) : Optional.empty();
            }

            @Override
            public Optional<Client> findActiveByPhone(String phone) {
                return Optional.empty();
            }

            @Override
            public List<ClientSummary> searchSummaries(String q, boolean includeInactive) {
                return Collections.emptyList();
            }

            @Override
            public void save(Client client) {
            }
        };

        SalonServiceRepository serviceRepo = new SalonServiceRepository() {
            private final SalonService cut = new SalonService.Builder()
                    .id("svc-cut").name("Corte").category("Corte")
                    .price(Money.ofCents(1_500_000L)).durationMin(30).active(true).build();
            private final SalonService color = new SalonService.Builder()
                    .id("svc-color").name("Color").category("Color")
                    .price(Money.ofCents(3_000_000L)).durationMin(60)
                    .colorService(true).active(true).build();

            @Override
            public List<SalonService> findAll(boolean includeInactive) {
                return Arrays.asList(cut, color);
            }

            @Override
            public Optional<SalonService> findById(String id) {
                if ("svc-cut".equals(id)) {
                    return Optional.of(cut);
                }
                if ("svc-color".equals(id)) {
                    return Optional.of(color);
                }
                return Optional.empty();
            }

            @Override
            public Optional<SalonService> findActiveByCategoryAndNormalizedName(String c, String n) {
                return Optional.empty();
            }

            @Override
            public void save(SalonService service) {
            }
        };

        ProfessionalRepository proRepo = new ProfessionalRepository() {
            private final Professional proCutOnly = new Professional.Builder()
                    .id("pro-1").name("Ana").active(true)
                    .serviceIds(Collections.singleton("svc-cut")).build();

            @Override
            public List<Professional> findAll(boolean includeInactive) {
                return Collections.singletonList(proCutOnly);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(proCutOnly) : Optional.empty();
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
            private long ms = 10_000L;

            @Override
            public long nowMillis() {
                return ms++;
            }

            @Override
            public long todayEpochDay() {
                return currentToday;
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
        IdGenerator ids = () -> "wl-" + seq.getAndIncrement();

        saveUseCase = new SaveWaitlistEntryUseCase(
                waitlistRepo, clientRepo, serviceRepo, proRepo,
                settingsRepo, auditRepo, clock, ids);
        cancelUseCase = new CancelWaitlistEntryUseCase(
                waitlistRepo, settingsRepo, auditRepo, clock, ids);
        expireUseCase = new ExpireWaitlistEntriesUseCase(waitlistRepo, clock);
    }

    @Test
    public void execute_validacionesYServicioNoHabilitadoParaProfesional_fallan() {
        Result<WaitlistEntry> badRange = saveUseCase.execute(new SaveWaitlistEntryUseCase.Request(
                null, "cli-1", Collections.singletonList("svc-cut"), null,
                TODAY + 1, Collections.emptySet(), 700, 600, null, ""));
        assertTrue(badRange.isFailure());
        assertEquals(DomainError.VALIDATION_ERROR, badRange.getError());

        Result<WaitlistEntry> unsupportedPro = saveUseCase.execute(
                new SaveWaitlistEntryUseCase.Request(
                        null, "cli-1", Arrays.asList("svc-cut", "svc-color"), "pro-1",
                        TODAY + 1, Collections.emptySet(), 540, 720, null, ""));
        assertTrue(unsupportedPro.isFailure());
        assertEquals(DomainError.VALIDATION_ERROR, unsupportedPro.getError());
    }

    @Test
    public void execute_duplicadoMismaPreferencia_rechazaConflictYPermiteDistintaPreferencia() {
        Set<Integer> fridaySat = new LinkedHashSet<>(Arrays.asList(5, 6));
        Result<WaitlistEntry> first = saveUseCase.execute(new SaveWaitlistEntryUseCase.Request(
                null, "cli-1", Arrays.asList("svc-cut", "svc-color"), null,
                null, fridaySat, 600, 900, null, "Prefiere tarde"));
        assertTrue(first.isSuccess());
        assertEquals(TODAY + Settings.DEFAULT_WAITLIST_EXPIRY_DAYS,
                first.getValue().validUntilDay());

        Result<WaitlistEntry> duplicate = saveUseCase.execute(new SaveWaitlistEntryUseCase.Request(
                null, "cli-1", Arrays.asList("svc-color", "svc-cut"), null,
                null, fridaySat, 600, 900, null, "Otro texto"));
        assertTrue(duplicate.isFailure());
        assertEquals(DomainError.CONFLICT, duplicate.getError());

        Result<WaitlistEntry> differentWindow = saveUseCase.execute(
                new SaveWaitlistEntryUseCase.Request(
                        null, "cli-1", Collections.singletonList("svc-cut"), "pro-1",
                        TODAY + 2, Collections.emptySet(), 900, 1080, null, ""));
        assertTrue(differentWindow.isSuccess());
    }

    @Test
    public void execute_edicionCancelacionYVencimientoAutomatico_actualizanEstados() {
        Result<WaitlistEntry> created = saveUseCase.execute(new SaveWaitlistEntryUseCase.Request(
                null, "cli-1", Collections.singletonList("svc-cut"), null,
                TODAY + 1, Collections.emptySet(), 540, 720, TODAY + 3, "Nota inicial"));
        assertTrue(created.isSuccess());

        Result<WaitlistEntry> edited = saveUseCase.execute(new SaveWaitlistEntryUseCase.Request(
                created.getValue().id(), "cli-1", Collections.singletonList("svc-cut"), "pro-1",
                TODAY + 1, Collections.emptySet(), 600, 780, TODAY + 3, "Con Ana"));
        assertTrue(edited.isSuccess());
        assertEquals(Optional.of("pro-1"), edited.getValue().professionalId());

        Result<WaitlistEntry> second = saveUseCase.execute(new SaveWaitlistEntryUseCase.Request(
                null, "cli-1", Collections.singletonList("svc-color"), null,
                TODAY + 2, Collections.emptySet(), 540, 840, TODAY + 5, ""));
        assertTrue(second.isSuccess());

        Result<WaitlistEntry> cancelled = cancelUseCase.execute(second.getValue().id());
        assertTrue(cancelled.isSuccess());
        assertEquals(WaitlistStatus.CANCELLED, cancelled.getValue().status());

        currentToday = TODAY + 4;
        Result<Integer> expired = expireUseCase.execute();
        assertTrue(expired.isSuccess());
        assertEquals(Integer.valueOf(1), expired.getValue());
        assertEquals(WaitlistStatus.EXPIRED,
                waitlistRepo.findById(created.getValue().id()).get().status());
        assertEquals(WaitlistStatus.CANCELLED,
                waitlistRepo.findById(second.getValue().id()).get().status());
    }

    private static final class FakeWaitlistRepo implements WaitlistRepository {
        private final Map<String, WaitlistEntry> store = new LinkedHashMap<>();

        @Override
        public Optional<WaitlistEntry> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<WaitlistEntry> findByStatuses(Set<WaitlistStatus> statuses) {
            List<WaitlistEntry> out = new ArrayList<>();
            for (WaitlistEntry entry : store.values()) {
                if (statuses.contains(entry.status())) {
                    out.add(entry);
                }
            }
            return out;
        }

        @Override
        public List<WaitlistEntry> findOpenByClientId(String clientId) {
            List<WaitlistEntry> out = new ArrayList<>();
            for (WaitlistEntry entry : store.values()) {
                if (clientId.equals(entry.clientId()) && entry.status().isOpen()) {
                    out.add(entry);
                }
            }
            return out;
        }

        @Override
        public void save(WaitlistEntry entry) {
            store.put(entry.id(), entry);
        }

        @Override
        public int expireBefore(long todayEpochDay) {
            int count = 0;
            for (Map.Entry<String, WaitlistEntry> pair : store.entrySet()) {
                WaitlistEntry entry = pair.getValue();
                if (entry.status().isOpen() && entry.validUntilDay() < todayEpochDay) {
                    pair.setValue(entry.toBuilder().status(WaitlistStatus.EXPIRED).build());
                    count++;
                }
            }
            return count;
        }
    }
}
