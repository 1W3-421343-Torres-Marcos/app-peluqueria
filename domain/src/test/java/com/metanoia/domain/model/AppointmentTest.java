package com.metanoia.domain.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AppointmentTest {

    @Test
    public void calculateEndMin_multipleItemsAndSlotRounding_roundsUpToSlot() {
        List<AppointmentItem> items = Arrays.asList(
                item("i1", 1_500_000L, 25),
                item("i2", 1_000_000L, 20)
        );

        assertEquals(45, Appointment.rawItemsDurationMin(items));
        assertEquals(45, Appointment.calculateRoundedDurationMin(items, 15));
        assertEquals(60, Appointment.calculateRoundedDurationMin(items, 20));
        assertEquals(600, Appointment.calculateEndMin(540, items, 20));
    }

    @Test
    public void itemsSubtotalAndTotal_withAdjustment_computesTotalNonNegative() {
        Appointment appointment = new Appointment.Builder()
                .id("apt-1")
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(20_500L)
                .startMin(540)
                .endMin(600)
                .status(AppointmentStatus.PENDING)
                .adjustment(Money.ofCents(-500_000L))
                .items(Arrays.asList(
                        item("i1", 1_500_000L, 30),
                        item("i2", 1_000_000L, 30)
                ))
                .createdAt(1000L)
                .updatedAt(1000L)
                .createdBy("Dueño")
                .build();

        assertEquals(Money.ofCents(2_500_000L), appointment.itemsSubtotal());
        assertEquals(Money.ofCents(2_000_000L), appointment.totalAmount());
        assertEquals(60, appointment.totalDurationMin());
    }

    private static AppointmentItem item(String id, long priceCents, int durationMin) {
        return new AppointmentItem.Builder()
                .id(id)
                .appointmentId("apt-1")
                .serviceId("svc-" + id)
                .serviceNameSnapshot("Servicio " + id)
                .priceSnapshot(Money.ofCents(priceCents))
                .durationMinSnapshot(durationMin)
                .position(0)
                .build();
    }
}
