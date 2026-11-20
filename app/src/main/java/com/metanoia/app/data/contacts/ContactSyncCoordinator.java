package com.metanoia.app.data.contacts;

import android.content.Context;

import androidx.annotation.NonNull;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.ContactSyncOutcome;
import com.metanoia.domain.model.DeviceContactCandidate;
import com.metanoia.domain.usecase.SyncContactsUseCase;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordina de forma idempotente y segura la lectura de contactos y alta automática de nuevos clientes.
 */
public final class ContactSyncCoordinator {

    private static final ReentrantLock SYNC_LOCK = new ReentrantLock();

    private ContactSyncCoordinator() {
    }

    public static void runAutomaticSync(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        AppContainer container = MetanoiaApp.container(appContext);
        if (container == null) {
            return;
        }
        DevicePrefs prefs = container.devicePrefs();
        if (!prefs.isContactSyncEnabled()) {
            return;
        }
        if (!DeviceContactsReader.hasPermission(appContext)) {
            prefs.recordContactSyncResult(
                    container.clock().nowMillis(),
                    0,
                    DevicePrefs.SYNC_STATUS_NO_PERMISSION);
            ContactSyncScheduler.cancelContentUriTrigger(appContext);
            return;
        }

        SYNC_LOCK.lock();
        try {
            if (!prefs.isContactSyncEnabled()) {
                return;
            }
            if (!DeviceContactsReader.hasPermission(appContext)) {
                prefs.recordContactSyncResult(
                        container.clock().nowMillis(),
                        0,
                        DevicePrefs.SYNC_STATUS_NO_PERMISSION);
                ContactSyncScheduler.cancelContentUriTrigger(appContext);
                return;
            }
            List<DeviceContactCandidate> candidates =
                    container.deviceContactsReader().readAllContacts();
            Result<ContactSyncOutcome> result = container.syncContactsUseCase().execute(
                    candidates, SyncContactsUseCase.ACTION_AUTO_SYNC);
            long now = container.clock().nowMillis();
            if (result.isSuccess()) {
                ContactSyncOutcome outcome = result.getValue();
                prefs.recordContactSyncResult(
                        outcome.executedAtMillis(),
                        outcome.importedCount(),
                        DevicePrefs.SYNC_STATUS_OK);
                prefs.setLastContactSyncAttemptMillis(outcome.executedAtMillis());
            } else {
                prefs.recordContactSyncResult(now, 0, DevicePrefs.SYNC_STATUS_ERROR);
                prefs.setLastContactSyncAttemptMillis(now);
            }
        } catch (RuntimeException ex) {
            prefs.recordContactSyncResult(
                    container.clock().nowMillis(), 0, DevicePrefs.SYNC_STATUS_ERROR);
        } finally {
            SYNC_LOCK.unlock();
        }
    }
}
