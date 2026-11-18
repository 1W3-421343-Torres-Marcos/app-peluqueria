package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.SalonHoursEntity;
import com.metanoia.app.data.db.entity.SettingsEntity;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapea entre las entidades Room de configuración y el modelo de dominio {@link Settings}.
 */
public final class SettingsMapper {

    private SettingsMapper() {
    }

    public static Settings toDomain(SettingsEntity entity, List<SalonHoursEntity> hoursEntities) {
        if (entity == null) {
            return Settings.defaultSettings();
        }
        List<String> operators = parseOperatorsCsv(entity.operatorsCsv);
        if (operators.isEmpty()) {
            operators = Collections.singletonList(entity.currentOperator);
        }
        List<TimeRange> schedule = new ArrayList<>();
        if (hoursEntities != null) {
            for (SalonHoursEntity hour : hoursEntities) {
                if (hour.dayOfWeek >= 1 && hour.dayOfWeek <= 7
                        && hour.startMin >= 0 && hour.endMin <= 1440
                        && hour.startMin < hour.endMin) {
                    schedule.add(new TimeRange(hour.dayOfWeek, hour.startMin, hour.endMin));
                }
            }
        }
        return new Settings.Builder()
                .businessName(entity.businessName)
                .subtitle(entity.subtitle)
                .address(entity.address)
                .phone(entity.phone)
                .cuit(entity.cuit)
                .slotMinutes(entity.slotMinutes)
                .currentOperator(entity.currentOperator)
                .operators(operators)
                .generalSchedule(schedule)
                .waitlistExpiryDays(entity.waitlistExpiryDays)
                .build();
    }

    public static SettingsEntity toEntity(Settings settings, long updatedAtMillis) {
        String operatorsCsv = String.join(",", settings.operators());
        return new SettingsEntity(
                SettingsEntity.DEFAULT_ID,
                settings.businessName(),
                settings.subtitle(),
                settings.address(),
                settings.phone(),
                settings.cuit(),
                settings.slotMinutes(),
                settings.currentOperator(),
                operatorsCsv,
                settings.waitlistExpiryDays(),
                updatedAtMillis
        );
    }

    public static List<SalonHoursEntity> toHoursEntities(Settings settings) {
        List<SalonHoursEntity> list = new ArrayList<>();
        for (TimeRange range : settings.generalSchedule()) {
            list.add(new SalonHoursEntity(range.dayOfWeek(), range.startMin(), range.endMin()));
        }
        return list;
    }

    private static List<String> parseOperatorsCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
