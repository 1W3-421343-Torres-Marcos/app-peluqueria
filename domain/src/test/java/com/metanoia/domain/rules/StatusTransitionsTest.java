package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Money;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class StatusTransitionsTest {

    @Test
    public void canTransitionManually_allValidAndInvalidCases_checkedStrictly() {
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, 0L));
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.PENDING, AppointmentStatus.COMPLETED, 0L));
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.PENDING, AppointmentStatus.CANCELLED, 0L));
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.PENDING, AppointmentStatus.NO_SHOW, 0L));
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED, 0L));
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED, 0L));
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.CONFIRMED, AppointmentStatus.NO_SHOW, 0L));

        // Corrección COMPLETED -> CONFIRMED permitida solo sin cobros
        assertTrue(StatusTransitions.canTransitionManually(
                AppointmentStatus.COMPLETED, AppointmentStatus.CONFIRMED, 0L));
        assertFalse(StatusTransitions.canTransitionManually(
                AppointmentStatus.COMPLETED, AppointmentStatus.CONFIRMED, 100L));

        // PAID nunca manual; finales irreversibles; regresiones prohibidas
        assertFalse(StatusTransitions.canTransitionManually(
                AppointmentStatus.COMPLETED, AppointmentStatus.PAID, 0L));
        assertFalse(StatusTransitions.canTransitionManually(
                AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING, 0L));
        assertFalse(StatusTransitions.canTransitionManually(
                AppointmentStatus.PAID, AppointmentStatus.COMPLETED, 0L));
        assertFalse(StatusTransitions.canTransitionManually(
                AppointmentStatus.CANCELLED, AppointmentStatus.PENDING, 0L));
        assertFalse(StatusTransitions.canTransitionManually(
                AppointmentStatus.NO_SHOW, AppointmentStatus.CONFIRMED, 0L));
    }

    @Test
    public void resolveStatusAfterPaymentChange_transitionsCompletedAndPaidAutomatically() {
        AppointmentBalance covered = new AppointmentBalance(
                Money.ofCents(2_000_000L), Money.zero(), Money.ofCents(2_000_000L));
        assertEquals(AppointmentStatus.PAID, StatusTransitions
                .resolveStatusAfterPaymentChange(AppointmentStatus.COMPLETED, covered));

        AppointmentBalance afterReversal = new AppointmentBalance(
                Money.ofCents(2_000_000L), Money.zero(), Money.ofCents(1_000_000L));
        assertEquals(AppointmentStatus.COMPLETED, StatusTransitions
                .resolveStatusAfterPaymentChange(AppointmentStatus.PAID, afterReversal));
    }

    @Test
    public void freezeAndClearCommissions_updatesItemSnapshots() {
        AppointmentItem item = new AppointmentItem.Builder()
                .id("i1")
                .appointmentId("a1")
                .serviceId("s1")
                .serviceNameSnapshot("Corte")
                .priceSnapshot(Money.ofCents(1_500_000L))
                .durationMinSnapshot(30)
                .position(0)
                .build();

        List<AppointmentItem> frozen = StatusTransitions.freezeCommissions(
                Collections.singletonList(item), 4_500);
        assertEquals(Integer.valueOf(4_500),
                frozen.get(0).commissionBasisPointsSnapshot().orElseThrow());

        List<AppointmentItem> cleared = StatusTransitions.clearCommissions(frozen);
        assertFalse(cleared.get(0).commissionBasisPointsSnapshot().isPresent());
    }
}
