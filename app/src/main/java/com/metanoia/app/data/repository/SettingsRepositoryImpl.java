package com.metanoia.app.data.repository;

import com.metanoia.app.data.db.dao.SettingsDao;
import com.metanoia.app.data.db.entity.SalonHoursEntity;
import com.metanoia.app.data.db.entity.SettingsEntity;
import com.metanoia.app.data.mapper.SettingsMapper;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.SettingsRepository;

import java.util.List;
import java.util.Objects;

/**
 * Implementación Room de {@link SettingsRepository}.
 */
public final class SettingsRepositoryImpl implements SettingsRepository {

    private final SettingsDao settingsDao;
    private final Clock clock;

    public SettingsRepositoryImpl(SettingsDao settingsDao, Clock clock) {
        this.settingsDao = Objects.requireNonNull(settingsDao, "settingsDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Settings getSettings() {
        SettingsEntity entity = settingsDao.getSettingsSync();
        if (entity == null) {
            Settings defaults = Settings.defaultSettings();
            saveSettings(defaults);
            return defaults;
        }
        List<SalonHoursEntity> hours = settingsDao.getHoursSync();
        return SettingsMapper.toDomain(entity, hours);
    }

    @Override
    public void saveSettings(Settings settings) {
        SettingsEntity entity = SettingsMapper.toEntity(settings, clock.nowMillis());
        List<SalonHoursEntity> hours = SettingsMapper.toHoursEntities(settings);
        settingsDao.saveSettingsAndHours(entity, hours);
    }
}
