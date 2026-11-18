package com.metanoia.app;

import android.content.Context;

import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.data.repository.AuditLogRepositoryImpl;
import com.metanoia.app.data.repository.SettingsRepositoryImpl;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.usecase.GetSettingsUseCase;
import com.metanoia.domain.usecase.UpdateSettingsUseCase;

/**
 * Contenedor manual de dependencias de la aplicación.
 */
public final class AppContainer {

    private final AppExecutors executors;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DevicePrefs devicePrefs;
    private final AppDatabase database;
    private final AuditLogRepository auditLogRepository;
    private final SettingsRepository settingsRepository;
    private final GetSettingsUseCase getSettingsUseCase;
    private final UpdateSettingsUseCase updateSettingsUseCase;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        this.executors = new AppExecutors();
        this.clock = Clock.systemArgentina();
        this.idGenerator = IdGenerator.uuid();
        this.devicePrefs = new DevicePrefs(appContext);
        this.database = AppDatabase.create(appContext);
        this.auditLogRepository = new AuditLogRepositoryImpl(database.auditLogDao());
        this.settingsRepository = new SettingsRepositoryImpl(database.settingsDao(), clock);
        this.getSettingsUseCase = new GetSettingsUseCase(settingsRepository);
        this.updateSettingsUseCase = new UpdateSettingsUseCase(
                settingsRepository, auditLogRepository, clock, idGenerator);
    }

    public AppExecutors executors() {
        return executors;
    }

    public Clock clock() {
        return clock;
    }

    public IdGenerator idGenerator() {
        return idGenerator;
    }

    public DevicePrefs devicePrefs() {
        return devicePrefs;
    }

    public AppDatabase database() {
        return database;
    }

    public AuditLogRepository auditLogRepository() {
        return auditLogRepository;
    }

    public SettingsRepository settingsRepository() {
        return settingsRepository;
    }

    public GetSettingsUseCase getSettingsUseCase() {
        return getSettingsUseCase;
    }

    public UpdateSettingsUseCase updateSettingsUseCase() {
        return updateSettingsUseCase;
    }
}
