package com.metanoia.app.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Preferencias locales del dispositivo (nunca viajan en el backup de datos de negocio).
 */
public final class DevicePrefs {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String PREFS_NAME = "metanoia_device_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

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
