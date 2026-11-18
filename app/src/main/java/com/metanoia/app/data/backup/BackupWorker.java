package com.metanoia.app.data.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderState;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderStatus;
import com.metanoia.app.data.backup.BackupManager.BackupCreated;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.model.BackupTrigger;

/**
 * Worker de segundo plano para disparos {@code AUTO}, {@code ON_BACKGROUND} y {@code CASH_CLOSE}.
 */
public final class BackupWorker extends Worker {

    public static final String KEY_TRIGGER = "backup_trigger";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        BackupTrigger trigger = parseTrigger(getInputData());
        Context appContext = getApplicationContext();
        AppContainer container = MetanoiaApp.container(appContext);
        FolderStatus status = BackupFolderAccess.inspect(appContext, container.devicePrefs());
        if (status.state != FolderState.READY) {
            return ListenableWorker.Result.success();
        }

        com.metanoia.domain.common.Result<BackupCreated> outcome =
                container.backupManager().createBackupToConfiguredFolder(trigger);
        if (outcome.isSuccess() || outcome.getError() == DomainError.CONFLICT) {
            return ListenableWorker.Result.success();
        }
        return getRunAttemptCount() < MAX_RETRY_ATTEMPTS
                ? ListenableWorker.Result.retry()
                : ListenableWorker.Result.failure();
    }

    @NonNull
    private static BackupTrigger parseTrigger(@NonNull Data inputData) {
        String raw = inputData.getString(KEY_TRIGGER);
        if (raw == null || raw.trim().isEmpty()) {
            return BackupTrigger.AUTO;
        }
        try {
            return BackupTrigger.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            return BackupTrigger.AUTO;
        }
    }
}
