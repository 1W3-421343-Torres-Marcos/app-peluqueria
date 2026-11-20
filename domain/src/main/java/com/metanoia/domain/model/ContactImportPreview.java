package com.metanoia.domain.model;

/**
 * Contadores previos a confirmar la importación masiva desde contactos del teléfono.
 */
public final class ContactImportPreview {

    private final int toImportCount;
    private final int alreadyLoadedCount;
    private final int skippedCount;

    public ContactImportPreview(int toImportCount, int alreadyLoadedCount, int skippedCount) {
        this.toImportCount = toImportCount;
        this.alreadyLoadedCount = alreadyLoadedCount;
        this.skippedCount = skippedCount;
    }

    public int toImportCount() {
        return toImportCount;
    }

    public int alreadyLoadedCount() {
        return alreadyLoadedCount;
    }

    public int skippedCount() {
        return skippedCount;
    }
}
