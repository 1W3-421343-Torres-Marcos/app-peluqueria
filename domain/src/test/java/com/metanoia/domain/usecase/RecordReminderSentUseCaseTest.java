package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Reminder;
import com.metanoia.domain.model.ReminderType;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ReminderRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RecordReminderSentUseCaseTest {

    @Test
    public void appointmentReminder_avoidsDuplicatesAndSupportsUndo() {
        InMemoryReminderRepo repo = new InMemoryReminderRepo();
        List<AuditRecord> audits = new ArrayList<>();
        RecordReminderSentUseCase useCase = buildUseCase(repo, audits);

        Result<Reminder> first = useCase.recordAppointmentReminder("apt-1", "cli-1");
        assertTrue(first.isSuccess());
        assertEquals(ReminderType.APPOINTMENT, first.getValue().type());
        assertEquals(1, repo.map.size());
        assertEquals(1, audits.size());

        Result<Reminder> duplicate = useCase.recordAppointmentReminder("apt-1", "cli-1");
        assertTrue(duplicate.isSuccess());
        assertEquals(first.getValue().id(), duplicate.getValue().id());
        assertEquals(1, repo.map.size());
        assertEquals(1, audits.size());

        Result<Boolean> undone = useCase.undoReminder(first.getValue().id());
        assertTrue(undone.isSuccess());
        assertTrue(repo.map.isEmpty());
        assertEquals("REMINDER_UNDO", audits.get(1).action());
    }

    @Test
    public void birthdayGreeting_isUniquePerClientAndYear() {
        InMemoryReminderRepo repo = new InMemoryReminderRepo();
        List<AuditRecord> audits = new ArrayList<>();
        RecordReminderSentUseCase useCase = buildUseCase(repo, audits);

        Result<Reminder> y2026 = useCase.recordBirthdayGreeting("cli-1", 2026);
        assertTrue(y2026.isSuccess());
        assertEquals(Integer.valueOf(2026), y2026.getValue().year().orElse(null));

        Result<Reminder> duplicate2026 = useCase.recordBirthdayGreeting("cli-1", 2026);
        assertEquals(y2026.getValue().id(), duplicate2026.getValue().id());
        assertEquals(1, repo.map.size());

        Result<Reminder> y2027 = useCase.recordBirthdayGreeting("cli-1", 2027);
        assertTrue(y2027.isSuccess());
        assertEquals(2, repo.map.size());

        Result<Reminder> invalid = useCase.recordBirthdayGreeting("", 2026);
        assertFalse(invalid.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, invalid.getError());
    }

    private static RecordReminderSentUseCase buildUseCase(
            ReminderRepository repo, List<AuditRecord> audits) {
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 8_000L;
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
        SettingsRepository settingsRepo = new SettingsRepository() {
            @Override
            public Settings getSettings() {
                return Settings.defaultSettings();
            }

            @Override
            public void saveSettings(Settings settings) {
            }
        };
        AuditLogRepository auditRepo = audits::add;
        int[] seq = {0};
        IdGenerator ids = () -> "rem-" + (++seq[0]);
        return new RecordReminderSentUseCase(repo, settingsRepo, auditRepo, clock, ids);
    }

    private static final class InMemoryReminderRepo implements ReminderRepository {
        final Map<String, Reminder> map = new LinkedHashMap<>();

        @Override
        public Optional<Reminder> findAppointmentReminder(String appointmentId) {
            for (Reminder r : map.values()) {
                if (r.type() == ReminderType.APPOINTMENT
                        && appointmentId.equals(r.appointmentId().orElse(null))) {
                    return Optional.of(r);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<Reminder> findBirthdayReminder(String clientId, int year) {
            for (Reminder r : map.values()) {
                if (r.type() == ReminderType.BIRTHDAY
                        && clientId.equals(r.clientId())
                        && Integer.valueOf(year).equals(r.year().orElse(null))) {
                    return Optional.of(r);
                }
            }
            return Optional.empty();
        }

        @Override
        public void save(Reminder reminder) {
            map.put(reminder.id(), reminder);
        }

        @Override
        public void deleteById(String reminderId) {
            map.remove(reminderId);
        }
    }
}
