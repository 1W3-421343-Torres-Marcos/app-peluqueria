package com.metanoia.domain.repository;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.RecurrenceRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para consultar y persistir turnos de la agenda y sus series recurrentes.
 */
public interface AppointmentRepository {

    Optional<Appointment> findById(String id);

    List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId);

    List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch);

    default List<Appointment> findByProfessionalAndDayRange(
            String professionalId,
            long startDayEpoch,
            long endDayEpoch
    ) {
        if (professionalId == null || professionalId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String proId = professionalId.trim();
        List<Appointment> result = new ArrayList<>();
        for (Appointment appointment : findByDayRange(startDayEpoch, endDayEpoch)) {
            if (proId.equals(appointment.professionalId())) {
                result.add(appointment);
            }
        }
        return result;
    }

    default List<Appointment> findByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Appointment> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (id != null && !id.trim().isEmpty()) {
                findById(id.trim()).ifPresent(result::add);
            }
        }
        return result;
    }

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
