package com.metanoia.app.data.reminders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.rules.DigestScheduleCalculator;

import java.util.concurrent.TimeUnit;

/**
 * Programa o cancela el aviso diario local de recordatorios a la hora configurada.
 */
public final class ReminderDigestScheduler {

    public static final String UNIQUE_WORK_REMINDER_DIGEST = "reminder_daily_digest";

    private ReminderDigestScheduler() {
    }

    public static void syncSchedule(
            @NonNull Context context,
            @NonNull DevicePrefs prefs,
            @NonNull Clock clock
    ) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        if (!prefs.isReminderDigestEnabled()) {
            workManager.cancelUniqueWork(UNIQUE_WORK_REMINDER_DIGEST);
            return;
        }
        long initialDelayMillis = DigestScheduleCalculator.initialDelayMillis(
                clock.nowMillis(),
                clock.zoneId(),
                prefs.getReminderDigestHour(),
                prefs.getReminderDigestMinute());
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderDigestWorker.class,
                24,
                TimeUnit.HOURS
        )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.NONE)
                .build();
        workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_REMINDER_DIGEST,
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }
}
