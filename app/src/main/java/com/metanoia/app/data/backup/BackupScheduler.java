package com.metanoia.app.data.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.metanoia.app.data.backup.BackupFolderAccess.FolderState;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.model.BackupTrigger;
import com.metanoia.domain.rules.BackupRetentionPolicy;

import java.util.concurrent.TimeUnit;

/**
 * Programador único de trabajos WorkManager para backups periódicos ({@code AUTO}),
 * al pasar a segundo plano ({@code ON_BACKGROUND}) y al cerrar caja ({@code CASH_CLOSE}).
 */
public final class BackupScheduler {

    public static final String UNIQUE_WORK_AUTO = "backup_periodic_auto";
    public static final String UNIQUE_WORK_ON_BACKGROUND = "backup_on_background";
    public static final String UNIQUE_WORK_CASH_CLOSE = "backup_cash_close";

    private BackupScheduler() {
    }

    public static void schedulePeriodicAuto(@NonNull Context context, int intervalHours) {
        int safeHours = Math.max(1, intervalHours);
        Data input = new Data.Builder()
                .putString(BackupWorker.KEY_TRIGGER, BackupTrigger.AUTO.name())
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                BackupWorker.class,
                safeHours,
                TimeUnit.HOURS
        )
                .setConstraints(Constraints.NONE)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_AUTO,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    public static void enqueueOnBackgroundIfNeeded(
            @NonNull Context context,
            @NonNull DevicePrefs prefs,
            @NonNull Clock clock
    ) {
        if (BackupFolderAccess.inspect(context, prefs).state != FolderState.READY) {
            return;
        }
        boolean overdueForBackground = BackupRetentionPolicy.shouldTriggerOnBackground(
                prefs.getLastBackupSuccessMillis(),
                clock.nowMillis(),
                prefs.getOnBackgroundThresholdHours()
        );
        if (overdueForBackground) {
            enqueueUniqueOneTime(context, UNIQUE_WORK_ON_BACKGROUND, BackupTrigger.ON_BACKGROUND);
        }
    }

    public static void enqueueCashClose(@NonNull Context context) {
        enqueueUniqueOneTime(context, UNIQUE_WORK_CASH_CLOSE, BackupTrigger.CASH_CLOSE);
    }

    private static void enqueueUniqueOneTime(
            @NonNull Context context,
            @NonNull String uniqueWorkName,
            @NonNull BackupTrigger trigger
    ) {
        Data input = new Data.Builder()
                .putString(BackupWorker.KEY_TRIGGER, trigger.name())
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setConstraints(Constraints.NONE)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                request
        );
    }
}
