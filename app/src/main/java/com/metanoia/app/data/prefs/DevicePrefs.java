package com.metanoia.app.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Preferencias locales del dispositivo (nunca viajan en el backup de datos de negocio).
 */
public final class DevicePrefs {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final int AUTO_FREQ_12H = 12;
    public static final int AUTO_FREQ_24H = 24;
    public static final int AUTO_FREQ_48H = 48;

    public static final int DEFAULT_AUTO_FREQUENCY_HOURS = AUTO_FREQ_24H;
    public static final int DEFAULT_RETENTION_DAYS = 7;
    public static final int DEFAULT_ON_BACKGROUND_HOURS = 6;
    public static final int DEFAULT_OVERDUE_WARNING_DAYS = 3;
    public static final boolean DEFAULT_REMINDER_DIGEST_ENABLED = true;
    public static final int DEFAULT_REMINDER_DIGEST_HOUR = 18;
    public static final int DEFAULT_REMINDER_DIGEST_MINUTE = 0;

    private static final String PREFS_NAME = "metanoia_device_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_BACKUP_TREE_URI = "backup_tree_uri";
    private static final String KEY_LAST_BACKUP_SUCCESS_MILLIS = "last_backup_success_millis";
    private static final String KEY_LAST_BACKUP_FILE = "last_backup_file";
    private static final String KEY_LAST_BACKUP_TRIGGER = "last_backup_trigger";
    private static final String KEY_LAST_BACKUP_ERROR = "last_backup_error";
    private static final String KEY_AUTO_FREQUENCY_HOURS = "backup_auto_frequency_hours";
    private static final String KEY_RETENTION_DAYS = "backup_retention_days";
    private static final String KEY_ON_BACKGROUND_HOURS = "backup_on_background_hours";
    private static final String KEY_OVERDUE_WARNING_DAYS = "backup_overdue_warning_days";
    private static final String KEY_REMINDER_DIGEST_ENABLED = "reminder_digest_enabled";
    private static final String KEY_REMINDER_DIGEST_HOUR = "reminder_digest_hour";
    private static final String KEY_REMINDER_DIGEST_MINUTE = "reminder_digest_minute";

    private final SharedPreferences prefs;

    public DevicePrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public void setThemeMode(int mode) {
        int normalized = normalizeThemeMode(mode);
        prefs.edit().putInt(KEY_THEME_MODE, normalized).apply();
        applyTheme(normalized);
    }

    public void applySavedTheme() {
        applyTheme(getThemeMode());
    }

    @Nullable
    public String getBackupTreeUri() {
        String raw = prefs.getString(KEY_BACKUP_TREE_URI, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return raw;
    }

    public void setBackupTreeUri(@Nullable String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) {
            prefs.edit().remove(KEY_BACKUP_TREE_URI).apply();
        } else {
            prefs.edit().putString(KEY_BACKUP_TREE_URI, uriString.trim()).apply();
        }
    }

    public long getLastBackupSuccessMillis() {
        return prefs.getLong(KEY_LAST_BACKUP_SUCCESS_MILLIS, 0L);
    }

    @NonNull
    public String getLastBackupFileName() {
        return prefs.getString(KEY_LAST_BACKUP_FILE, "");
    }

    @NonNull
    public String getLastBackupTrigger() {
        return prefs.getString(KEY_LAST_BACKUP_TRIGGER, "");
    }

    @NonNull
    public String getLastBackupError() {
        return prefs.getString(KEY_LAST_BACKUP_ERROR, "");
    }

    public void recordBackupSuccess(
            long timestampMillis,
            @NonNull String fileName,
            @NonNull String trigger
    ) {
        prefs.edit()
                .putLong(KEY_LAST_BACKUP_SUCCESS_MILLIS, timestampMillis)
                .putString(KEY_LAST_BACKUP_FILE, fileName)
                .putString(KEY_LAST_BACKUP_TRIGGER, trigger)
                .remove(KEY_LAST_BACKUP_ERROR)
                .commit();
    }

    public void recordBackupError(@NonNull String errorMessage) {
        prefs.edit()
                .putString(KEY_LAST_BACKUP_ERROR, errorMessage)
                .commit();
    }

    public int getAutoFrequencyHours() {
        int raw = prefs.getInt(KEY_AUTO_FREQUENCY_HOURS, DEFAULT_AUTO_FREQUENCY_HOURS);
        return normalizeFrequencyHours(raw);
    }

    public void setAutoFrequencyHours(int hours) {
        prefs.edit().putInt(KEY_AUTO_FREQUENCY_HOURS, normalizeFrequencyHours(hours)).apply();
    }

    public int getRetentionDays() {
        return clampInt(
                prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS), 1, 60);
    }

    public void setRetentionDays(int days) {
        prefs.edit().putInt(KEY_RETENTION_DAYS, clampInt(days, 1, 60)).apply();
    }

    public int getOnBackgroundThresholdHours() {
        return clampInt(
                prefs.getInt(KEY_ON_BACKGROUND_HOURS, DEFAULT_ON_BACKGROUND_HOURS), 1, 72);
    }

    public void setOnBackgroundThresholdHours(int hours) {
        prefs.edit().putInt(KEY_ON_BACKGROUND_HOURS, clampInt(hours, 1, 72)).apply();
    }

    public int getOverdueWarningDays() {
        return clampInt(
                prefs.getInt(KEY_OVERDUE_WARNING_DAYS, DEFAULT_OVERDUE_WARNING_DAYS), 1, 30);
    }

    public void setOverdueWarningDays(int days) {
        prefs.edit().putInt(KEY_OVERDUE_WARNING_DAYS, clampInt(days, 1, 30)).apply();
    }

    public boolean isReminderDigestEnabled() {
        return prefs.getBoolean(KEY_REMINDER_DIGEST_ENABLED, DEFAULT_REMINDER_DIGEST_ENABLED);
    }

    public void setReminderDigestEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDER_DIGEST_ENABLED, enabled).apply();
    }

    public int getReminderDigestHour() {
        return clampInt(prefs.getInt(KEY_REMINDER_DIGEST_HOUR, DEFAULT_REMINDER_DIGEST_HOUR), 0, 23);
    }

    public int getReminderDigestMinute() {
        return clampInt(
                prefs.getInt(KEY_REMINDER_DIGEST_MINUTE, DEFAULT_REMINDER_DIGEST_MINUTE), 0, 59);
    }

    public void setReminderDigestTime(int hour, int minute) {
        prefs.edit()
                .putInt(KEY_REMINDER_DIGEST_HOUR, clampInt(hour, 0, 23))
                .putInt(KEY_REMINDER_DIGEST_MINUTE, clampInt(minute, 0, 59))
                .apply();
    }

    private static int normalizeFrequencyHours(int hours) {
        if (hours == AUTO_FREQ_12H || hours == AUTO_FREQ_48H) {
            return hours;
        }
        return AUTO_FREQ_24H;
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static void applyTheme(int mode) {
        switch (normalizeThemeMode(mode)) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private static int normalizeThemeMode(int mode) {
        if (mode == THEME_LIGHT || mode == THEME_DARK) {
            return mode;
        }
        return THEME_SYSTEM;
    }

    SharedPreferences raw() {
        return prefs;
    }
}
