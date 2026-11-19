package com.metanoia.domain.rules;

/**
 * Reglas puras de bloqueo por PIN al iniciar o volver de segundo plano y espera progresiva ante
 * intentos fallidos.
 */
public final class PinLockPolicy {

    public static final int MAX_FREE_ATTEMPTS = 5;
    public static final long BASE_COOLDOWN_MS = 30_000L;
    public static final long MAX_COOLDOWN_MS = 900_000L;
    public static final int DEFAULT_TIMEOUT_MINUTES = 5;

    private PinLockPolicy() {
    }

    public static int normalizeTimeoutMinutes(int minutes) {
        if (minutes == 0 || minutes == 1 || minutes == 5 || minutes == 15) {
            return minutes;
        }
        return DEFAULT_TIMEOUT_MINUTES;
    }

    public static boolean shouldLockOnResume(
            boolean pinEnabled,
            boolean currentlyUnlocked,
            long backgroundedAtMillis,
            long nowMillis,
            int timeoutMinutes
    ) {
        if (!pinEnabled) {
            return false;
        }
        if (!currentlyUnlocked) {
            return true;
        }
        if (backgroundedAtMillis <= 0L) {
            return false;
        }
        int normalized = normalizeTimeoutMinutes(timeoutMinutes);
        if (normalized == 0) {
            return true;
        }
        long elapsed = nowMillis - backgroundedAtMillis;
        return elapsed >= normalized * 60_000L;
    }

    public static long cooldownDurationMs(int failedAttempts) {
        if (failedAttempts < MAX_FREE_ATTEMPTS) {
            return 0L;
        }
        long steps = (long) (failedAttempts - MAX_FREE_ATTEMPTS + 1);
        return Math.min(MAX_COOLDOWN_MS, steps * BASE_COOLDOWN_MS);
    }

    public static long remainingLockoutMs(long lockedUntilMillis, long nowMillis) {
        return lockedUntilMillis <= nowMillis ? 0L : lockedUntilMillis - nowMillis;
    }
}
