package com.metanoia.domain.model;

/**
 * Origen de creación de un cliente en la base del salón.
 */
public enum ClientOrigin {
    MANUAL,
    CONTACT_SYNC;

    public static ClientOrigin fromStorage(String raw) {
        if (CONTACT_SYNC.name().equals(raw)) {
            return CONTACT_SYNC;
        }
        return MANUAL;
    }
}
