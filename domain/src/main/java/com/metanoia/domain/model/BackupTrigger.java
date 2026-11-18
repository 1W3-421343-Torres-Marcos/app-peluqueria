package com.metanoia.domain.model;

/**
 * Origen o disparador de una copia de seguridad de Metanoia.
 */
public enum BackupTrigger {
    AUTO,
    MANUAL,
    PRE_RESTORE,
    CASH_CLOSE,
    ON_BACKGROUND
}
