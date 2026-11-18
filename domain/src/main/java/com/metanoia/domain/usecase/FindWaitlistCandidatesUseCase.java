package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;
import com.metanoia.domain.rules.WaitlistMatcher;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;
import com.metanoia.domain.rules.WaitlistMatcher.MatchedCandidate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Vence entradas expiradas y obtiene candidatos compatibles ordenados para un hueco liberado.
 */
public final class FindWaitlistCandidatesUseCase {

    private final WaitlistRepository waitlistRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClientRepository clientRepository;
    private final SalonServiceRepository serviceRepository;
    private final SettingsRepository settingsRepository;
    private final Clock clock;

    public FindWaitlistCandidatesUseCase(
            WaitlistRepository waitlistRepository,
            ProfessionalRepository professionalRepository,
            ClientRepository clientRepository,
            SalonServiceRepository serviceRepository,
            SettingsRepository settingsRepository,
            Clock clock
    ) {
        this.waitlistRepository = Objects.requireNonNull(
                waitlistRepository, "waitlistRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Result<List<MatchedCandidate>> execute(FreedSlot slot) {
        long today = clock.todayEpochDay();
        waitlistRepository.expireBefore(today);

        if (slot == null || slot.dayEpoch() < today) {
            return Result.success(Collections.emptyList());
        }
        Optional<Professional> proOpt = professionalRepository.findById(slot.professionalId());
        if (!proOpt.isPresent() || !proOpt.get().isActive()) {
            return Result.success(Collections.emptyList());
        }

        List<WaitlistEntry> activeEntries = waitlistRepository.findByStatuses(
                Collections.singleton(WaitlistStatus.ACTIVE));
        if (activeEntries.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, Client> clientsById = new LinkedHashMap<>();
        for (WaitlistEntry entry : activeEntries) {
            if (!clientsById.containsKey(entry.clientId())) {
                clientRepository.findById(entry.clientId())
                        .ifPresent(client -> clientsById.put(client.id(), client));
            }
        }

        Map<String, SalonService> servicesById = new LinkedHashMap<>();
        for (SalonService service : serviceRepository.findAll(false)) {
            servicesById.put(service.id(), service);
        }

        Settings settings = settingsRepository.getSettings();
        List<MatchedCandidate> candidates = WaitlistMatcher.match(
                slot,
                proOpt.get(),
                today,
                settings.slotMinutes(),
                activeEntries,
                clientsById,
                servicesById
        );
        return Result.success(candidates);
    }
}
