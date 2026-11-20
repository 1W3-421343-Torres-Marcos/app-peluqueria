package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ContactImportPreview;
import com.metanoia.domain.model.DeviceContactCandidate;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.rules.ClientContactSyncPlanner;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Calcula los contadores de vista previa (a importar, ya cargados, omitidos) sin escribir en base.
 */
public final class PreviewContactImportUseCase {

    private final ClientRepository clientRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public PreviewContactImportUseCase(
            ClientRepository clientRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<ContactImportPreview> execute(List<DeviceContactCandidate> candidates) {
        if (candidates == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Lista de contactos nula");
        }
        List<Client> existing = clientRepository.findAllForContactSync();
        int maxYear = LocalDate.ofEpochDay(clock.todayEpochDay()).getYear();
        ClientContactSyncPlanner.PlanResult plan = ClientContactSyncPlanner.plan(
                candidates, existing, maxYear, clock.nowMillis(), idGenerator);
        return Result.success(plan.preview());
    }
}
