package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaveProfessionalUseCaseTest {

    private FakeProfessionalRepository professionalRepo;
    private FakeServiceRepository serviceRepo;
    private SaveProfessionalUseCase saveUseCase;
    private SetProfessionalActiveUseCase setActiveUseCase;

    @Before
    public void setUp() {
        professionalRepo = new FakeProfessionalRepository();
        serviceRepo = new FakeServiceRepository();
        serviceRepo.save(new SalonService.Builder()
                .id("svc-1").name("Corte").category("Corte")
                .price(Money.ofCents(1_200_000L)).durationMin(30).active(true).build());
        serviceRepo.save(new SalonService.Builder()
                .id("svc-inactive").name("Antiguo").category("Corte")
                .price(Money.ofCents(900_000L)).durationMin(30).active(false).build());

        SettingsRepository settingsRepo = new SettingsRepository() {
            @Override
            public Settings getSettings() {
                return Settings.defaultSettings();
            }

            @Override
            public void saveSettings(Settings settings) {
            }
        };
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 1_700_000_000_000L;
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
        saveUseCase = new SaveProfessionalUseCase(
                professionalRepo, serviceRepo, settingsRepo, clock, () -> "pro-1");
        setActiveUseCase = new SetProfessionalActiveUseCase(professionalRepo, clock);
    }

    @Test
    public void execute_validProfessional_savesSchedulesServicesAndCommission() {
        Result<Professional> result = saveUseCase.execute(new SaveProfessionalUseCase.Request(
                null,
                "  Ana López ",
                "3511234567",
                "gray600",
                4_000,
                true,
                Collections.singletonList(new TimeRange(1, 540, 1140)),
                Collections.singleton("svc-1"),
                Collections.singleton(20_010L)
        ));

        assertTrue(result.isSuccess());
        Professional saved = result.getValue();
        assertEquals("pro-1", saved.id());
        assertEquals("Ana López", saved.name());
        assertEquals(4_000, saved.commissionBasisPoints());
        assertTrue(saved.daysOff().contains(20_010L));
    }

    @Test
    public void editProfessionalWithoutServices_addsFirstService() {
        Result<Professional> created = saveUseCase.execute(
                new SaveProfessionalUseCase.Request(
                        null,
                        "Ana López",
                        "",
                        "",
                        4_000,
                        true,
                        Collections.singletonList(new TimeRange(1, 540, 1140)),
                        Collections.emptySet(),
                        Collections.emptySet()));
        assertTrue(created.isSuccess());

        Result<Professional> edited = saveUseCase.execute(
                new SaveProfessionalUseCase.Request(
                        created.getValue().id(),
                        "Ana López",
                        "",
                        "",
                        4_000,
                        true,
                        created.getValue().workSchedules(),
                        Collections.singleton("svc-1"),
                        created.getValue().daysOff()));

        assertTrue(edited.isSuccess());
        assertTrue(edited.getValue().serviceIds().contains("svc-1"));
    }

    @Test
    public void execute_scheduleOutsideSalonOrInactiveService_failsValidation() {
        Result<Professional> outsideSalon = saveUseCase.execute(
                new SaveProfessionalUseCase.Request(
                        null, "Ana", "", "", 3_000, true,
                        Collections.singletonList(new TimeRange(7, 600, 900)),
                        Collections.singleton("svc-1"), Collections.emptySet()));
        assertEquals(DomainError.VALIDATION_ERROR, outsideSalon.getError());

        Result<Professional> inactiveSvc = saveUseCase.execute(
                new SaveProfessionalUseCase.Request(
                        null, "Ana", "", "", 3_000, true,
                        Collections.singletonList(new TimeRange(1, 600, 900)),
                        Collections.singleton("svc-inactive"), Collections.emptySet()));
        assertEquals(DomainError.VALIDATION_ERROR, inactiveSvc.getError());
    }

    @Test
    public void deactivate_whenProfessionalHasFutureAppointments_returnsConflict() {
        Professional saved = saveUseCase.execute(new SaveProfessionalUseCase.Request(
                null, "Ana", "", "", 4_000, true,
                Collections.singletonList(new TimeRange(1, 600, 900)),
                Collections.singleton("svc-1"), Collections.emptySet())).getValue();

        professionalRepo.futureAppointmentsFor.put(saved.id(), true);

        Result<Professional> deactivateViaToggle = setActiveUseCase.execute(saved.id(), false);
        assertFalse(deactivateViaToggle.isSuccess());
        assertEquals(DomainError.CONFLICT, deactivateViaToggle.getError());

        Result<Professional> deactivateViaEdit = saveUseCase.execute(
                new SaveProfessionalUseCase.Request(
                        saved.id(), "Ana", "", "", 4_000, false,
                        saved.workSchedules(), saved.serviceIds(), saved.daysOff()));
        assertEquals(DomainError.CONFLICT, deactivateViaEdit.getError());
    }

    private static final class FakeProfessionalRepository implements ProfessionalRepository {
        final Map<String, Professional> store = new LinkedHashMap<>();
        final Map<String, Boolean> futureAppointmentsFor = new LinkedHashMap<>();

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
            return Boolean.TRUE.equals(futureAppointmentsFor.get(professionalId));
        }
    }

    private static final class FakeServiceRepository implements SalonServiceRepository {
        final Map<String, SalonService> store = new LinkedHashMap<>();

        @Override
        public List<SalonService> findAll(boolean includeInactive) {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<SalonService> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<SalonService> findActiveByCategoryAndNormalizedName(
                String normalizedCategory, String normalizedName) {
            return Optional.empty();
        }

        @Override
        public void save(SalonService service) {
            store.put(service.id(), service);
        }
    }
}
