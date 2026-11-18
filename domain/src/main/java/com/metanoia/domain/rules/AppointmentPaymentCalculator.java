package com.metanoia.domain.rules;

import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;

import java.util.List;
import java.util.Objects;

/**
 * Derivación pura de importe neto pagado y saldo adeudado de un turno desde sus movimientos.
 */
public final class AppointmentPaymentCalculator {

    private AppointmentPaymentCalculator() {
    }

    public static Money netPaid(List<CashMovement> movementsForAppointment) {
        if (movementsForAppointment == null || movementsForAppointment.isEmpty()) {
            return Money.zero();
        }
        long netCents = 0L;
        for (CashMovement movement : movementsForAppointment) {
            if (movement == null) {
                continue;
            }
            if (movement.type() == CashMovementType.APPOINTMENT_PAYMENT) {
                netCents = Math.addExact(netCents, movement.amount().cents());
            } else if (movement.type() == CashMovementType.REVERSAL) {
                netCents = Math.subtractExact(netCents, movement.amount().cents());
            }
        }
        return Money.ofCents(Math.max(0L, netCents));
    }

    public static AppointmentBalance balanceOf(
            Appointment appointment,
            List<CashMovement> movementsForAppointment
    ) {
        Objects.requireNonNull(appointment, "appointment");
        return AppointmentBalance.of(appointment, netPaid(movementsForAppointment));
    }
}
