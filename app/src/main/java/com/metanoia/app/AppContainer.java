package com.metanoia.app;

import android.content.Context;

import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;

/**
 * Contenedor manual de dependencias de la aplicación.
 */
public final class AppContainer {

    private final AppExecutors executors;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DevicePrefs devicePrefs;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        this.executors = new AppExecutors();
        this.clock = Clock.systemArgentina();
        this.idGenerator = IdGenerator.uuid();
        this.devicePrefs = new DevicePrefs(appContext);
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
}
