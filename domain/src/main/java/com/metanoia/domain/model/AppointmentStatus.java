package com.metanoia.domain.model;

/**
 * Estados válidos de un turno en la agenda y su efecto sobre ocupación e historial.
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    PAID,
    CANCELLED,
    NO_SHOW;

    /**
     * Indica si el turno bloquea franja horaria en la agenda del profesional.
     */
    public boolean occupiesSchedule() {
        return this == PENDING || this == CONFIRMED || this == COMPLETED || this == PAID;
    }

    /**
     * Indica si cuenta como visita efectiva del cliente.
     */
    public boolean countsAsVisit() {
        return this == COMPLETED || this == PAID;
    }

    /**
     * Indica si el turno admite edición o reprogramación.
     */
    public boolean isEditable() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Indica si es un estado terminal sin transiciones posteriores.
     */
    public boolean isFinal() {
        return this == CANCELLED || this == NO_SHOW;
    }
}
