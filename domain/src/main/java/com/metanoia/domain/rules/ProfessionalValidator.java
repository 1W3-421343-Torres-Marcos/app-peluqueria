package com.metanoia.domain.rules;

import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.SalonServiceRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validaciones puras para profesionales, sus horarios y sus comisiones.
 */
public final class ProfessionalValidator {

    private static final int MIN_NAME_LEN = 2;
    private static final int MAX_NAME_LEN = 80;
    public static final int MAX_COMMISSION_BASIS_POINTS = 10_000;

    private ProfessionalValidator() {
    }

    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= MIN_NAME_LEN && trimmed.length() <= MAX_NAME_LEN;
    }

    public static boolean isValidCommissionBasisPoints(int basisPoints) {
        return basisPoints >= 0 && basisPoints <= MAX_COMMISSION_BASIS_POINTS;
    }

    public static boolean areValidWorkSchedules(
            List<TimeRange> schedules,
            List<TimeRange> salonSchedule
    ) {
        return ScheduleValidator.validateDayRanges(schedules)
                && ScheduleValidator.isWithinSchedule(schedules, salonSchedule);
    }

    public static boolean areAllServicesActive(
            Set<String> serviceIds,
            SalonServiceRepository serviceRepository
    ) {
        if (serviceIds == null) {
            return false;
        }
        for (String serviceId : serviceIds) {
            if (serviceId == null || serviceId.trim().isEmpty()) {
                return false;
            }
            Optional<SalonService> service = serviceRepository.findById(serviceId);
            if (!service.isPresent() || !service.get().isActive()) {
                return false;
            }
        }
        return true;
    }
}
