package com.metanoia.app.ui.lock;

import androidx.annotation.NonNull;

import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.rules.PinLockPolicy;

import java.util.Objects;

/**
 * Controla el estado de bloqueo de sesión al iniciar la app o volver desde segundo plano.
 */
public final class AppLockController {

    private final DevicePrefs devicePrefs;
    private volatile boolean sessionUnlocked;

    public AppLockController(@NonNull DevicePrefs devicePrefs) {
        this.devicePrefs = Objects.requireNonNull(devicePrefs, "devicePrefs");
        this.sessionUnlocked = !devicePrefs.isPinEnabled();
    }

    public boolean isLockEnabled() {
        return devicePrefs.isPinEnabled();
    }

    public void onAppBackgrounded(long nowMillis) {
        if (devicePrefs.isPinEnabled() && sessionUnlocked) {
            devicePrefs.setLastBackgroundMillis(nowMillis);
        }
    }

    public boolean checkNeedsLock(long nowMillis) {
        if (!devicePrefs.isPinEnabled()) {
            sessionUnlocked = true;
            return false;
        }
        boolean needs = PinLockPolicy.shouldLockOnResume(
                true,
                sessionUnlocked,
                devicePrefs.getLastBackgroundMillis(),
                nowMillis,
                devicePrefs.getLockTimeoutMinutes());
        if (needs) {
            sessionUnlocked = false;
        } else if (sessionUnlocked && devicePrefs.getLastBackgroundMillis() > 0L) {
            devicePrefs.setLastBackgroundMillis(0L);
        }
        return needs;
    }

    public void markUnlocked() {
        sessionUnlocked = true;
        devicePrefs.setLastBackgroundMillis(0L);
        devicePrefs.resetPinAttempts();
    }

    public void onPinEnabledNow() {
        markUnlocked();
    }
}
