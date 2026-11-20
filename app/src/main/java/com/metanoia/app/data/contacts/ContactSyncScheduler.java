package com.metanoia.app.data.contacts;

import android.content.Context;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.metanoia.app.AppContainer;
import com.metanoia.app.data.prefs.DevicePrefs;

import java.util.concurrent.TimeUnit;

/**
 * Programa el trigger por URI de contactos en WorkManager y el respaldo en primer plano (cada 15 min).
 */
public final class ContactSyncScheduler {

    public static final String UNIQUE_URI_WORK_NAME = "metanoia_contact_uri_sync";
    public static final long FOREGROUND_THROTTLE_MILLIS = 15L * 60L * 1000L;

    private ContactSyncScheduler() {
    }

    public static void scheduleContentUriTrigger(
            @NonNull Context context,
            boolean replaceExisting
    ) {
        Context appContext = context.getApplicationContext();
        if (!DeviceContactsReader.hasPermission(appContext)) {
            return;
        }
        Constraints constraints = new Constraints.Builder()
                .addContentUriTrigger(ContactsContract.Contacts.CONTENT_URI, true)
                .setTriggerContentUpdateDelay(5L, TimeUnit.SECONDS)
                .setTriggerContentMaxDelay(30L, TimeUnit.SECONDS)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ContactSyncWorker.class)
                .setConstraints(constraints)
                .build();

        ExistingWorkPolicy policy = replaceExisting
                ? ExistingWorkPolicy.REPLACE
                : ExistingWorkPolicy.KEEP;
        WorkManager.getInstance(appContext).enqueueUniqueWork(
                UNIQUE_URI_WORK_NAME, policy, request);
    }

    public static void cancelContentUriTrigger(@NonNull Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(UNIQUE_URI_WORK_NAME);
    }

    public static void syncOnForegroundIfDue(
            @NonNull Context context,
            @NonNull AppContainer container
    ) {
        Context appContext = context.getApplicationContext();
        DevicePrefs prefs = container.devicePrefs();
        if (!prefs.isContactSyncEnabled()) {
            return;
        }
        long now = container.clock().nowMillis();
        if (!DeviceContactsReader.hasPermission(appContext)) {
            if (!DevicePrefs.SYNC_STATUS_NO_PERMISSION.equals(prefs.getLastContactSyncStatus())) {
                prefs.recordContactSyncResult(now, 0, DevicePrefs.SYNC_STATUS_NO_PERMISSION);
            }
            cancelContentUriTrigger(appContext);
            return;
        }

        scheduleContentUriTrigger(appContext, false);
        long lastAttempt = prefs.getLastContactSyncAttemptMillis();
        if (lastAttempt == 0L || (now - lastAttempt) >= FOREGROUND_THROTTLE_MILLIS) {
            prefs.setLastContactSyncAttemptMillis(now);
            ContactSyncCoordinator.runAutomaticSync(appContext);
        }
    }
}
