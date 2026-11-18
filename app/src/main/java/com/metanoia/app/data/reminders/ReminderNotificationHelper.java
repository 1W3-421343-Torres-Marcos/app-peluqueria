package com.metanoia.app.data.reminders;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.metanoia.app.MainActivity;
import com.metanoia.app.R;

/**
 * Muestra la notificación local diaria con el resumen de recordatorios y cumpleaños pendientes.
 */
public final class ReminderNotificationHelper {

    public static final String CHANNEL_ID = "metanoia_reminders";
    private static final int NOTIFICATION_ID = 6001;

    private ReminderNotificationHelper() {
    }

    public static boolean hasNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void ensureChannel(@NonNull Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminders_notification_channel_desc));
        manager.createNotificationChannel(channel);
    }

    @SuppressLint("MissingPermission")
    public static void showDigestNotification(
            @NonNull Context context,
            int pendingTomorrow,
            int pendingBirthdays
    ) {
        if (!hasNotificationPermission(context)) {
            return;
        }
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (!nm.areNotificationsEnabled()) {
            return;
        }
        ensureChannel(context);

        String body;
        if (pendingTomorrow > 0 && pendingBirthdays > 0) {
            body = context.getString(
                    R.string.reminders_notification_body_both, pendingTomorrow, pendingBirthdays);
        } else if (pendingTomorrow > 0) {
            body = context.getString(
                    R.string.reminders_notification_body_tomorrow, pendingTomorrow);
        } else {
            body = context.getString(
                    R.string.reminders_notification_body_birthdays, pendingBirthdays);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_REMINDERS, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_clock)
                .setContentTitle(context.getString(R.string.reminders_notification_title))
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
