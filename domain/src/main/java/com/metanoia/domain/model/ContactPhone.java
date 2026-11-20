package com.metanoia.domain.model;

import java.util.Objects;

/**
 * Número telefónico leído desde un contacto del dispositivo con su clasificación móvil.
 */
public final class ContactPhone {

    private final String rawNumber;
    private final boolean mobile;

    public ContactPhone(String rawNumber, boolean mobile) {
        this.rawNumber = Objects.requireNonNull(rawNumber, "rawNumber");
        this.mobile = mobile;
    }

    public String rawNumber() {
        return rawNumber;
    }

    public boolean isMobile() {
        return mobile;
    }
}
