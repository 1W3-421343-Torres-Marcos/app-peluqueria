package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.entity.RecurrenceRuleEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.RecurrenceRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Convierte entre entidades Room de turnos/ítems/recurrencias y modelos de dominio.
 */
public final class AppointmentMapper {

    private AppointmentMapper() {
    }

    public static Appointment toDomain(AppointmentWithItems withItems) {
        AppointmentEntity entity = withItems.appointment;
        List<AppointmentItemEntity> rawItems = withItems.items == null
                ? Collections.emptyList()
                : new ArrayList<>(withItems.items);
        rawItems.sort(Comparator.comparingInt(item -> item.position));

        List<AppointmentItem> domainItems = new ArrayList<>(rawItems.size());
        for (AppointmentItemEntity itemEntity : rawItems) {
            domainItems.add(toDomainItem(itemEntity));
        }

        return new Appointment.Builder()
                .id(entity.id)
                .clientId(entity.clientId)
                .professionalId(entity.professionalId)
                .day(entity.day)
                .startMin(entity.startMin)
                .endMin(entity.endMin)
                .status(AppointmentStatus.valueOf(entity.status))
                .adjustment(Money.ofCents(entity.adjustmentCents))
                .notes(entity.notes)
                .recurrenceRuleId(entity.recurrenceRuleId)
                .cancelReason(entity.cancelReason)
                .items(domainItems)
                .createdAt(entity.createdAt)
                .updatedAt(entity.updatedAt)
                .createdBy(entity.createdBy)
                .build();
    }

    public static List<Appointment> toDomainList(List<AppointmentWithItems> list) {
        List<Appointment> result = new ArrayList<>(list.size());
        for (AppointmentWithItems withItems : list) {
            result.add(toDomain(withItems));
        }
        return result;
    }

    public static AppointmentItem toDomainItem(AppointmentItemEntity entity) {
        return new AppointmentItem.Builder()
                .id(entity.id)
                .appointmentId(entity.appointmentId)
                .serviceId(entity.serviceId)
                .serviceNameSnapshot(entity.serviceNameSnapshot)
                .priceSnapshot(Money.ofCents(entity.priceCentsSnapshot))
                .durationMinSnapshot(entity.durationMinSnapshot)
                .commissionBasisPointsSnapshot(entity.commissionBasisPointsSnapshot)
                .colorFormulaId(entity.colorFormulaId)
                .position(entity.position)
                .build();
    }

    public static AppointmentEntity toEntity(Appointment appointment) {
        return new AppointmentEntity(
                appointment.id(),
                appointment.clientId(),
                appointment.professionalId(),
                appointment.day(),
                appointment.startMin(),
                appointment.endMin(),
                appointment.status().name(),
                appointment.adjustment().cents(),
                appointment.notes(),
                appointment.recurrenceRuleId().orElse(null),
                appointment.cancelReason().orElse(null),
                appointment.createdAt(),
                appointment.updatedAt(),
                appointment.createdBy()
        );
    }

    public static List<AppointmentItemEntity> toItemEntities(Appointment appointment) {
        List<AppointmentItem> items = appointment.items();
        List<AppointmentItemEntity> entities = new ArrayList<>(items.size());
        for (AppointmentItem item : items) {
            entities.add(new AppointmentItemEntity(
                    item.id(),
                    appointment.id(),
                    item.serviceId(),
                    item.serviceNameSnapshot(),
                    item.priceSnapshot().cents(),
                    item.durationMinSnapshot(),
                    item.commissionBasisPointsSnapshot().orElse(null),
                    item.colorFormulaId().orElse(null),
                    item.position()
            ));
        }
        return entities;
    }

    public static RecurrenceRuleEntity toRecurrenceEntity(RecurrenceRule rule) {
        return new RecurrenceRuleEntity(
                rule.id(),
                rule.intervalWeeks(),
                rule.untilDay().orElse(null),
                rule.maxOccurrences().orElse(null),
                rule.createdAt()
        );
    }
}
