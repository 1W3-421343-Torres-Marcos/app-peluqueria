package com.metanoia.domain.model;

/**
 * Resultado de una corrida de importación o sincronización automática desde contactos.
 */
public final class ContactSyncOutcome {

    private final int importedCount;
    private final int alreadyLoadedCount;
    private final int skippedCount;
    private final long executedAtMillis;

    public ContactSyncOutcome(
            int importedCount,
            int alreadyLoadedCount,
            int skippedCount,
            long executedAtMillis
    ) {
        this.importedCount = importedCount;
        this.alreadyLoadedCount = alreadyLoadedCount;
        this.skippedCount = skippedCount;
        this.executedAtMillis = executedAtMillis;
    }

    public int importedCount() {
        return importedCount;
    }

    public int alreadyLoadedCount() {
        return alreadyLoadedCount;
    }

    public int skippedCount() {
        return skippedCount;
    }

    public long executedAtMillis() {
        return executedAtMillis;
    }
}
