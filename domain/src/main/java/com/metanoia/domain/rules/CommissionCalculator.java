package com.metanoia.domain.rules;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.CommissionSettlementEntry;
import com.metanoia.domain.model.CommissionSettlementEntryType;
import com.metanoia.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Calculadora pura de comisiones por profesional y período sobre ítems de turnos {@code PAID}
 * usando porcentajes congelados, prorrateo de descuentos/recargos y ajustes por reversos.
 */
public final class CommissionCalculator {

    private static final BigDecimal BASIS_POINTS_DIVISOR = BigDecimal.valueOf(10_000L);

    private CommissionCalculator() {
    }

    public static final class ItemCalculation {
        private final String itemId;
        private final String serviceName;
        private final Money priceSnapshot;
        private final Money adjustedBase;
        private final int commissionBasisPoints;
        private final Money commission;

        public ItemCalculation(
                String itemId,
                String serviceName,
                Money priceSnapshot,
                Money adjustedBase,
                int commissionBasisPoints,
                Money commission
        ) {
            this.itemId = Objects.requireNonNull(itemId, "itemId");
            this.serviceName = Objects.requireNonNull(serviceName, "serviceName");
            this.priceSnapshot = Objects.requireNonNull(priceSnapshot, "priceSnapshot");
            this.adjustedBase = Objects.requireNonNull(adjustedBase, "adjustedBase");
            this.commissionBasisPoints = commissionBasisPoints;
            this.commission = Objects.requireNonNull(commission, "commission");
        }

        public String itemId() {
            return itemId;
        }

        public String serviceName() {
            return serviceName;
        }

        public Money priceSnapshot() {
            return priceSnapshot;
        }

        public Money adjustedBase() {
            return adjustedBase;
        }

        public int commissionBasisPoints() {
            return commissionBasisPoints;
        }

        public Money commission() {
            return commission;
        }
    }

    public static final class AppointmentCalculation {
        private final Appointment appointment;
        private final Money adjustedBase;
        private final Money commission;
        private final List<ItemCalculation> items;

        public AppointmentCalculation(
                Appointment appointment,
                Money adjustedBase,
                Money commission,
                List<ItemCalculation> items
        ) {
            this.appointment = Objects.requireNonNull(appointment, "appointment");
            this.adjustedBase = Objects.requireNonNull(adjustedBase, "adjustedBase");
            this.commission = Objects.requireNonNull(commission, "commission");
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
        }

        public Appointment appointment() {
            return appointment;
        }

        public Money adjustedBase() {
            return adjustedBase;
        }

        public Money commission() {
            return commission;
        }

        public List<ItemCalculation> items() {
            return items;
        }
    }

    public static final class ReversalAdjustmentLine {
        private final Appointment appointment;
        private final long baseCents;
        private final long commissionCents;

        public ReversalAdjustmentLine(
                Appointment appointment,
                long baseCents,
                long commissionCents
        ) {
            this.appointment = Objects.requireNonNull(appointment, "appointment");
            this.baseCents = baseCents;
            this.commissionCents = commissionCents;
        }

        public Appointment appointment() {
            return appointment;
        }

        public long baseCents() {
            return baseCents;
        }

        public long commissionCents() {
            return commissionCents;
        }
    }

    public static final class PeriodCalculation {
        private final String professionalId;
        private final long fromDay;
        private final long toDay;
        private final boolean overlapsExistingSettlement;
        private final List<AppointmentCalculation> liquidableAppointments;
        private final List<ReversalAdjustmentLine> reversalAdjustments;
        private final List<AppointmentCalculation> pendingCollectionAppointments;
        private final long totalBaseCents;
        private final long totalCommissionCents;
        private final long pendingCollectionBaseCents;
        private final long pendingCollectionCommissionCents;

        public PeriodCalculation(
                String professionalId,
                long fromDay,
                long toDay,
                boolean overlapsExistingSettlement,
                List<AppointmentCalculation> liquidableAppointments,
                List<ReversalAdjustmentLine> reversalAdjustments,
                List<AppointmentCalculation> pendingCollectionAppointments,
                long totalBaseCents,
                long totalCommissionCents,
                long pendingCollectionBaseCents,
                long pendingCollectionCommissionCents
        ) {
            this.professionalId = Objects.requireNonNull(professionalId, "professionalId");
            this.fromDay = fromDay;
            this.toDay = toDay;
            this.overlapsExistingSettlement = overlapsExistingSettlement;
            this.liquidableAppointments = Collections.unmodifiableList(
                    new ArrayList<>(liquidableAppointments));
            this.reversalAdjustments = Collections.unmodifiableList(
                    new ArrayList<>(reversalAdjustments));
            this.pendingCollectionAppointments = Collections.unmodifiableList(
                    new ArrayList<>(pendingCollectionAppointments));
            this.totalBaseCents = totalBaseCents;
            this.totalCommissionCents = totalCommissionCents;
            this.pendingCollectionBaseCents = pendingCollectionBaseCents;
            this.pendingCollectionCommissionCents = pendingCollectionCommissionCents;
        }

        public String professionalId() {
            return professionalId;
        }

        public long fromDay() {
            return fromDay;
        }

        public long toDay() {
            return toDay;
        }

        public boolean overlapsExistingSettlement() {
            return overlapsExistingSettlement;
        }

        public List<AppointmentCalculation> liquidableAppointments() {
            return liquidableAppointments;
        }

        public List<ReversalAdjustmentLine> reversalAdjustments() {
            return reversalAdjustments;
        }

        public List<AppointmentCalculation> pendingCollectionAppointments() {
            return pendingCollectionAppointments;
        }

        public long totalBaseCents() {
            return totalBaseCents;
        }

        public long totalCommissionCents() {
            return totalCommissionCents;
        }

        public long pendingCollectionBaseCents() {
            return pendingCollectionBaseCents;
        }

        public long pendingCollectionCommissionCents() {
            return pendingCollectionCommissionCents;
        }

        public boolean canSettle() {
            return !overlapsExistingSettlement
                    && totalCommissionCents > 0L
                    && (!liquidableAppointments.isEmpty() || !reversalAdjustments.isEmpty());
        }
    }

    public static AppointmentCalculation calculateAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        List<AppointmentItem> rawItems = appointment.items();
        long subtotal = appointment.itemsSubtotal().cents();
        long total = appointment.totalAmount().cents();

        List<ItemCalculation> itemCalcs = new ArrayList<>(rawItems.size());
        long totalCommissionCents = 0L;

        if (total <= 0L) {
            for (AppointmentItem item : rawItems) {
                int bp = item.commissionBasisPointsSnapshot().orElse(0);
                itemCalcs.add(new ItemCalculation(
                        item.id(),
                        item.serviceNameSnapshot(),
                        item.priceSnapshot(),
                        Money.zero(),
                        bp,
                        Money.zero()
                ));
            }
        } else if (subtotal > 0L) {
            BigDecimal subtotalBd = BigDecimal.valueOf(subtotal);
            BigDecimal totalBd = BigDecimal.valueOf(total);
            BigDecimal commDivisor = subtotalBd.multiply(BASIS_POINTS_DIVISOR);

            for (AppointmentItem item : rawItems) {
                int bp = item.commissionBasisPointsSnapshot().orElse(0);
                BigDecimal adjustedNumerator = BigDecimal.valueOf(item.priceSnapshot().cents())
                        .multiply(totalBd);
                long adjBaseCents = adjustedNumerator
                        .divide(subtotalBd, 0, RoundingMode.HALF_UP)
                        .longValueExact();
                long commCents = bp == 0
                        ? 0L
                        : adjustedNumerator.multiply(BigDecimal.valueOf(bp))
                                .divide(commDivisor, 0, RoundingMode.HALF_UP)
                                .longValueExact();
                totalCommissionCents = Math.addExact(totalCommissionCents, commCents);
                itemCalcs.add(new ItemCalculation(
                        item.id(),
                        item.serviceNameSnapshot(),
                        item.priceSnapshot(),
                        Money.ofCents(adjBaseCents),
                        bp,
                        Money.ofCents(commCents)
                ));
            }
        } else {
            int count = rawItems.size();
            long equalShare = total / count;
            long remainder = total % count;
            for (int i = 0; i < count; i++) {
                AppointmentItem item = rawItems.get(i);
                int bp = item.commissionBasisPointsSnapshot().orElse(0);
                long share = i == count - 1 ? equalShare + remainder : equalShare;
                Money adjBase = Money.ofCents(share);
                Money comm = adjBase.percentage(bp);
                totalCommissionCents = Math.addExact(totalCommissionCents, comm.cents());
                itemCalcs.add(new ItemCalculation(
                        item.id(),
                        item.serviceNameSnapshot(),
                        item.priceSnapshot(),
                        adjBase,
                        bp,
                        comm
                ));
            }
        }

        return new AppointmentCalculation(
                appointment,
                Money.ofCents(total),
                Money.ofCents(totalCommissionCents),
                itemCalcs
        );
    }

    public static PeriodCalculation calculatePeriod(
            String professionalId,
            long fromDay,
            long toDay,
            List<Appointment> candidateAppointments,
            List<CommissionSettlement> existingSettlements
    ) {
        if (professionalId == null || professionalId.trim().isEmpty()) {
            throw new IllegalArgumentException("professionalId obligatorio");
        }
        if (fromDay > toDay) {
            throw new IllegalArgumentException(
                    "El período de cálculo es inválido: " + fromDay + " > " + toDay);
        }
        String proId = professionalId.trim();

        List<CommissionSettlement> proSettlements = new ArrayList<>();
        boolean overlaps = false;
        Map<String, Long> netSettledBase = new HashMap<>();
        Map<String, Long> netSettledCommission = new HashMap<>();
        Set<String> hadClawback = new HashSet<>();

        if (existingSettlements != null) {
            for (CommissionSettlement settlement : existingSettlements) {
                if (settlement == null || !proId.equals(settlement.professionalId())) {
                    continue;
                }
                proSettlements.add(settlement);
                if (settlement.overlaps(fromDay, toDay)) {
                    overlaps = true;
                }
                for (CommissionSettlementEntry entry : settlement.entries()) {
                    String aptId = entry.appointmentId();
                    netSettledBase.put(aptId,
                            Math.addExact(netSettledBase.getOrDefault(aptId, 0L), entry.baseCents()));
                    netSettledCommission.put(aptId,
                            Math.addExact(netSettledCommission.getOrDefault(aptId, 0L),
                                    entry.commissionCents()));
                    if (entry.type() == CommissionSettlementEntryType.REVERSAL_ADJUSTMENT) {
                        hadClawback.add(aptId);
                    }
                }
            }
        }

        Map<String, Appointment> uniqueCandidates = new LinkedHashMap<>();
        if (candidateAppointments != null) {
            for (Appointment apt : candidateAppointments) {
                if (apt != null && proId.equals(apt.professionalId())) {
                    uniqueCandidates.put(apt.id(), apt);
                }
            }
        }

        List<Appointment> sorted = new ArrayList<>(uniqueCandidates.values());
        sorted.sort(Comparator
                .comparingLong(Appointment::day)
                .thenComparingInt(Appointment::startMin)
                .thenComparing(Appointment::id));

        List<AppointmentCalculation> liquidable = new ArrayList<>();
        List<ReversalAdjustmentLine> reversals = new ArrayList<>();
        List<AppointmentCalculation> pendingCollection = new ArrayList<>();

        long totalBaseCents = 0L;
        long totalCommissionCents = 0L;
        long pendingBaseCents = 0L;
        long pendingCommissionCents = 0L;

        for (Appointment apt : sorted) {
            long netComm = netSettledCommission.getOrDefault(apt.id(), 0L);
            long netBase = netSettledBase.getOrDefault(apt.id(), 0L);

            if (netComm > 0L) {
                if (apt.status() != AppointmentStatus.PAID) {
                    long adjBase = -netBase;
                    long adjComm = -netComm;
                    reversals.add(new ReversalAdjustmentLine(apt, adjBase, adjComm));
                    totalBaseCents = Math.addExact(totalBaseCents, adjBase);
                    totalCommissionCents = Math.addExact(totalCommissionCents, adjComm);
                }
                continue;
            }

            if (apt.status() == AppointmentStatus.PAID) {
                boolean inTargetRange = apt.day() >= fromDay && apt.day() <= toDay;
                boolean delayedOrRechargedFromSettled = apt.day() < fromDay
                        && (hadClawback.contains(apt.id())
                        || isDayCoveredByAnySettlement(apt.day(), proSettlements));
                if (inTargetRange || delayedOrRechargedFromSettled) {
                    AppointmentCalculation calc = calculateAppointment(apt);
                    liquidable.add(calc);
                    totalBaseCents = Math.addExact(totalBaseCents, calc.adjustedBase().cents());
                    totalCommissionCents = Math.addExact(
                            totalCommissionCents, calc.commission().cents());
                }
            } else if (apt.status() == AppointmentStatus.COMPLETED
                    && apt.day() >= fromDay
                    && apt.day() <= toDay) {
                AppointmentCalculation calc = calculateAppointment(apt);
                pendingCollection.add(calc);
                pendingBaseCents = Math.addExact(pendingBaseCents, calc.adjustedBase().cents());
                pendingCommissionCents = Math.addExact(
                        pendingCommissionCents, calc.commission().cents());
            }
        }

        return new PeriodCalculation(
                proId,
                fromDay,
                toDay,
                overlaps,
                liquidable,
                reversals,
                pendingCollection,
                totalBaseCents,
                totalCommissionCents,
                pendingBaseCents,
                pendingCommissionCents
        );
    }

    private static boolean isDayCoveredByAnySettlement(
            long day,
            List<CommissionSettlement> settlements
    ) {
        for (CommissionSettlement settlement : settlements) {
            if (settlement.coversDay(day)) {
                return true;
            }
        }
        return false;
    }
}
