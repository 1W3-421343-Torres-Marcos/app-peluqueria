package com.metanoia.domain.repository;

import com.metanoia.domain.model.Settings;

/**
 * Repositorio para leer y persistir la configuración global del salón.
 */
public interface SettingsRepository {

    Settings getSettings();

    void saveSettings(Settings settings);
}
