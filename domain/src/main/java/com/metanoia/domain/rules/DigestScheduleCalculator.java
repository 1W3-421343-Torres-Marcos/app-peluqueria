package com.metanoia.domain.rules;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Calcula el retardo inicial en milisegundos hasta la próxima ejecución diaria del aviso local.
 */
public final class DigestScheduleCalculator {

    private static final long MIN_DELAY_MILLIS = 1_000L;

    private DigestScheduleCalculator() {
    }

    public static long initialDelayMillis(
            long nowMillis,
            ZoneId zoneId,
            int targetHour,
            int targetMinute
    ) {
        Objects.requireNonNull(zoneId, "zoneId");
        int hour = Math.max(0, Math.min(23, targetHour));
        int minute = Math.max(0, Math.min(59, targetMinute));

        ZonedDateTime nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId);
        ZonedDateTime nextTrigger = nowZdt
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);
        if (!nextTrigger.isAfter(nowZdt)) {
            nextTrigger = nextTrigger.plusDays(1L);
        }
        return Math.max(MIN_DELAY_MILLIS, Duration.between(nowZdt, nextTrigger).toMillis());
    }
}
