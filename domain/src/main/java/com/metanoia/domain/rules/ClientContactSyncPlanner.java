package com.metanoia.domain.rules;

import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientOrigin;
import com.metanoia.domain.model.ContactBirthday;
import com.metanoia.domain.model.ContactImportPreview;
import com.metanoia.domain.model.DeviceContactCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Planifica de forma pura qué contactos deben crearse sin duplicar ni sobrescribir clientes existentes.
 */
public final class ClientContactSyncPlanner {

    private ClientContactSyncPlanner() {
    }

    public static PlanResult plan(
            List<DeviceContactCandidate> rawCandidates,
            List<Client> existingClients,
            int maxBirthYear,
            long nowMillis,
            IdGenerator idGenerator
    ) {
        DeduplicatedDeviceSet deviceSet = deduplicateDeviceContacts(rawCandidates);
        Set<String> existingPhones = new HashSet<>();
        Set<String> existingLookupKeys = new HashSet<>();
        indexExistingClients(existingClients, existingPhones, existingLookupKeys);

        List<Client> newClients = new ArrayList<>();
        int alreadyLoaded = 0;

        for (ResolvedCandidate candidate : deviceSet.uniqueCandidates) {
            if (isAlreadyKnown(candidate, existingPhones, existingLookupKeys)) {
                alreadyLoaded++;
                continue;
            }
            Client created = buildNewClient(candidate, maxBirthYear, nowMillis, idGenerator);
            newClients.add(created);
            existingPhones.add(candidate.normalizedPhone);
            if (candidate.lookupKey != null) {
                existingLookupKeys.add(candidate.lookupKey);
            }
        }

        ContactImportPreview preview = new ContactImportPreview(
                newClients.size(), alreadyLoaded, deviceSet.skippedCount);
        return new PlanResult(newClients, preview);
    }

    private static DeduplicatedDeviceSet deduplicateDeviceContacts(
            List<DeviceContactCandidate> rawCandidates
    ) {
        if (rawCandidates == null || rawCandidates.isEmpty()) {
            return new DeduplicatedDeviceSet(Collections.emptyList(), 0);
        }
        Map<String, ResolvedCandidate> byPhone = new LinkedHashMap<>();
        Map<String, ResolvedCandidate> byLookup = new HashMap<>();
        int skipped = 0;

        for (DeviceContactCandidate raw : rawCandidates) {
            if (raw == null) {
                continue;
            }
            Optional<String> selectedPhone = ContactPhoneSelector.selectPrimaryNormalizedPhone(raw.phones());
            if (!selectedPhone.isPresent()) {
                skipped++;
                continue;
            }
            String normalizedPhone = selectedPhone.get();
            String lookupKey = raw.lookupKey().isEmpty() ? null : raw.lookupKey();
            ResolvedCandidate incoming = new ResolvedCandidate(
                    lookupKey, raw.displayName(), normalizedPhone, raw.rawBirthday().orElse(null));

            ResolvedCandidate existingByKey = lookupKey == null ? null : byLookup.get(lookupKey);
            ResolvedCandidate existingByPhone = byPhone.get(normalizedPhone);
            ResolvedCandidate existing = existingByKey != null ? existingByKey : existingByPhone;

            if (existing != null) {
                existing.mergeFrom(incoming);
                if (lookupKey != null) {
                    byLookup.putIfAbsent(lookupKey, existing);
                }
                continue;
            }

            byPhone.put(normalizedPhone, incoming);
            if (lookupKey != null) {
                byLookup.put(lookupKey, incoming);
            }
        }

        return new DeduplicatedDeviceSet(new ArrayList<>(byPhone.values()), skipped);
    }

    private static void indexExistingClients(
            List<Client> existingClients,
            Set<String> existingPhones,
            Set<String> existingLookupKeys
    ) {
        if (existingClients == null) {
            return;
        }
        for (Client client : existingClients) {
            if (client == null) {
                continue;
            }
            String normalized = PhoneNormalizer.normalize(client.phone());
            if (!normalized.isEmpty()) {
                existingPhones.add(normalized);
            } else if (!client.phone().isEmpty()) {
                existingPhones.add(client.phone());
            }
            client.contactLookupKey().ifPresent(existingLookupKeys::add);
        }
    }

    private static boolean isAlreadyKnown(
            ResolvedCandidate candidate,
            Set<String> existingPhones,
            Set<String> existingLookupKeys
    ) {
        if (existingPhones.contains(candidate.normalizedPhone)) {
            return true;
        }
        return candidate.lookupKey != null && existingLookupKeys.contains(candidate.lookupKey);
    }

    private static Client buildNewClient(
            ResolvedCandidate candidate,
            int maxBirthYear,
            long nowMillis,
            IdGenerator idGenerator
    ) {
        String name = ContactClientNameResolver.resolve(
                candidate.displayName, candidate.normalizedPhone);
        Optional<ContactBirthday> birthday = ContactBirthdayParser.parse(
                candidate.rawBirthday, maxBirthYear);
        return new Client.Builder()
                .id(idGenerator.nextId())
                .name(name)
                .nameNormalized(TextNormalizer.normalize(name))
                .phone(candidate.normalizedPhone)
                .birthDay(birthday.map(ContactBirthday::day).orElse(null))
                .birthMonth(birthday.map(ContactBirthday::month).orElse(null))
                .birthYear(birthday.flatMap(ContactBirthday::year).orElse(null))
                .notes("")
                .noShowCount(0)
                .contactLookupKey(candidate.lookupKey)
                .origin(ClientOrigin.CONTACT_SYNC)
                .createdFromContactAt(nowMillis)
                .active(true)
                .createdAt(nowMillis)
                .updatedAt(nowMillis)
                .build();
    }

    public static final class PlanResult {
        private final List<Client> newClients;
        private final ContactImportPreview preview;

        PlanResult(List<Client> newClients, ContactImportPreview preview) {
            this.newClients = Collections.unmodifiableList(newClients);
            this.preview = preview;
        }

        public List<Client> newClients() {
            return newClients;
        }

        public ContactImportPreview preview() {
            return preview;
        }
    }

    private static final class DeduplicatedDeviceSet {
        final List<ResolvedCandidate> uniqueCandidates;
        final int skippedCount;

        DeduplicatedDeviceSet(List<ResolvedCandidate> uniqueCandidates, int skippedCount) {
            this.uniqueCandidates = uniqueCandidates;
            this.skippedCount = skippedCount;
        }
    }

    private static final class ResolvedCandidate {
        String lookupKey;
        String displayName;
        final String normalizedPhone;
        String rawBirthday;

        ResolvedCandidate(
                String lookupKey,
                String displayName,
                String normalizedPhone,
                String rawBirthday
        ) {
            this.lookupKey = lookupKey;
            this.displayName = displayName;
            this.normalizedPhone = normalizedPhone;
            this.rawBirthday = rawBirthday;
        }

        void mergeFrom(ResolvedCandidate other) {
            if (this.lookupKey == null && other.lookupKey != null) {
                this.lookupKey = other.lookupKey;
            }
            if (!ContactClientNameResolver.hasRealDisplayName(this.displayName)
                    && ContactClientNameResolver.hasRealDisplayName(other.displayName)) {
                this.displayName = other.displayName;
            }
            if ((this.rawBirthday == null || this.rawBirthday.isEmpty())
                    && other.rawBirthday != null && !other.rawBirthday.isEmpty()) {
                this.rawBirthday = other.rawBirthday;
            }
        }
    }
}
