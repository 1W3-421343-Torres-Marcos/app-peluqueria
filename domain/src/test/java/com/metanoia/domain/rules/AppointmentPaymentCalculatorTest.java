package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AppointmentPaymentCalculatorTest {

    @Test
    public void netPaidAndBalance_combinePaymentsAndReversals() {
        Appointment apt = sampleCompletedAppointment(20_000L);
        CashMovement payCash = movement("m-1", CashMovementType.APPOINTMENT_PAYMENT,
                PaymentMethod.CASH, 12_000L, null);
        CashMovement payMp = movement("m-2", CashMovementType.APPOINTMENT_PAYMENT,
                PaymentMethod.MERCADO_PAGO, 8_000L, null);
        CashMovement reversal = movement("m-3", CashMovementType.REVERSAL,
                PaymentMethod.MERCADO_PAGO, 8_000L, "m-2");

        AppointmentBalance balance = AppointmentPaymentCalculator.balanceOf(
                apt, Arrays.asList(payCash, payMp, reversal));

        assertEquals(20_000L, balance.total().cents());
        assertEquals(12_000L, balance.paid().cents());
        assertEquals(8_000L, balance.balance().cents());
        assertFalse(balance.isFullyPaid());
        assertTrue(balance.hasPayments());
    }

    private static Appointment sampleCompletedAppointment(long priceCents) {
        AppointmentItem item = new AppointmentItem.Builder()
                .id("item-1")
                .appointmentId("apt-1")
                .serviceId("svc-1")
                .serviceNameSnapshot("Corte")
                .priceSnapshot(Money.ofCents(priceCents))
                .durationMinSnapshot(30)
                .position(0)
                .build();
        return new Appointment.Builder()
                .id("apt-1")
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(20_500L)
                .startMin(600)
                .endMin(630)
                .status(AppointmentStatus.COMPLETED)
                .items(Collections.singletonList(item))
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }

    private static CashMovement movement(
            String id,
            CashMovementType type,
            PaymentMethod method,
            long cents,
            String reversesId
    ) {
        return new CashMovement.Builder()
                .id(id)
                .day(20_500L)
                .timestamp(2_000L)
                .type(type)
                .method(method)
                .amount(Money.ofCents(cents))
                .concept("Cobro")
                .appointmentId("apt-1")
                .clientId("cli-1")
                .reversesMovementId(reversesId)
                .operator("Dueño")
                .build();
    }
}
