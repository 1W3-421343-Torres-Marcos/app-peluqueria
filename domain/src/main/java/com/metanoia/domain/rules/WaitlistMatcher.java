package com.metanoia.domain.rules;

import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reglas puras para encontrar y ordenar candidatos compatibles de la lista de espera
 * cuando un turno se cancela o reprograma liberando un hueco.
 */
public final class WaitlistMatcher {

    private WaitlistMatcher() {
    }

    public static List<MatchedCandidate> match(
            FreedSlot slot,
            Professional slotProfessional,
            long todayEpochDay,
            int slotMinutes,
            List<WaitlistEntry> entries,
            Map<String, Client> clientsById,
            Map<String, SalonService> servicesById
    ) {
        if (slot == null || slotProfessional == null || !slotProfessional.isActive()
                || slot.dayEpoch() < todayEpochDay || slot.durationMin() <= 0
                || slotMinutes <= 0 || entries == null || entries.isEmpty()
                || clientsById == null || servicesById == null) {
            return Collections.emptyList();
        }
        if (!slotProfessional.id().equals(slot.professionalId())) {
            return Collections.emptyList();
        }

        List<MatchedCandidate> candidates = new ArrayList<>();
        for (WaitlistEntry entry : entries) {
            if (entry == null || entry.status() != WaitlistStatus.ACTIVE) {
                continue;
            }
            if (entry.validUntilDay() < todayEpochDay) {
                continue;
            }
            Client client = clientsById.get(entry.clientId());
            if (client == null || !client.isActive()) {
                continue;
            }
            if (entry.professionalId().isPresent()
                    && !entry.professionalId().get().equals(slot.professionalId())) {
                continue;
            }
            if (!entry.matchesDay(slot.dayEpoch())) {
                continue;
            }

            List<SalonService> resolvedServices = resolveSupportedServices(
                    entry.serviceIds(), slotProfessional, servicesById);
            if (resolvedServices == null) {
                continue;
            }

            int rawDuration = 0;
            for (SalonService service : resolvedServices) {
                rawDuration += service.durationMin();
            }
            int requiredDuration = ((rawDuration + slotMinutes - 1) / slotMinutes) * slotMinutes;
            if (requiredDuration <= 0 || requiredDuration > slot.durationMin()) {
                continue;
            }
            if (entry.fromMin() > slot.startMin()
                    || slot.startMin() + requiredDuration > entry.toMin()) {
                continue;
            }

            boolean exactProMatch = entry.professionalId().isPresent();
            candidates.add(new MatchedCandidate(
                    entry, client, resolvedServices, requiredDuration, exactProMatch));
        }

        candidates.sort(Comparator
                .comparing(MatchedCandidate::exactProfessionalMatch).reversed()
                .thenComparingLong(candidate -> candidate.entry().createdAt())
                .thenComparingInt(candidate -> candidate.client().noShowCount())
                .thenComparing(candidate -> candidate.entry().id()));
        return Collections.unmodifiableList(candidates);
    }

    private static List<SalonService> resolveSupportedServices(
            List<String> requestedServiceIds,
            Professional slotProfessional,
            Map<String, SalonService> servicesById
    ) {
        if (requestedServiceIds.isEmpty()
                || !slotProfessional.serviceIds().containsAll(requestedServiceIds)) {
            return null;
        }
        List<SalonService> resolved = new ArrayList<>(requestedServiceIds.size());
        for (String serviceId : requestedServiceIds) {
            SalonService service = servicesById.get(serviceId);
            if (service == null || !service.isActive()) {
                return null;
            }
            resolved.add(service);
        }
        return resolved;
    }

    /**
     * Hueco de agenda liberado tras una cancelación o reprogramación.
     */
    public static final class FreedSlot {
        private final String professionalId;
        private final long dayEpoch;
        private final int startMin;
        private final int endMin;

        public FreedSlot(String professionalId, long dayEpoch, int startMin, int endMin) {
            if (professionalId == null || professionalId.trim().isEmpty()) {
                throw new IllegalArgumentException("professionalId obligatorio");
            }
            if (startMin < 0 || endMin <= startMin || endMin > 1440) {
                throw new IllegalArgumentException("Rango horario inválido: " + startMin + ".." + endMin);
            }
            this.professionalId = professionalId.trim();
            this.dayEpoch = dayEpoch;
            this.startMin = startMin;
            this.endMin = endMin;
        }

        public String professionalId() {
            return professionalId;
        }

        public long dayEpoch() {
            return dayEpoch;
        }

        public int startMin() {
            return startMin;
        }

        public int endMin() {
            return endMin;
        }

        public int durationMin() {
            return endMin - startMin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FreedSlot)) {
                return false;
            }
            FreedSlot that = (FreedSlot) o;
            return dayEpoch == that.dayEpoch
                    && startMin == that.startMin
                    && endMin == that.endMin
                    && professionalId.equals(that.professionalId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(professionalId, dayEpoch, startMin, endMin);
        }
    }

    /**
     * Candidato compatible con el hueco liberado, listo para ofrecer o asignar.
     */
    public static final class MatchedCandidate {
        private final WaitlistEntry entry;
        private final Client client;
        private final List<SalonService> services;
        private final int requiredDurationMin;
        private final boolean exactProfessionalMatch;

        public MatchedCandidate(
                WaitlistEntry entry,
                Client client,
                List<SalonService> services,
                int requiredDurationMin,
                boolean exactProfessionalMatch
        ) {
            this.entry = Objects.requireNonNull(entry, "entry");
            this.client = Objects.requireNonNull(client, "client");
            this.services = Collections.unmodifiableList(new ArrayList<>(services));
            this.requiredDurationMin = requiredDurationMin;
            this.exactProfessionalMatch = exactProfessionalMatch;
        }

        public WaitlistEntry entry() {
            return entry;
        }

        public Client client() {
            return client;
        }

        public List<SalonService> services() {
            return services;
        }

        public int requiredDurationMin() {
            return requiredDurationMin;
        }

        public boolean exactProfessionalMatch() {
            return exactProfessionalMatch;
        }
    }
}
