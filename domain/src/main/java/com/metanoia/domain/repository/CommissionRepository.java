package com.metanoia.domain.repository;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.CommissionSettlement;

import java.util.List;

/**
 * Repositorio para consultar liquidaciones históricas de comisiones y persistir nuevas de forma inmutable.
 */
public interface CommissionRepository {

    List<CommissionSettlement> findSettlementsByProfessional(String professionalId);

    boolean hasOverlappingSettlement(String professionalId, long fromDay, long toDay);

    List<Appointment> findPaidAppointmentsInPreviousSettledPeriods(
            String professionalId,
            long beforeDayEpoch
    );

    void saveSettlement(CommissionSettlement settlement);

    void runInTransaction(Runnable action);
}
