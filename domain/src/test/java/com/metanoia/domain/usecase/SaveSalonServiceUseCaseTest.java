package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.TextNormalizer;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaveSalonServiceUseCaseTest {

    private FakeServiceRepository serviceRepo;
    private SaveSalonServiceUseCase saveUseCase;
    private SetSalonServiceActiveUseCase setActiveUseCase;

    @Before
    public void setUp() {
        serviceRepo = new FakeServiceRepository();
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
                return 20_000L;
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
                return "svc-" + seq++;
            }
        };
        saveUseCase = new SaveSalonServiceUseCase(serviceRepo, settingsRepo, clock, ids);
        setActiveUseCase = new SetSalonServiceActiveUseCase(serviceRepo, clock);
    }

    @Test
    public void execute_validNewService_savesNormalizedAndColorFlag() {
        Result<SalonService> result = saveUseCase.execute(new SaveSalonServiceUseCase.Request(
                null, "  Balayage   ", "Mechas", Money.ofCents(4_500_000L), 120, true, true, 1));

        assertTrue(result.isSuccess());
        SalonService saved = result.getValue();
        assertEquals("svc-1", saved.id());
        assertEquals("Balayage", saved.name());
        assertTrue(saved.isColorService());
        assertTrue(saved.isActive());
    }

    @Test
    public void execute_duplicateActiveNameInSameCategory_returnsConflict() {
        saveUseCase.execute(new SaveSalonServiceUseCase.Request(
                null, "Tintura raíz", "Color", Money.ofCents(2_000_000L), 60, true, true, 0));

        Result<SalonService> duplicate = saveUseCase.execute(new SaveSalonServiceUseCase.Request(
                null, "tintura raiz", "COLOR", Money.ofCents(2_200_000L), 60, true, true, 0));

        assertFalse(duplicate.isSuccess());
        assertEquals(DomainError.CONFLICT, duplicate.getError());
    }

    @Test
    public void deactivateAndReuseName_allowsNewActiveServiceAndKeepsHistory() {
        SalonService first = saveUseCase.execute(new SaveSalonServiceUseCase.Request(
                null, "Corte clásico", "Corte", Money.ofCents(1_200_000L), 30, false, true, 0))
                .getValue();

        Result<SalonService> deactivated = setActiveUseCase.execute(first.id(), false);
        assertTrue(deactivated.isSuccess());
        assertFalse(deactivated.getValue().isActive());
        assertTrue(serviceRepo.findById(first.id()).isPresent());

        Result<SalonService> replacement = saveUseCase.execute(new SaveSalonServiceUseCase.Request(
                null, "Corte clásico", "Corte", Money.ofCents(1_500_000L), 30, false, true, 0));
        assertTrue(replacement.isSuccess());

        Result<SalonService> reactivateConflict = setActiveUseCase.execute(first.id(), true);
        assertEquals(DomainError.CONFLICT, reactivateConflict.getError());
    }

    @Test
    public void execute_invalidDurationNotMultipleOfSlot_failsValidation() {
        Result<SalonService> result = saveUseCase.execute(new SaveSalonServiceUseCase.Request(
                null, "Brushing", "Peinado", Money.ofCents(800_000L), 25, false, true, 0));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, result.getError());
    }

    private static final class FakeServiceRepository implements SalonServiceRepository {
        private final Map<String, SalonService> store = new LinkedHashMap<>();

        @Override
        public List<SalonService> findAll(boolean includeInactive) {
            List<SalonService> result = new ArrayList<>();
            for (SalonService service : store.values()) {
                if (includeInactive || service.isActive()) {
                    result.add(service);
                }
            }
            return result;
        }

        @Override
        public Optional<SalonService> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<SalonService> findActiveByCategoryAndNormalizedName(
                String normalizedCategory, String normalizedName) {
            for (SalonService service : store.values()) {
                if (service.isActive()
                        && TextNormalizer.normalize(service.category()).equals(normalizedCategory)
                        && TextNormalizer.normalize(service.name()).equals(normalizedName)) {
                    return Optional.of(service);
                }
            }
            return Optional.empty();
        }

        @Override
        public void save(SalonService service) {
            store.put(service.id(), service);
        }
    }
}
