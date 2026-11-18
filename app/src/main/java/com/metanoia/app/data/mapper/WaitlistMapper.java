package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.WaitlistEntryEntity;
import com.metanoia.app.data.db.entity.WaitlistEntryServiceCrossRef;
import com.metanoia.app.data.db.pojo.WaitlistEntryWithServices;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Convierte entre entidades Room de lista de espera y modelos {@link WaitlistEntry}.
 */
public final class WaitlistMapper {

    private WaitlistMapper() {
    }

    public static WaitlistEntry toDomain(WaitlistEntryWithServices row) {
        WaitlistEntryEntity entity = row.entry;
        List<String> serviceIds = new ArrayList<>();
        if (row.serviceRefs != null) {
            for (WaitlistEntryServiceCrossRef ref : row.serviceRefs) {
                serviceIds.add(ref.serviceId);
            }
        }
        return new WaitlistEntry.Builder()
                .id(entity.id)
                .clientId(entity.clientId)
                .serviceIds(serviceIds)
                .professionalId(entity.professionalId)
                .specificDay(entity.specificDay)
                .preferredDaysOfWeek(parseWeekdays(entity.preferredWeekdays))
                .fromMin(entity.fromMin)
                .toMin(entity.toMin)
                .validUntilDay(entity.validUntilDay)
                .status(WaitlistStatus.valueOf(entity.status))
                .notes(entity.notes)
                .createdAt(entity.createdAt)
                .offeredAt(entity.offeredAt)
                .build();
    }

    public static List<WaitlistEntry> toDomainList(List<WaitlistEntryWithServices> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<WaitlistEntry> out = new ArrayList<>(rows.size());
        for (WaitlistEntryWithServices row : rows) {
            if (row != null && row.entry != null
                    && row.serviceRefs != null && !row.serviceRefs.isEmpty()) {
                out.add(toDomain(row));
            }
        }
        return out;
    }

    public static WaitlistEntryEntity toEntity(WaitlistEntry entry) {
        return new WaitlistEntryEntity(
                entry.id(),
                entry.clientId(),
                entry.professionalId().orElse(null),
                entry.specificDay().orElse(null),
                serializeWeekdays(entry.preferredDaysOfWeek()),
                entry.fromMin(),
                entry.toMin(),
                entry.validUntilDay(),
                entry.status().name(),
                entry.notes(),
                entry.createdAt(),
                entry.offeredAt().orElse(null)
        );
    }

    public static List<WaitlistEntryServiceCrossRef> toServiceRefs(WaitlistEntry entry) {
        List<String> serviceIds = entry.serviceIds();
        List<WaitlistEntryServiceCrossRef> refs = new ArrayList<>(serviceIds.size());
        for (String serviceId : serviceIds) {
            refs.add(new WaitlistEntryServiceCrossRef(entry.id(), serviceId));
        }
        return refs;
    }

    private static Set<Integer> parseWeekdays(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> result = new TreeSet<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            String clean = part.trim();
            if (!clean.isEmpty()) {
                result.add(Integer.parseInt(clean));
            }
        }
        return result;
    }

    private static String serializeWeekdays(Set<Integer> days) {
        if (days == null || days.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer day : new TreeSet<>(days)) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(day);
        }
        return sb.toString();
    }
}
