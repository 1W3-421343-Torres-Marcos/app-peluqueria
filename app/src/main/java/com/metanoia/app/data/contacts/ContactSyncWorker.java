package com.metanoia.app.data.contacts;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;

/**
 * Worker de WorkManager activado por cambios en {@code ContactsContract} que se auto-reprograma al terminar.
 */
public final class ContactSyncWorker extends Worker {

    public ContactSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        try {
            ContactSyncCoordinator.runAutomaticSync(appContext);
            return Result.success();
        } finally {
            AppContainer container = MetanoiaApp.container(appContext);
            if (container != null
                    && container.devicePrefs().isContactSyncEnabled()
                    && DeviceContactsReader.hasPermission(appContext)) {
                ContactSyncScheduler.scheduleContentUriTrigger(appContext, true);
            }
        }
    }
}
