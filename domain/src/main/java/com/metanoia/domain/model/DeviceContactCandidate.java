package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Contacto leído en solo lectura desde la agenda del dispositivo candidato a convertirse en cliente.
 */
public final class DeviceContactCandidate {

    private final String lookupKey;
    private final String displayName;
    private final List<ContactPhone> phones;
    private final String rawBirthday;

    public DeviceContactCandidate(
            String lookupKey,
            String displayName,
            List<ContactPhone> phones,
            String rawBirthday
    ) {
        this.lookupKey = lookupKey == null ? "" : lookupKey.trim();
        this.displayName = displayName == null ? "" : displayName;
        this.phones = phones == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(phones));
        this.rawBirthday = rawBirthday == null || rawBirthday.trim().isEmpty()
                ? null
                : rawBirthday.trim();
    }

    public String lookupKey() {
        return lookupKey;
    }

    public String displayName() {
        return displayName;
    }

    public List<ContactPhone> phones() {
        return phones;
    }

    public Optional<String> rawBirthday() {
        return Optional.ofNullable(rawBirthday);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeviceContactCandidate)) {
            return false;
        }
        DeviceContactCandidate that = (DeviceContactCandidate) o;
        return Objects.equals(lookupKey, that.lookupKey)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(phones, that.phones)
                && Objects.equals(rawBirthday, that.rawBirthday);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupKey, displayName, phones, rawBirthday);
    }
}
