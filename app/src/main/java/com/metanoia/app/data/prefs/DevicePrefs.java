package com.metanoia.app.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferencias locales del dispositivo (nunca viajan en el backup de datos de negocio).
 */
public final class DevicePrefs {

    private static final String PREFS_NAME = "metanoia_device_prefs";

    private final SharedPreferences prefs;

    public DevicePrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    SharedPreferences raw() {
        return prefs;
    }
}
