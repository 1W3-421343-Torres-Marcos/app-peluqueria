package com.metanoia.domain.model;

/**
 * Estados válidos de una entrada en la lista de espera.
 */
public enum WaitlistStatus {
    ACTIVE,
    OFFERED,
    FULFILLED,
    EXPIRED,
    CANCELLED;

    public boolean isOpen() {
        return this == ACTIVE || this == OFFERED;
    }
}
