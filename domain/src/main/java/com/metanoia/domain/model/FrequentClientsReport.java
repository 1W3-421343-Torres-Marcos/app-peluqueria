package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reporte inmutable de clientes frecuentes ordenados por visitas y por gasto en el período.
 */
public final class FrequentClientsReport {

    public static final class ClientRow {
        private final String clientId;
        private final String clientName;
        private final String phone;
        private final int visitCount;
        private final long totalSpentCents;
        private final long lastVisitDay;

        public ClientRow(
                String clientId,
                String clientName,
                String phone,
                int visitCount,
                long totalSpentCents,
                long lastVisitDay
        ) {
            this.clientId = Objects.requireNonNull(clientId, "clientId");
            this.clientName = Objects.requireNonNull(clientName, "clientName");
            this.phone = phone == null ? "" : phone;
            this.visitCount = visitCount;
            this.totalSpentCents = totalSpentCents;
            this.lastVisitDay = lastVisitDay;
        }

        public String clientId() {
            return clientId;
        }

        public String clientName() {
            return clientName;
        }

        public String phone() {
            return phone;
        }

        public int visitCount() {
            return visitCount;
        }

        public long totalSpentCents() {
            return totalSpentCents;
        }

        public long lastVisitDay() {
            return lastVisitDay;
        }
    }

    private final int limit;
    private final List<ClientRow> topByVisits;
    private final List<ClientRow> topBySpend;

    public FrequentClientsReport(
            int limit,
            List<ClientRow> topByVisits,
            List<ClientRow> topBySpend
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit debe ser positivo: " + limit);
        }
        this.limit = limit;
        this.topByVisits = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(topByVisits, "topByVisits")));
        this.topBySpend = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(topBySpend, "topBySpend")));
    }

    public int limit() {
        return limit;
    }

    public List<ClientRow> topByVisits() {
        return topByVisits;
    }

    public List<ClientRow> topBySpend() {
        return topBySpend;
    }
}
