package com.metanoia.domain.model;

import java.util.Objects;

/**
 * Metadatos inmutables empaquetados en {@code manifest.json} dentro de cada backup ZIP.
 */
public final class BackupManifest {

    public static final int CURRENT_FORMAT = 1;

    private final int format;
    private final int schemaVersion;
    private final int appVersionCode;
    private final String createdAt;
    private final String sha256;
    private final int clientsCount;
    private final int appointmentsCount;
    private final int cashMovementsCount;
    private final BackupTrigger trigger;

    public BackupManifest(
            int format,
            int schemaVersion,
            int appVersionCode,
            String createdAt,
            String sha256,
            int clientsCount,
            int appointmentsCount,
            int cashMovementsCount,
            BackupTrigger trigger
    ) {
        if (format < 1) {
            throw new IllegalArgumentException("format debe ser >= 1");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion debe ser >= 1");
        }
        if (appVersionCode < 1) {
            throw new IllegalArgumentException("appVersionCode debe ser >= 1");
        }
        if (createdAt == null || createdAt.trim().isEmpty()) {
            throw new IllegalArgumentException("createdAt obligatorio");
        }
        if (sha256 == null || sha256.trim().isEmpty()) {
            throw new IllegalArgumentException("sha256 obligatorio");
        }
        if (clientsCount < 0 || appointmentsCount < 0 || cashMovementsCount < 0) {
            throw new IllegalArgumentException("Los conteos no pueden ser negativos");
        }
        this.format = format;
        this.schemaVersion = schemaVersion;
        this.appVersionCode = appVersionCode;
        this.createdAt = createdAt.trim();
        this.sha256 = sha256.trim().toLowerCase(java.util.Locale.ROOT);
        this.clientsCount = clientsCount;
        this.appointmentsCount = appointmentsCount;
        this.cashMovementsCount = cashMovementsCount;
        this.trigger = Objects.requireNonNull(trigger, "trigger");
    }

    public int format() {
        return format;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public int appVersionCode() {
        return appVersionCode;
    }

    public String createdAt() {
        return createdAt;
    }

    public String sha256() {
        return sha256;
    }

    public int clientsCount() {
        return clientsCount;
    }

    public int appointmentsCount() {
        return appointmentsCount;
    }

    public int cashMovementsCount() {
        return cashMovementsCount;
    }

    public BackupTrigger trigger() {
        return trigger;
    }
}
