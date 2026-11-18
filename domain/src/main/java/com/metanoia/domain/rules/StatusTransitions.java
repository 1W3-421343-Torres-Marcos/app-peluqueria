package com.metanoia.domain.rules;

import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reglas estrictas de transición entre estados de turno y congelado/liberación de comisiones.
 */
public final class StatusTransitions {

    private StatusTransitions() {
    }

    /**
     * Valida transiciones solicitadas manualmente por el usuario. {@link AppointmentStatus#PAID}
     * nunca puede asignarse manualmente, y {@code COMPLETED -> CONFIRMED} requiere saldo sin cobros.
     */
    public static boolean canTransitionManually(
            AppointmentStatus from,
            AppointmentStatus to,
            long paidCents
    ) {
        if (from == null || to == null || from == to) {
            return false;
        }
        if (to == AppointmentStatus.PAID) {
            return false;
        }
        switch (from) {
            case PENDING:
                return to == AppointmentStatus.CONFIRMED
                        || to == AppointmentStatus.COMPLETED
                        || to == AppointmentStatus.CANCELLED
                        || to == AppointmentStatus.NO_SHOW;
            case CONFIRMED:
                return to == AppointmentStatus.COMPLETED
                        || to == AppointmentStatus.CANCELLED
                        || to == AppointmentStatus.NO_SHOW;
            case COMPLETED:
                return to == AppointmentStatus.CONFIRMED && paidCents == 0L;
            case PAID:
            case CANCELLED:
            case NO_SHOW:
            default:
                return false;
        }
    }

    /**
     * Resuelve el paso automático {@code COMPLETED <-> PAID} según el saldo derivado de caja.
     */
    public static AppointmentStatus resolveStatusAfterPaymentChange(
            AppointmentStatus currentStatus,
            AppointmentBalance balance
    ) {
        Objects.requireNonNull(currentStatus, "currentStatus");
        Objects.requireNonNull(balance, "balance");
        if (currentStatus == AppointmentStatus.COMPLETED && balance.isFullyPaid()) {
            return AppointmentStatus.PAID;
        }
        if (currentStatus == AppointmentStatus.PAID && !balance.isFullyPaid()) {
            return AppointmentStatus.COMPLETED;
        }
        return currentStatus;
    }

    /**
     * Congela el porcentaje de comisión actual del profesional en todos los ítems al completar.
     */
    public static List<AppointmentItem> freezeCommissions(
            List<AppointmentItem> items,
            int commissionBasisPoints
    ) {
        Objects.requireNonNull(items, "items");
        List<AppointmentItem> frozen = new ArrayList<>(items.size());
        for (AppointmentItem item : items) {
            frozen.add(item.withFrozenCommission(commissionBasisPoints));
        }
        return frozen;
    }

    /**
     * Libera la comisión congelada al revertir una corrección {@code COMPLETED -> CONFIRMED}.
     */
    public static List<AppointmentItem> clearCommissions(List<AppointmentItem> items) {
        Objects.requireNonNull(items, "items");
        List<AppointmentItem> cleared = new ArrayList<>(items.size());
        for (AppointmentItem item : items) {
            cleared.add(item.withClearedCommission());
        }
        return cleared;
    }
}
