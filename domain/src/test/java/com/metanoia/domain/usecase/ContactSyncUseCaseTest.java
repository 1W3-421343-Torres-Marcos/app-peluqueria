package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientOrigin;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.ContactImportPreview;
import com.metanoia.domain.model.ContactPhone;
import com.metanoia.domain.model.ContactSyncOutcome;
import com.metanoia.domain.model.DeviceContactCandidate;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContactSyncUseCaseTest {

    private InMemoryClientRepo repo;
    private PreviewContactImportUseCase previewUseCase;
    private SyncContactsUseCase syncUseCase;

    @Before
    public void setUp() {
        repo = new InMemoryClientRepo();
        Clock clock = new Clock() {
            private long now = 1_760_000_000_000L;

            @Override
            public long nowMillis() {
                return now;
            }

            @Override
            public long todayEpochDay() {
                return 20_776L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 700;
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
                return "sync-id-" + seq++;
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
        previewUseCase = new PreviewContactImportUseCase(repo, clock, ids);
        syncUseCase = new SyncContactsUseCase(repo, settingsRepo, clock, ids);
    }

    @Test
    public void previewAndImport_deduplicatesPhonesAccountsAndSkipsInvalid() {
        repo.save(new Client.Builder()
                .id("manual-1")
                .name("Sofía Original")
                .nameNormalized("sofia original")
                .phone("5491122334455")
                .birthDay(10)
                .birthMonth(4)
                .notes("Alergia al amoníaco")
                .origin(ClientOrigin.MANUAL)
                .active(true)
                .createdAt(100L)
                .updatedAt(100L)
                .build());

        List<DeviceContactCandidate> device = Arrays.asList(
                new DeviceContactCandidate("lk-sofia", "Sofía Editada",
                        Collections.singletonList(new ContactPhone("011 15-2233-4455", true)),
                        "1990-01-01"),
                new DeviceContactCandidate("lk-lucia", "Lucía Gómez",
                        Arrays.asList(
                                new ContactPhone("0351 422-1111", false),
                                new ContactPhone("0351 15-6677889", true)),
                        "--09-15"),
                new DeviceContactCandidate("lk-lucia-sim", "",
                        Collections.singletonList(new ContactPhone("+54 9 351 6677889", true)),
                        null),
                new DeviceContactCandidate("lk-sin-nombre", " ",
                        Collections.singletonList(new ContactPhone("11 9988-7766", true)),
                        null),
                new DeviceContactCandidate("lk-sin-tel", "Sin Teléfono",
                        Collections.singletonList(new ContactPhone("123", true)),
                        null)
        );

        Result<ContactImportPreview> previewResult = previewUseCase.execute(device);
        assertTrue(previewResult.isSuccess());
        ContactImportPreview preview = previewResult.getValue();
        assertEquals(2, preview.toImportCount());
        assertEquals(1, preview.alreadyLoadedCount());
        assertEquals(1, preview.skippedCount());

        Result<ContactSyncOutcome> syncResult = syncUseCase.execute(
                device, SyncContactsUseCase.ACTION_BULK_IMPORT);
        assertTrue(syncResult.isSuccess());
        assertEquals(2, syncResult.getValue().importedCount());
        assertEquals(3, repo.all.size());

        Client sofiaAfter = repo.findById("manual-1").get();
        assertEquals("Sofía Original", sofiaAfter.name());
        assertEquals("Alergia al amoníaco", sofiaAfter.notes());
        assertEquals(Integer.valueOf(10), sofiaAfter.birthDay().orElse(null));

        Client lucia = repo.findActiveByPhone("5493516677889").get();
        assertEquals("Lucía Gómez", lucia.name());
        assertEquals(ClientOrigin.CONTACT_SYNC, lucia.origin());
        assertEquals("lk-lucia", lucia.contactLookupKey().orElse(""));
        assertEquals(Integer.valueOf(15), lucia.birthDay().orElse(null));
        assertEquals(Integer.valueOf(9), lucia.birthMonth().orElse(null));
        assertFalse(lucia.birthYear().isPresent());
        assertTrue(lucia.createdFromContactAt().isPresent());

        Client unnamed = repo.findActiveByPhone("5491199887766").get();
        assertEquals("5491199887766", unnamed.name());

        assertEquals(1, repo.audits.size());
        String auditConcept = repo.audits.get(0).concept();
        assertTrue(auditConcept.contains("2"));
        assertFalse(auditConcept.contains("Lucía"));
        assertFalse(auditConcept.contains("5493516677889"));
    }

    @Test
    public void sync_isIdempotentRespectsInactiveDeletesNothingAndTracksLookupKey() {
        repo.save(new Client.Builder()
                .id("inactive-1")
                .name("Contacto Baja")
                .nameNormalized("contacto baja")
                .phone("5491144445555")
                .origin(ClientOrigin.CONTACT_SYNC)
                .active(false)
                .createdAt(100L)
                .updatedAt(100L)
                .build());
        repo.save(new Client.Builder()
                .id("missing-from-phone")
                .name("Cliente Historico")
                .nameNormalized("cliente historico")
                .phone("5491188887777")
                .origin(ClientOrigin.MANUAL)
                .active(true)
                .createdAt(100L)
                .updatedAt(100L)
                .build());

        List<DeviceContactCandidate> firstRun = Arrays.asList(
                new DeviceContactCandidate("lk-baja", "Contacto Baja",
                        Collections.singletonList(new ContactPhone("11 4444-5555", true)), null),
                new DeviceContactCandidate("lk-carla", "Carla Perez",
                        Collections.singletonList(new ContactPhone("11 3333-2222", true)), null)
        );

        Result<ContactSyncOutcome> first = syncUseCase.execute(
                firstRun, SyncContactsUseCase.ACTION_AUTO_SYNC);
        assertEquals(1, first.getValue().importedCount());
        assertEquals(1, first.getValue().alreadyLoadedCount());
        assertFalse(repo.findById("inactive-1").get().isActive());
        assertTrue(repo.findById("missing-from-phone").get().isActive());

        // Misma corrida y luego cambio de nombre + teléfono manteniendo el mismo lookupKey
        List<DeviceContactCandidate> secondRun = Arrays.asList(
                new DeviceContactCandidate("lk-baja", "Contacto Baja",
                        Collections.singletonList(new ContactPhone("+54 9 11 4444-5555", true)), null),
                new DeviceContactCandidate("lk-carla", "Carla Nuevo Nombre",
                        Collections.singletonList(new ContactPhone("11 9999-0000", true)), "1995-03-04")
        );
        Result<ContactSyncOutcome> second = syncUseCase.execute(
                secondRun, SyncContactsUseCase.ACTION_AUTO_SYNC);
        assertEquals(0, second.getValue().importedCount());
        assertEquals(2, second.getValue().alreadyLoadedCount());
        assertEquals(1, repo.audits.size());

        Client carla = repo.findActiveByPhone("5491133332222").get();
        assertEquals("Carla Perez", carla.name());
        assertFalse(carla.birthDay().isPresent());
    }

    @Test
    public void sync_truncatesLongNameOverEightyCharacters() {
        String longName = "Nombre Muy Largo Que Supera Claramente Los Ochenta Caracteres Permitidos En Clientes Del Salon";
        Result<ContactSyncOutcome> result = syncUseCase.execute(
                Collections.singletonList(new DeviceContactCandidate(
                        "lk-long", longName,
                        Collections.singletonList(new ContactPhone("11 5555-4444", true)), null)),
                SyncContactsUseCase.ACTION_AUTO_SYNC);
        assertEquals(1, result.getValue().importedCount());
        Client saved = repo.findActiveByPhone("5491155554444").get();
        assertTrue(saved.name().length() <= 80);
    }

    private static final class InMemoryClientRepo implements ClientRepository {
        final Map<String, Client> all = new LinkedHashMap<>();
        final List<AuditRecord> audits = new ArrayList<>();

        @Override
        public Optional<Client> findById(String id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public Optional<Client> findActiveByPhone(String normalizedPhone) {
            for (Client client : all.values()) {
                if (client.isActive() && client.phone().equals(normalizedPhone)) {
                    return Optional.of(client);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<ClientSummary> searchSummaries(String normalizedQuery, boolean includeInactive) {
            return Collections.emptyList();
        }

        @Override
        public void save(Client client) {
            all.put(client.id(), client);
        }

        @Override
        public List<Client> findAllForContactSync() {
            return new ArrayList<>(all.values());
        }

        @Override
        public void saveAllNewClientsWithAudit(List<Client> newClients, AuditRecord auditRecord) {
            for (Client c : newClients) {
                all.put(c.id(), c);
            }
            audits.add(auditRecord);
        }
    }
}
