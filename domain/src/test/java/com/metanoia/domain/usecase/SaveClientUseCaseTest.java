package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaveClientUseCaseTest {

    private FakeClientRepository clientRepo;
    private FakeAuditLogRepository auditRepo;
    private SaveClientUseCase saveUseCase;
    private SetClientActiveUseCase setActiveUseCase;

    @Before
    public void setUp() {
        clientRepo = new FakeClientRepository();
        auditRepo = new FakeAuditLogRepository();
        SettingsRepository settingsRepo = new SettingsRepository() {
            private Settings settings = Settings.defaultSettings();

            @Override
            public Settings getSettings() {
                return settings;
            }

            @Override
            public void saveSettings(Settings settings) {
                this.settings = settings;
            }
        };
        Clock clock = new Clock() {
            private long millis = 1_700_000_000_000L;

            @Override
            public long nowMillis() {
                return millis++;
            }

            @Override
            public long todayEpochDay() {
                return 20_500L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 600;
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
                return "cli-" + seq++;
            }
        };
        saveUseCase = new SaveClientUseCase(clientRepo, settingsRepo, auditRepo, clock, ids);
        setActiveUseCase = new SetClientActiveUseCase(
                clientRepo, settingsRepo, auditRepo, clock, ids);
    }

    @Test
    public void execute_newClient_normalizesNamePhoneAndRecordsAudit() {
        Result<Client> result = saveUseCase.execute(new SaveClientUseCase.Request(
                null, "  María   José  ", "011 15-2233-4455", 14, 7, null, "Prefiere turno mañana", true));

        assertTrue(result.isSuccess());
        Client saved = result.getValue();
        assertEquals("María José", saved.name());
        assertEquals("maria jose", saved.nameNormalized());
        assertEquals("5491122334455", saved.phone());
        assertEquals(Integer.valueOf(14), saved.birthDay().orElseThrow());
        assertEquals(Integer.valueOf(7), saved.birthMonth().orElseThrow());
        assertFalse(saved.birthYear().isPresent());
        assertEquals(0, saved.noShowCount());
        assertEquals(1, auditRepo.records.size());
        assertEquals("CLIENT_CREATE", auditRepo.records.get(0).action());
    }

    @Test
    public void execute_duplicateActivePhone_returnsConflictWithExistingClientId() {
        Client first = saveUseCase.execute(new SaveClientUseCase.Request(
                null, "Sofía Gómez", "11 2233-4455", null, null, null, "", true)).getValue();

        Result<Client> duplicate = saveUseCase.execute(new SaveClientUseCase.Request(
                null, "Otra Sofía", "+54 9 11 2233-4455", null, null, null, "", true));

        assertFalse(duplicate.isSuccess());
        assertEquals(DomainError.CONFLICT, duplicate.getError());
        assertEquals(first.id(), duplicate.getErrorMessage());
    }

    @Test
    public void execute_editSameClientPreservesNoShowAndAllowsSamePhone() {
        Client initial = saveUseCase.execute(new SaveClientUseCase.Request(
                null, "Valentina", "11 4455-6677", null, null, null, "", true)).getValue();
        clientRepo.save(initial.toBuilder().noShowCount(2).build());

        Result<Client> edited = saveUseCase.execute(new SaveClientUseCase.Request(
                initial.id(), "Valentina López", "011 15-4455-6677", 3, 11, 1992, "Alérgica", true));

        assertTrue(edited.isSuccess());
        assertEquals(2, edited.getValue().noShowCount());
        assertEquals(initial.createdAt(), edited.getValue().createdAt());
        assertEquals("5491144556677", edited.getValue().phone());
    }

    @Test
    public void deactivateAndReusePhone_succeedsWhileKeepingOldRecord() {
        Client first = saveUseCase.execute(new SaveClientUseCase.Request(
                null, "Camila", "11 9988-7766", null, null, null, "", true)).getValue();

        Result<Client> deactivated = setActiveUseCase.execute(first.id(), false);
        assertTrue(deactivated.isSuccess());
        assertFalse(deactivated.getValue().isActive());

        Result<Client> second = saveUseCase.execute(new SaveClientUseCase.Request(
                null, "Camila Nueva", "11 9988-7766", null, null, null, "", true));
        assertTrue(second.isSuccess());

        Result<Client> reactivateConflict = setActiveUseCase.execute(first.id(), true);
        assertEquals(DomainError.CONFLICT, reactivateConflict.getError());
        assertEquals(second.getValue().id(), reactivateConflict.getErrorMessage());
    }

    private static final class FakeClientRepository implements ClientRepository {
        private final Map<String, Client> store = new LinkedHashMap<>();

        @Override
        public Optional<Client> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Client> findActiveByPhone(String normalizedPhone) {
            for (Client client : store.values()) {
                if (client.isActive() && client.phone().equals(normalizedPhone)) {
                    return Optional.of(client);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<ClientSummary> searchSummaries(String normalizedQuery, boolean includeInactive) {
            List<ClientSummary> out = new ArrayList<>();
            for (Client client : store.values()) {
                if ((includeInactive || client.isActive())
                        && client.nameNormalized().contains(normalizedQuery)) {
                    out.add(new ClientSummary(
                            client.id(), client.name(), client.phone(),
                            client.noShowCount(), client.isActive()));
                }
            }
            return out;
        }

        @Override
        public void save(Client client) {
            store.put(client.id(), client);
        }
    }

    private static final class FakeAuditLogRepository implements AuditLogRepository {
        final List<AuditRecord> records = new ArrayList<>();

        @Override
        public void record(AuditRecord record) {
            records.add(record);
        }
    }
}
