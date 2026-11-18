package com.metanoia.domain.rules;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.repository.SalonServiceRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Helpers compartidos entre casos de uso de agenda para validar servicios y mensajes de disponibilidad.
 */
public final class AppointmentSupport {

    private AppointmentSupport() {
    }

    public static Result<List<AppointmentItem>> buildFrozenItems(
            String appointmentId,
            List<String> requestedServiceIds,
            List<AppointmentItem> existingItems,
            Professional professional,
            SalonServiceRepository serviceRepository,
            IdGenerator idGenerator
    ) {
        if (requestedServiceIds == null || requestedServiceIds.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Seleccioná al menos un servicio para el turno");
        }
        Map<String, Queue<AppointmentItem>> existingByService = new HashMap<>();
        if (existingItems != null) {
            for (AppointmentItem existingItem : existingItems) {
                existingByService
                        .computeIfAbsent(existingItem.serviceId(), ignored -> new ArrayDeque<>())
                        .add(existingItem);
            }
        }

        List<AppointmentItem> built = new ArrayList<>(requestedServiceIds.size());
        for (int pos = 0; pos < requestedServiceIds.size(); pos++) {
            String rawServiceId = requestedServiceIds.get(pos);
            if (rawServiceId == null || rawServiceId.trim().isEmpty()) {
                return Result.failure(DomainError.VALIDATION_ERROR, "Servicio inválido");
            }
            String serviceId = rawServiceId.trim();
            if (!professional.serviceIds().contains(serviceId)) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El profesional no está habilitado para todos los servicios elegidos");
            }

            Queue<AppointmentItem> reusableQueue = existingByService.get(serviceId);
            if (reusableQueue != null && !reusableQueue.isEmpty()) {
                AppointmentItem preserved = reusableQueue.poll();
                built.add(preserved.toBuilder()
                        .appointmentId(appointmentId)
                        .position(pos)
                        .build());
                continue;
            }

            Optional<SalonService> found = serviceRepository.findById(serviceId);
            if (!found.isPresent() || !found.get().isActive()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Solo se pueden agendar servicios existentes y activos");
            }
            SalonService service = found.get();
            built.add(new AppointmentItem.Builder()
                    .id(idGenerator.nextId())
                    .appointmentId(appointmentId)
                    .serviceId(service.id())
                    .serviceNameSnapshot(service.name())
                    .priceSnapshot(service.price())
                    .durationMinSnapshot(service.durationMin())
                    .position(pos)
                    .build());
        }
        return Result.success(built);
    }

    public static Set<String> collectColorServiceIds(
            List<AppointmentItem> items,
            SalonServiceRepository serviceRepository
    ) {
        Set<String> colorIds = new HashSet<>();
        if (items == null) {
            return colorIds;
        }
        for (AppointmentItem item : items) {
            Optional<SalonService> found = serviceRepository.findById(item.serviceId());
            if (found.isPresent() && found.get().isColorService()) {
                colorIds.add(item.serviceId());
            }
        }
        return colorIds;
    }

    public static List<AppointmentItem> attachColorFormulaToColorItems(
            List<AppointmentItem> items,
            Set<String> colorServiceIds,
            String colorFormulaId
    ) {
        List<AppointmentItem> updated = new ArrayList<>(items.size());
        for (AppointmentItem item : items) {
            if (colorServiceIds.contains(item.serviceId())) {
                updated.add(item.toBuilder().colorFormulaId(colorFormulaId).build());
            } else {
                updated.add(item);
            }
        }
        return updated;
    }

    public static String unavailabilityMessage(AvailabilityChecker.UnavailabilityReason reason) {
        switch (reason) {
            case PROFESSIONAL_INACTIVE:
                return "El profesional está inactivo";
            case INVALID_TIME_RANGE:
                return "El horario elegido excede los límites del día";
            case OUTSIDE_SALON_HOURS:
                return "El turno cae fuera del horario de atención del salón";
            case OUTSIDE_PROFESSIONAL_HOURS:
                return "El turno cae fuera del horario laboral del profesional";
            case PROFESSIONAL_DAY_OFF:
                return "El profesional tiene el día libre seleccionado";
            case BLOCKED_BY_SCHEDULE_BLOCK:
                return "Existe un bloqueo de agenda en esa fecha u horario";
            case OVERLAPS_EXISTING_APPOINTMENT:
                return "El horario se solapa con otro turno del profesional";
            case NONE:
            default:
                return "";
        }
    }
}
