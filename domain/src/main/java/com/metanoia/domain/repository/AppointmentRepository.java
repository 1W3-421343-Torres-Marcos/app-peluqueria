package com.metanoia.domain.repository;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.RecurrenceRule;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para consultar y persistir turnos de la agenda y sus series recurrentes.
 */
public interface AppointmentRepository {

    Optional<Appointment> findById(String id);

    List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId);

    List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch);

    List<Appointment> findFutureActiveFromSeries(
            String recurrenceRuleId,
            long fromDayEpoch,
            int fromStartMin
    );

    boolean hasFutureActiveForProfessional(String professionalId, long fromDayEpoch);

    void save(Appointment appointment);

    void saveAll(List<Appointment> appointments);

    void saveRecurrenceRule(RecurrenceRule rule);
}
