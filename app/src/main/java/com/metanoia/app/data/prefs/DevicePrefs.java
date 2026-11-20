package com.metanoia.app.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.metanoia.domain.rules.PinHasher;
import com.metanoia.domain.rules.PinLockPolicy;

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
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_ITERATIONS = "pin_iterations";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_LOCK_TIMEOUT_MINUTES = "lock_timeout_minutes";
    private static final String KEY_PIN_FAILED_ATTEMPTS = "pin_failed_attempts";
    private static final String KEY_PIN_LOCKED_UNTIL_MILLIS = "pin_locked_until_millis";
    private static final String KEY_LAST_BACKGROUND_MILLIS = "last_background_millis";
    private static final String KEY_INITIAL_SETUP_DISMISSED = "initial_setup_dismissed";
    private static final String KEY_INITIAL_CONTACTS_IMPORT_DONE = "initial_contacts_import_done";
    private static final String KEY_CONTACT_SYNC_ENABLED = "contact_sync_enabled";
    private static final String KEY_LAST_CONTACT_SYNC_MILLIS = "last_contact_sync_millis";
    private static final String KEY_LAST_CONTACT_SYNC_ADDED = "last_contact_sync_added";
    private static final String KEY_LAST_CONTACT_SYNC_STATUS = "last_contact_sync_status";
    private static final String KEY_LAST_CONTACT_SYNC_ATTEMPT_MILLIS =
            "last_contact_sync_attempt_millis";

    public static final String SYNC_STATUS_NONE = "";
    public static final String SYNC_STATUS_OK = "OK";
    public static final String SYNC_STATUS_NO_PERMISSION = "NO_PERMISSION";
    public static final String SYNC_STATUS_ERROR = "ERROR";

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

    public boolean isPinEnabled() {
        return prefs.getBoolean(KEY_PIN_ENABLED, false)
                && !getPinSalt().isEmpty()
                && !getPinHash().isEmpty();
    }

    @NonNull
    public String getPinSalt() {
        return prefs.getString(KEY_PIN_SALT, "");
    }

    @NonNull
    public String getPinHash() {
        return prefs.getString(KEY_PIN_HASH, "");
    }

    public int getPinIterations() {
        int raw = prefs.getInt(KEY_PIN_ITERATIONS, PinHasher.DEFAULT_ITERATIONS);
        return raw > 0 ? raw : PinHasher.DEFAULT_ITERATIONS;
    }

    public void savePinHash(@NonNull String saltBase64, @NonNull String hashBase64, int iterations) {
        prefs.edit()
                .putBoolean(KEY_PIN_ENABLED, true)
                .putString(KEY_PIN_SALT, saltBase64)
                .putString(KEY_PIN_HASH, hashBase64)
                .putInt(KEY_PIN_ITERATIONS, iterations)
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKED_UNTIL_MILLIS, 0L)
                .commit();
    }

    public void clearPin() {
        prefs.edit()
                .putBoolean(KEY_PIN_ENABLED, false)
                .remove(KEY_PIN_SALT)
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_ITERATIONS)
                .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKED_UNTIL_MILLIS, 0L)
                .putLong(KEY_LAST_BACKGROUND_MILLIS, 0L)
                .commit();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) && isPinEnabled();
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled && isPinEnabled()).commit();
    }

    public int getLockTimeoutMinutes() {
        return PinLockPolicy.normalizeTimeoutMinutes(
                prefs.getInt(KEY_LOCK_TIMEOUT_MINUTES, PinLockPolicy.DEFAULT_TIMEOUT_MINUTES));
    }

    public void setLockTimeoutMinutes(int minutes) {
        prefs.edit().putInt(
                KEY_LOCK_TIMEOUT_MINUTES,
                PinLockPolicy.normalizeTimeoutMinutes(minutes)).apply();
    }

    public int getFailedPinAttempts() {
        return Math.max(0, prefs.getInt(KEY_PIN_FAILED_ATTEMPTS, 0));
    }

    public long getPinLockedUntilMillis() {
        return Math.max(0L, prefs.getLong(KEY_PIN_LOCKED_UNTIL_MILLIS, 0L));
    }

    public long recordFailedPinAttempt(long nowMillis) {
        int nextAttempts = getFailedPinAttempts() + 1;
        long cooldownMs = PinLockPolicy.cooldownDurationMs(nextAttempts);
        long lockedUntil = cooldownMs > 0L ? nowMillis + cooldownMs : 0L;
        prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, nextAttempts)
                .putLong(KEY_PIN_LOCKED_UNTIL_MILLIS, lockedUntil)
                .commit();
        return lockedUntil;
    }

    public void resetPinAttempts() {
        prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKED_UNTIL_MILLIS, 0L)
                .commit();
    }

    public long getLastBackgroundMillis() {
        return Math.max(0L, prefs.getLong(KEY_LAST_BACKGROUND_MILLIS, 0L));
    }

    public void setLastBackgroundMillis(long millis) {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_MILLIS, Math.max(0L, millis)).apply();
    }

    public boolean isInitialSetupDismissed() {
        return prefs.getBoolean(KEY_INITIAL_SETUP_DISMISSED, false);
    }

    public void setInitialSetupDismissed(boolean dismissed) {
        prefs.edit().putBoolean(KEY_INITIAL_SETUP_DISMISSED, dismissed).apply();
    }

    public boolean isInitialContactsImportDone() {
        return prefs.getBoolean(KEY_INITIAL_CONTACTS_IMPORT_DONE, false);
    }

    public void markInitialContactsImportDone() {
        prefs.edit().putBoolean(KEY_INITIAL_CONTACTS_IMPORT_DONE, true).apply();
    }

    public boolean isContactSyncEnabled() {
        return prefs.getBoolean(KEY_CONTACT_SYNC_ENABLED, false);
    }

    public void setContactSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CONTACT_SYNC_ENABLED, enabled).apply();
    }

    public long getLastContactSyncMillis() {
        return Math.max(0L, prefs.getLong(KEY_LAST_CONTACT_SYNC_MILLIS, 0L));
    }

    public int getLastContactSyncAdded() {
        return Math.max(0, prefs.getInt(KEY_LAST_CONTACT_SYNC_ADDED, 0));
    }

    @NonNull
    public String getLastContactSyncStatus() {
        String raw = prefs.getString(KEY_LAST_CONTACT_SYNC_STATUS, SYNC_STATUS_NONE);
        return raw == null ? SYNC_STATUS_NONE : raw;
    }

    public void recordContactSyncResult(long millis, int addedCount, @NonNull String status) {
        prefs.edit()
                .putLong(KEY_LAST_CONTACT_SYNC_MILLIS, Math.max(0L, millis))
                .putInt(KEY_LAST_CONTACT_SYNC_ADDED, Math.max(0, addedCount))
                .putString(KEY_LAST_CONTACT_SYNC_STATUS, status)
                .apply();
    }

    public long getLastContactSyncAttemptMillis() {
        return Math.max(0L, prefs.getLong(KEY_LAST_CONTACT_SYNC_ATTEMPT_MILLIS, 0L));
    }

    public void setLastContactSyncAttemptMillis(long millis) {
        prefs.edit().putLong(KEY_LAST_CONTACT_SYNC_ATTEMPT_MILLIS, Math.max(0L, millis)).apply();
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
