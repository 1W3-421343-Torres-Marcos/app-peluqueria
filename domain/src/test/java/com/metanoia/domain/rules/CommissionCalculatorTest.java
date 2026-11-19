package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.CommissionSettlementEntry;
import com.metanoia.domain.model.CommissionSettlementEntryType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.rules.CommissionCalculator.AppointmentCalculation;
import com.metanoia.domain.rules.CommissionCalculator.PeriodCalculation;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class CommissionCalculatorTest {

    @Test
    public void multiItemFrozenPercentsAndProratedDiscountAndSurcharge() {
        AppointmentItem cut = item("it-1", "apt-1", "Corte", 1_000_000L, 4_000, 0);
        AppointmentItem color = item("it-2", "apt-1", "Color", 2_000_000L, 5_000, 1);
        Appointment discounted = appointment("apt-1", "pro-1", 20_500L,
                AppointmentStatus.PAID, Money.ofCents(-300_000L), Arrays.asList(cut, color));

        AppointmentCalculation discCalc = CommissionCalculator.calculateAppointment(discounted);
        assertEquals(2_700_000L, discCalc.adjustedBase().cents());
        // Corte: 1.000.000 * 2.700.000 / 3.000.000 = 900.000 * 40% = 360.000
        // Color: 2.000.000 * 2.700.000 / 3.000.000 = 1.800.000 * 50% = 900.000
        assertEquals(360_000L, discCalc.items().get(0).commission().cents());
        assertEquals(900_000L, discCalc.items().get(1).commission().cents());
        assertEquals(1_260_000L, discCalc.commission().cents());

        Appointment surcharged = appointment("apt-2", "pro-1", 20_501L,
                AppointmentStatus.PAID, Money.ofCents(300_000L), Arrays.asList(cut, color));
        AppointmentCalculation surCalc = CommissionCalculator.calculateAppointment(surcharged);
        assertEquals(3_300_000L, surCalc.adjustedBase().cents());
        // Corte: 1.100.000 * 40% = 440.000; Color: 2.200.000 * 50% = 1.100.000
        assertEquals(1_540_000L, surCalc.commission().cents());

        Appointment zeroTotal = appointment("apt-3", "pro-1", 20_502L,
                AppointmentStatus.PAID, Money.ofCents(-4_000_000L), Arrays.asList(cut, color));
        AppointmentCalculation zeroCalc = CommissionCalculator.calculateAppointment(zeroTotal);
        assertEquals(0L, zeroCalc.adjustedBase().cents());
        assertEquals(0L, zeroCalc.commission().cents());
    }

    @Test
    public void halfUpRoundingPerItemSumsRoundedValues() {
        // Dos ítems idénticos de $1,00 (100 centavos) al 25,5 % (2550 bp) -> 25,5 centavos c/u -> 26 c/u -> total 52
        AppointmentItem a = item("it-a", "apt-r", "Servicio A", 100L, 2_550, 0);
        AppointmentItem b = item("it-b", "apt-r", "Servicio B", 100L, 2_550, 1);
        Appointment apt = appointment("apt-r", "pro-1", 20_500L,
                AppointmentStatus.PAID, Money.zero(), Arrays.asList(a, b));

        AppointmentCalculation calc = CommissionCalculator.calculateAppointment(apt);
        assertEquals(26L, calc.items().get(0).commission().cents());
        assertEquals(26L, calc.items().get(1).commission().cents());
        assertEquals(52L, calc.commission().cents());
    }

    @Test
    public void onlyPaidCountsTowardSettlement_completedExcludedToPending_cancelledAndNoShowZero() {
        Appointment paid = appointment("apt-paid", "pro-1", 20_500L,
                AppointmentStatus.PAID, Money.zero(),
                Collections.singletonList(item("i1", "apt-paid", "Corte", 1_000_000L, 4_000, 0)));
        Appointment completedDebt = appointment("apt-comp", "pro-1", 20_501L,
                AppointmentStatus.COMPLETED, Money.zero(),
                Collections.singletonList(item("i2", "apt-comp", "Color", 2_000_000L, 5_000, 0)));
        Appointment cancelled = appointment("apt-can", "pro-1", 20_502L,
                AppointmentStatus.CANCELLED, Money.zero(),
                Collections.singletonList(item("i3", "apt-can", "Brushing", 500_000L, 5_000, 0)));
        Appointment noShow = appointment("apt-ns", "pro-1", 20_503L,
                AppointmentStatus.NO_SHOW, Money.zero(),
                Collections.singletonList(item("i4", "apt-ns", "Brushing", 500_000L, 5_000, 0)));
        Appointment otherPro = appointment("apt-other", "pro-2", 20_500L,
                AppointmentStatus.PAID, Money.zero(),
                Collections.singletonList(item("i5", "apt-other", "Corte", 1_000_000L, 5_000, 0)));

        PeriodCalculation period = CommissionCalculator.calculatePeriod(
                "pro-1",
                20_500L,
                20_506L,
                Arrays.asList(paid, completedDebt, cancelled, noShow, otherPro),
                Collections.emptyList()
        );

        assertFalse(period.overlapsExistingSettlement());
        assertEquals(1, period.liquidableAppointments().size());
        assertEquals("apt-paid", period.liquidableAppointments().get(0).appointment().id());
        assertEquals(1_000_000L, period.totalBaseCents());
        assertEquals(400_000L, period.totalCommissionCents());

        assertEquals(1, period.pendingCollectionAppointments().size());
        assertEquals("apt-comp", period.pendingCollectionAppointments().get(0).appointment().id());
        assertEquals(2_000_000L, period.pendingCollectionBaseCents());
        assertEquals(1_000_000L, period.pendingCollectionCommissionCents());
        assertTrue(period.canSettle());
    }

    @Test
    public void preventsDoubleSettlementDetectsOverlapAndHandlesReversalAndRecharge() {
        Appointment apt1 = appointment("apt-1", "pro-1", 20_500L,
                AppointmentStatus.PAID, Money.zero(),
                Collections.singletonList(item("i1", "apt-1", "Corte", 1_000_000L, 4_000, 0)));
        Appointment apt1Reversed = apt1.toBuilder().status(AppointmentStatus.COMPLETED).build();
        Appointment apt2 = appointment("apt-2", "pro-1", 20_510L,
                AppointmentStatus.PAID, Money.zero(),
                Collections.singletonList(item("i2", "apt-2", "Color", 2_000_000L, 5_000, 0)));
        Appointment delayedFromFirstPeriod = appointment("apt-delayed", "pro-1", 20_502L,
                AppointmentStatus.PAID, Money.zero(),
                Collections.singletonList(item("i3", "apt-delayed", "Peinado", 500_000L, 4_000, 0)));

        CommissionSettlement s1 = new CommissionSettlement.Builder()
                .id("set-1")
                .professionalId("pro-1")
                .fromDay(20_500L)
                .toDay(20_506L)
                .baseAmount(Money.ofCents(1_000_000L))
                .commissionAmount(Money.ofCents(400_000L))
                .cashMovementId("cm-1")
                .settledAt(1_000L)
                .operator("Dueño")
                .entries(Collections.singletonList(new CommissionSettlementEntry.Builder()
                        .id("ent-1")
                        .settlementId("set-1")
                        .appointmentId("apt-1")
                        .type(CommissionSettlementEntryType.SETTLED)
                        .baseCents(1_000_000L)
                        .commissionCents(400_000L)
                        .build()))
                .build();

        // Solapamiento con período ya liquidado
        PeriodCalculation overlapping = CommissionCalculator.calculatePeriod(
                "pro-1",
                20_505L,
                20_512L,
                Arrays.asList(apt1, apt2),
                Collections.singletonList(s1)
        );
        assertTrue(overlapping.overlapsExistingSettlement());
        assertFalse(overlapping.canSettle());
        assertEquals(1, overlapping.liquidableAppointments().size()); // apt1 no se vuelve a liquidar

        // Período siguiente: apt1 revertido (COMPLETED) genera ajuste negativo -400.000 y apt-delayed se liquida +200.000
        PeriodCalculation nextWithClawback = CommissionCalculator.calculatePeriod(
                "pro-1",
                20_507L,
                20_513L,
                Arrays.asList(apt1Reversed, apt2, delayedFromFirstPeriod),
                Collections.singletonList(s1)
        );
        assertFalse(nextWithClawback.overlapsExistingSettlement());
        assertEquals(2, nextWithClawback.liquidableAppointments().size()); // apt2 + delayed
        assertEquals(1, nextWithClawback.reversalAdjustments().size());
        assertEquals(-400_000L, nextWithClawback.reversalAdjustments().get(0).commissionCents());
        // Net base = 2.000.000 + 500.000 - 1.000.000 = 1.500.000; net comm = 1.000.000 + 200.000 - 400.000 = 800.000
        assertEquals(1_500_000L, nextWithClawback.totalBaseCents());
        assertEquals(800_000L, nextWithClawback.totalCommissionCents());

        // Luego de liquidar el ajuste negativo (s2), apt-1 vuelve a PAID y se liquida en el 3er período
        CommissionSettlement s2 = new CommissionSettlement.Builder()
                .id("set-2")
                .professionalId("pro-1")
                .fromDay(20_507L)
                .toDay(20_513L)
                .baseAmount(Money.ofCents(1_500_000L))
                .commissionAmount(Money.ofCents(800_000L))
                .cashMovementId("cm-2")
                .settledAt(2_000L)
                .operator("Dueño")
                .entries(Arrays.asList(
                        new CommissionSettlementEntry.Builder()
                                .id("ent-2")
                                .settlementId("set-2")
                                .appointmentId("apt-2")
                                .type(CommissionSettlementEntryType.SETTLED)
                                .baseCents(2_000_000L)
                                .commissionCents(1_000_000L)
                                .build(),
                        new CommissionSettlementEntry.Builder()
                                .id("ent-3")
                                .settlementId("set-2")
                                .appointmentId("apt-delayed")
                                .type(CommissionSettlementEntryType.SETTLED)
                                .baseCents(500_000L)
                                .commissionCents(200_000L)
                                .build(),
                        new CommissionSettlementEntry.Builder()
                                .id("ent-4")
                                .settlementId("set-2")
                                .appointmentId("apt-1")
                                .type(CommissionSettlementEntryType.REVERSAL_ADJUSTMENT)
                                .baseCents(-1_000_000L)
                                .commissionCents(-400_000L)
                                .build()))
                .build();

        PeriodCalculation thirdWhenRepaid = CommissionCalculator.calculatePeriod(
                "pro-1",
                20_514L,
                20_520L,
                Arrays.asList(apt1, apt2, delayedFromFirstPeriod),
                Arrays.asList(s1, s2)
        );
        assertEquals(1, thirdWhenRepaid.liquidableAppointments().size());
        assertEquals("apt-1", thirdWhenRepaid.liquidableAppointments().get(0).appointment().id());
        assertEquals(400_000L, thirdWhenRepaid.totalCommissionCents());
    }

    private static AppointmentItem item(
            String id,
            String aptId,
            String name,
            long priceCents,
            int bp,
            int position
    ) {
        return new AppointmentItem.Builder()
                .id(id)
                .appointmentId(aptId)
                .serviceId("svc-" + id)
                .serviceNameSnapshot(name)
                .priceSnapshot(Money.ofCents(priceCents))
                .durationMinSnapshot(30)
                .commissionBasisPointsSnapshot(bp)
                .position(position)
                .build();
    }

    private static Appointment appointment(
            String id,
            String proId,
            long day,
            AppointmentStatus status,
            Money adjustment,
            java.util.List<AppointmentItem> items
    ) {
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId(proId)
                .day(day)
                .startMin(600)
                .endMin(630)
                .status(status)
                .adjustment(adjustment)
                .items(items)
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }
}
