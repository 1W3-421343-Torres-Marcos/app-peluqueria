package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UpdateSettingsUseCaseTest {

    private FakeSettingsRepository settingsRepo;
    private FakeAuditLogRepository auditRepo;
    private UpdateSettingsUseCase useCase;

    @Before
    public void setUp() {
        settingsRepo = new FakeSettingsRepository(Settings.defaultSettings());
        auditRepo = new FakeAuditLogRepository();
        Clock fixedClock = new Clock() {
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
        IdGenerator fixedIds = () -> "audit-1";
        useCase = new UpdateSettingsUseCase(settingsRepo, auditRepo, fixedClock, fixedIds);
    }

    @Test
    public void execute_validChangeWithScheduleAndOperator_savesAndAudits() {
        Settings updated = Settings.defaultSettings().toBuilder()
                .businessName("  Metanoia Estudio  ")
                .slotMinutes(30)
                .operators(Arrays.asList("Dueño", "Sofía"))
                .currentOperator("Sofía")
                .generalSchedule(Collections.singletonList(new TimeRange(1, 600, 1140)))
                .build();

        Result<Settings> result = useCase.execute(updated);

        assertTrue(result.isSuccess());
        assertEquals("Metanoia Estudio", result.getValue().businessName());
        assertEquals(30, settingsRepo.getSettings().slotMinutes());
        assertEquals(1, auditRepo.records.size());
        assertEquals("Sofía", auditRepo.records.get(0).operator());
        assertEquals("SETTINGS", auditRepo.records.get(0).entityType());
    }

    @Test
    public void execute_businessDataOnlyChange_savesWithoutExtraAudit() {
        Settings updated = Settings.defaultSettings().toBuilder()
                .address("Av. Colón 1234")
                .phone("3515551234")
                .build();

        Result<Settings> result = useCase.execute(updated);

        assertTrue(result.isSuccess());
        assertEquals("Av. Colón 1234", settingsRepo.getSettings().address());
        assertEquals(0, auditRepo.records.size());
    }

    @Test
    public void execute_invalidSlotOrEmptyNameOrMissingOperator_failsValidation() {
        Settings invalidSlot = Settings.defaultSettings().toBuilder().slotMinutes(12).build();
        assertEquals(DomainError.VALIDATION_ERROR, useCase.execute(invalidSlot).getError());

        Settings shortName = Settings.defaultSettings().toBuilder().businessName("A").build();
        assertEquals(DomainError.VALIDATION_ERROR, useCase.execute(shortName).getError());

        Settings missingCurrentOp = Settings.defaultSettings().toBuilder()
                .operators(Collections.singletonList("Dueño"))
                .currentOperator("Otro")
                .build();
        Result<Settings> opResult = useCase.execute(missingCurrentOp);
        assertFalse(opResult.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, opResult.getError());
    }

    private static final class FakeSettingsRepository implements SettingsRepository {
        private Settings stored;

        FakeSettingsRepository(Settings initial) {
            this.stored = initial;
        }

        @Override
        public Settings getSettings() {
            return stored;
        }

        @Override
        public void saveSettings(Settings settings) {
            this.stored = settings;
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
