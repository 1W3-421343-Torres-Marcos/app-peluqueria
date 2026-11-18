package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.SettingsRepository;

import java.util.Objects;

/**
 * Caso de uso para obtener la configuración vigente del salón.
 */
public final class GetSettingsUseCase {

    private final SettingsRepository settingsRepository;

    public GetSettingsUseCase(SettingsRepository settingsRepository) {
        this.settingsRepository = Objects.requireNonNull(
                settingsRepository, "settingsRepository");
    }

    public Result<Settings> execute() {
        return Result.success(settingsRepository.getSettings());
    }
}
