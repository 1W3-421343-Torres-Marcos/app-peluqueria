package com.metanoia.app.data.reminders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.data.db.dao.ReminderDao;

import java.time.LocalDate;

/**
 * Worker diario 100 % local que cuenta recordatorios y cumpleaños pendientes y notifica al usuario.
 */
public final class ReminderDigestWorker extends Worker {

    public ReminderDigestWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        Context appContext = getApplicationContext();
        AppContainer container = MetanoiaApp.container(appContext);
        if (!container.devicePrefs().isReminderDigestEnabled()) {
            return ListenableWorker.Result.success();
        }
        long todayEpoch = container.clock().todayEpochDay();
        LocalDate today = LocalDate.ofEpochDay(todayEpoch);

        ReminderDao reminderDao = container.database().reminderDao();
        int pendingTomorrow = reminderDao.countPendingTomorrowSync(todayEpoch + 1L);
        int pendingBirthdays = reminderDao.countPendingBirthdaysSync(
                today.getDayOfMonth(), today.getMonthValue(), today.getYear());

        if (pendingTomorrow + pendingBirthdays > 0) {
            ReminderNotificationHelper.showDigestNotification(
                    appContext, pendingTomorrow, pendingBirthdays);
        }
        return ListenableWorker.Result.success();
    }
}
