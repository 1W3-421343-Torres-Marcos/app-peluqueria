package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Reminder;
import com.metanoia.domain.model.ReminderType;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ReminderRepository;
import com.metanoia.domain.repository.SettingsRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Registra o deshace el envío de recordatorios de turno y saludos de cumpleaños evitando duplicados.
 */
public final class RecordReminderSentUseCase {

    private final ReminderRepository reminderRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public RecordReminderSentUseCase(
            ReminderRepository reminderRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.reminderRepository = Objects.requireNonNull(reminderRepository, "reminderRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Reminder> recordAppointmentReminder(String appointmentId, String clientId) {
        if (isBlank(appointmentId) || isBlank(clientId)) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Turno o cliente inválido");
        }
        String cleanAptId = appointmentId.trim();
        Optional<Reminder> existing = reminderRepository.findAppointmentReminder(cleanAptId);
        if (existing.isPresent()) {
            return Result.success(existing.get());
        }

        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        Reminder reminder = new Reminder.Builder()
                .id(idGenerator.nextId())
                .type(ReminderType.APPOINTMENT)
                .appointmentId(cleanAptId)
                .clientId(clientId.trim())
                .sentAt(now)
                .sentBy(operator)
                .build();
        reminderRepository.save(reminder);

        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(operator)
                .action("REMINDER_SENT")
                .entityType("REMINDER")
                .entityId(reminder.id())
                .concept("Recordatorio de turno enviado")
                .build());

        return Result.success(reminder);
    }

    public Result<Reminder> recordBirthdayGreeting(String clientId, int year) {
        if (isBlank(clientId) || year < 2000) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Cliente o año inválido");
        }
        String cleanClientId = clientId.trim();
        Optional<Reminder> existing = reminderRepository.findBirthdayReminder(cleanClientId, year);
        if (existing.isPresent()) {
            return Result.success(existing.get());
        }

        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        Reminder reminder = new Reminder.Builder()
                .id(idGenerator.nextId())
                .type(ReminderType.BIRTHDAY)
                .clientId(cleanClientId)
                .year(year)
                .sentAt(now)
                .sentBy(operator)
                .build();
        reminderRepository.save(reminder);

        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(operator)
                .action("REMINDER_SENT")
                .entityType("REMINDER")
                .entityId(reminder.id())
                .concept("Saludo de cumpleaños " + year)
                .build());

        return Result.success(reminder);
    }

    public Result<Boolean> undoReminder(String reminderId) {
        if (isBlank(reminderId)) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Recordatorio inválido");
        }
        String cleanId = reminderId.trim();
        reminderRepository.deleteById(cleanId);

        String operator = settingsRepository.getSettings().currentOperator();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(clock.nowMillis())
                .operator(operator)
                .action("REMINDER_UNDO")
                .entityType("REMINDER")
                .entityId(cleanId)
                .concept("Deshacer envío de recordatorio")
                .build());

        return Result.success(Boolean.TRUE);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
