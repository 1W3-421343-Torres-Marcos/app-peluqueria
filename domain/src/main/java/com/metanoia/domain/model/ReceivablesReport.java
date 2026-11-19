package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reporte inmutable de saldos pendientes por cobrar sobre turnos {@code COMPLETED}.
 */
public final class ReceivablesReport {

    public static final class ClientReceivableRow {
        private final String clientId;
        private final String clientName;
        private final int appointmentCount;
        private final long balanceCents;
        private final long oldestDay;

        public ClientReceivableRow(
                String clientId,
                String clientName,
                int appointmentCount,
                long balanceCents,
                long oldestDay
        ) {
            this.clientId = Objects.requireNonNull(clientId, "clientId");
            this.clientName = Objects.requireNonNull(clientName, "clientName");
            this.appointmentCount = appointmentCount;
            this.balanceCents = balanceCents;
            this.oldestDay = oldestDay;
        }

        public String clientId() {
            return clientId;
        }

        public String clientName() {
            return clientName;
        }

        public int appointmentCount() {
            return appointmentCount;
        }

        public long balanceCents() {
            return balanceCents;
        }

        public long oldestDay() {
            return oldestDay;
        }
    }

    private final long totalBalanceCents;
    private final int totalAppointmentCount;
    private final int clientCount;
    private final long historicalTotalBalanceCents;
    private final int historicalAppointmentCount;
    private final List<ClientReceivableRow> byClient;

    public ReceivablesReport(
            long historicalTotalBalanceCents,
            int historicalAppointmentCount,
            List<ClientReceivableRow> byClient
    ) {
        this.byClient = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byClient, "byClient")));
        long balance = 0L;
        int appts = 0;
        for (ClientReceivableRow row : this.byClient) {
            balance = Math.addExact(balance, row.balanceCents());
            appts += row.appointmentCount();
        }
        this.totalBalanceCents = balance;
        this.totalAppointmentCount = appts;
        this.clientCount = this.byClient.size();
        this.historicalTotalBalanceCents = Math.max(balance, historicalTotalBalanceCents);
        this.historicalAppointmentCount = Math.max(appts, historicalAppointmentCount);
    }

    public long totalBalanceCents() {
        return totalBalanceCents;
    }

    public int totalAppointmentCount() {
        return totalAppointmentCount;
    }

    public int clientCount() {
        return clientCount;
    }

    public long historicalTotalBalanceCents() {
        return historicalTotalBalanceCents;
    }

    public int historicalAppointmentCount() {
        return historicalAppointmentCount;
    }

    public List<ClientReceivableRow> byClient() {
        return byClient;
    }
}
