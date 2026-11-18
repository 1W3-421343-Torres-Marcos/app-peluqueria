package com.metanoia.app;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.metanoia.app.data.backup.BackupScheduler;
import com.metanoia.app.data.reminders.ReminderDigestScheduler;

/**
 * Punto de entrada de la aplicación; inicializa el contenedor de dependencias y aplica el tema.
 */
public class MetanoiaApp extends Application {

    private AppContainer container;
    private boolean lifecycleRegistered;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        container.devicePrefs().applySavedTheme();
        BackupScheduler.schedulePeriodicAuto(this, container.devicePrefs().getAutoFrequencyHours());
        ReminderDigestScheduler.syncSchedule(
                this, container.devicePrefs(), container.clock());
        registerBackgroundBackupObserver();
    }

    private void registerBackgroundBackupObserver() {
        if (lifecycleRegistered) {
            return;
        }
        lifecycleRegistered = true;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(
                (LifecycleEventObserver) (source, event) -> {
                    if (event == Lifecycle.Event.ON_STOP && container != null) {
                        AppContainer active = container;
                        active.executors().io().execute(() ->
                                BackupScheduler.enqueueOnBackgroundIfNeeded(
                                        MetanoiaApp.this,
                                        active.devicePrefs(),
                                        active.clock()));
                    }
                });
    }

    public AppContainer getContainer() {
        return container;
    }

    public static AppContainer container(Context context) {
        MetanoiaApp app = (MetanoiaApp) context.getApplicationContext();
        return app.getContainer();
    }

    public static void resetContainer(Context context) {
        MetanoiaApp app = (MetanoiaApp) context.getApplicationContext();
        if (app.container != null && app.container.database().isOpen()) {
            app.container.database().close();
        }
        app.container = new AppContainer(app);
        app.container.devicePrefs().applySavedTheme();
        BackupScheduler.schedulePeriodicAuto(app, app.container.devicePrefs().getAutoFrequencyHours());
        ReminderDigestScheduler.syncSchedule(
                app, app.container.devicePrefs(), app.container.clock());
    }
}
