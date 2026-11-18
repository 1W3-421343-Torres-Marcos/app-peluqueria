package com.metanoia.domain.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Fuente de tiempo inyectable para evitar llamadas directas al reloj del sistema en el dominio.
 */
public interface Clock {

    ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    long nowMillis();

    long todayEpochDay();

    int currentMinuteOfDay();

    ZoneId zoneId();

    /**
     * Implementación estándar del reloj del sistema fijada en la zona horaria de Argentina.
     */
    static Clock systemArgentina() {
        return SystemArgentinaClock.INSTANCE;
    }

    final class SystemArgentinaClock implements Clock {

        private static final SystemArgentinaClock INSTANCE = new SystemArgentinaClock();

        private SystemArgentinaClock() {
        }

        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public long todayEpochDay() {
            return LocalDate.now(ARGENTINA_ZONE).toEpochDay();
        }

        @Override
        public int currentMinuteOfDay() {
            LocalTime time = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(nowMillis()), ARGENTINA_ZONE).toLocalTime();
            return time.getHour() * 60 + time.getMinute();
        }

        @Override
        public ZoneId zoneId() {
            return ARGENTINA_ZONE;
        }
    }
}
