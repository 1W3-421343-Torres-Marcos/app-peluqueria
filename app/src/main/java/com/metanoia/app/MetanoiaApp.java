package com.metanoia.app;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.metanoia.app.data.backup.BackupScheduler;
import com.metanoia.app.data.contacts.ContactSyncScheduler;
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
        runStartupBackgroundTasks(this, container);
        registerBackgroundBackupObserver();
    }

    private static void runStartupBackgroundTasks(Context context, AppContainer active) {
        Context appContext = context.getApplicationContext();
        active.executors().io().execute(() -> {
            BackupScheduler.schedulePeriodicAuto(
                    appContext, active.devicePrefs().getAutoFrequencyHours());
            ReminderDigestScheduler.syncSchedule(
                    appContext, active.devicePrefs(), active.clock());
            active.expireWaitlistEntriesUseCase().execute();
            ContactSyncScheduler.syncOnForegroundIfDue(appContext, active);
        });
    }

    private void registerBackgroundBackupObserver() {
        if (lifecycleRegistered) {
            return;
        }
        lifecycleRegistered = true;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(
                (LifecycleEventObserver) (source, event) -> {
                    if (container == null) {
                        return;
                    }
                    AppContainer active = container;
                    if (event == Lifecycle.Event.ON_START) {
                        active.executors().io().execute(() ->
                                ContactSyncScheduler.syncOnForegroundIfDue(
                                        MetanoiaApp.this, active));
                    } else if (event == Lifecycle.Event.ON_STOP) {
                        active.lockController().onAppBackgrounded(active.clock().nowMillis());
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
        runStartupBackgroundTasks(app, app.container);
    }
}
