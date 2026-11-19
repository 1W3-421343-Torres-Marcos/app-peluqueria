package com.metanoia.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reporte inmutable de rendimiento por profesional (turnos, facturación, ticket, ausencias y comisiones).
 */
public final class ProfessionalPerformanceReport {

    public static final class ProfessionalRow {
        private final String professionalId;
        private final String professionalName;
        private final int completedAppointments;
        private final long billedCents;
        private final long averageTicketCents;
        private final int noShowCount;
        private final long commissionCents;

        public ProfessionalRow(
                String professionalId,
                String professionalName,
                int completedAppointments,
                long billedCents,
                int noShowCount,
                long commissionCents
        ) {
            this.professionalId = Objects.requireNonNull(professionalId, "professionalId");
            this.professionalName = Objects.requireNonNull(professionalName, "professionalName");
            this.completedAppointments = completedAppointments;
            this.billedCents = billedCents;
            this.averageTicketCents = computeAverageTicketCents(billedCents, completedAppointments);
            this.noShowCount = noShowCount;
            this.commissionCents = commissionCents;
        }

        public String professionalId() {
            return professionalId;
        }

        public String professionalName() {
            return professionalName;
        }

        public int completedAppointments() {
            return completedAppointments;
        }

        public long billedCents() {
            return billedCents;
        }

        public long averageTicketCents() {
            return averageTicketCents;
        }

        public int noShowCount() {
            return noShowCount;
        }

        public long commissionCents() {
            return commissionCents;
        }
    }

    private final int totalCompletedAppointments;
    private final long totalBilledCents;
    private final long overallAverageTicketCents;
    private final int totalNoShowCount;
    private final long totalCommissionCents;
    private final List<ProfessionalRow> rows;

    public ProfessionalPerformanceReport(List<ProfessionalRow> rows) {
        this.rows = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(rows, "rows")));
        int completed = 0;
        long billed = 0L;
        int noShows = 0;
        long commissions = 0L;
        for (ProfessionalRow row : this.rows) {
            completed += row.completedAppointments();
            billed = Math.addExact(billed, row.billedCents());
            noShows += row.noShowCount();
            commissions = Math.addExact(commissions, row.commissionCents());
        }
        this.totalCompletedAppointments = completed;
        this.totalBilledCents = billed;
        this.overallAverageTicketCents = computeAverageTicketCents(billed, completed);
        this.totalNoShowCount = noShows;
        this.totalCommissionCents = commissions;
    }

    public static long computeAverageTicketCents(long billedCents, int completedCount) {
        if (completedCount <= 0) {
            return 0L;
        }
        return BigDecimal.valueOf(billedCents)
                .divide(BigDecimal.valueOf(completedCount), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public int totalCompletedAppointments() {
        return totalCompletedAppointments;
    }

    public long totalBilledCents() {
        return totalBilledCents;
    }

    public long overallAverageTicketCents() {
        return overallAverageTicketCents;
    }

    public int totalNoShowCount() {
        return totalNoShowCount;
    }

    public long totalCommissionCents() {
        return totalCommissionCents;
    }

    public List<ProfessionalRow> rows() {
        return rows;
    }
}
