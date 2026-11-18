package com.metanoia.domain.repository;

/**
 * Disparador asíncrono y no bloqueante para encolar el backup {@code CASH_CLOSE}
 * al completarse un cierre diario de caja.
 */
public interface CashCloseBackupTrigger {

    void triggerCashCloseBackup(long closedDayEpoch);
}
