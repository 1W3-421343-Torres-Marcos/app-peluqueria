package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.ReminderEntity;
import com.metanoia.domain.model.Reminder;
import com.metanoia.domain.model.ReminderType;

/**
 * Conversor entre {@link ReminderEntity} y el modelo de dominio {@link Reminder}.
 */
public final class ReminderMapper {

    private ReminderMapper() {
    }

    public static Reminder toDomain(ReminderEntity entity) {
        return new Reminder.Builder()
                .id(entity.id)
                .type(ReminderType.valueOf(entity.type))
                .appointmentId(entity.appointmentId)
                .clientId(entity.clientId)
                .year(entity.year)
                .sentAt(entity.sentAt)
                .sentBy(entity.sentBy)
                .build();
    }

    public static ReminderEntity toEntity(Reminder reminder) {
        return new ReminderEntity(
                reminder.id(),
                reminder.type().name(),
                reminder.appointmentId().orElse(null),
                reminder.clientId(),
                reminder.year().orElse(null),
                reminder.sentAt(),
                reminder.sentBy()
        );
    }
}
