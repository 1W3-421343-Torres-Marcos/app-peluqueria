package com.metanoia.domain.rules;

import com.metanoia.domain.model.ContactPhone;

import java.util.List;
import java.util.Optional;

/**
 * Selecciona el teléfono principal de un contacto priorizando el primer móvil válido y luego el primero disponible.
 */
public final class ContactPhoneSelector {

    private ContactPhoneSelector() {
    }

    public static Optional<String> selectPrimaryNormalizedPhone(List<ContactPhone> phones) {
        if (phones == null || phones.isEmpty()) {
            return Optional.empty();
        }
        String fallbackValid = null;
        for (ContactPhone phone : phones) {
            if (phone == null) {
                continue;
            }
            String normalized = PhoneNormalizer.normalize(phone.rawNumber());
            if (!PhoneNormalizer.isValid(normalized)) {
                continue;
            }
            if (phone.isMobile()) {
                return Optional.of(normalized);
            }
            if (fallbackValid == null) {
                fallbackValid = normalized;
            }
        }
        return Optional.ofNullable(fallbackValid);
    }
}
