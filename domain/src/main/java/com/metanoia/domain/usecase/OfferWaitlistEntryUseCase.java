package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.WaitlistRepository;
import com.metanoia.domain.rules.TemplateRenderer;
import com.metanoia.domain.rules.TemplateRenderer.TemplateContext;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Prepara la oferta de un turno liberado con la plantilla de WhatsApp y marca la entrada {@code OFFERED}.
 */
public final class OfferWaitlistEntryUseCase {

    private final WaitlistRepository waitlistRepository;
    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final SalonServiceRepository serviceRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public OfferWaitlistEntryUseCase(
            WaitlistRepository waitlistRepository,
            ClientRepository clientRepository,
            ProfessionalRepository professionalRepository,
            SalonServiceRepository serviceRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.waitlistRepository = Objects.requireNonNull(
                waitlistRepository, "waitlistRepository");
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<OfferPreview> prepareOffer(String entryId, FreedSlot slot) {
        if (entryId == null || entryId.trim().isEmpty() || slot == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud inválida");
        }
        Optional<WaitlistEntry> found = waitlistRepository.findById(entryId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Entrada no encontrada");
        }
        WaitlistEntry entry = found.get();
        if (!entry.status().isOpen()) {
            return Result.failure(DomainError.CONFLICT,
                    "La entrada ya no está disponible para ofrecer");
        }
        Optional<Client> clientOpt = clientRepository.findById(entry.clientId());
        if (!clientOpt.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Cliente no encontrado");
        }
        Optional<Professional> proOpt = professionalRepository.findById(slot.professionalId());
        if (!proOpt.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado");
        }

        List<String> serviceNames = new ArrayList<>();
        for (String serviceId : entry.serviceIds()) {
            serviceRepository.findById(serviceId)
                    .map(SalonService::name)
                    .ifPresent(serviceNames::add);
        }
        Settings settings = settingsRepository.getSettings();
        TemplateContext context = new TemplateContext(
                clientOpt.get().name(),
                slot.dayEpoch(),
                slot.startMin(),
                String.join(", ", serviceNames),
                proOpt.get().name(),
                settings.businessDisplayName()
        );
        String message = TemplateRenderer.render(settings.waitlistOfferTemplate(), context)
                .renderedText();
        return Result.success(new OfferPreview(entry, message, clientOpt.get().phone()));
    }

    public Result<OfferResult> execute(String entryId, FreedSlot slot) {
        Result<OfferPreview> previewRes = prepareOffer(entryId, slot);
        if (previewRes.isFailure()) {
            return Result.failure(previewRes.getError(), previewRes.getErrorMessage());
        }
        OfferPreview preview = previewRes.getValue();
        long now = clock.nowMillis();
        WaitlistEntry offered = preview.entry().toBuilder()
                .status(WaitlistStatus.OFFERED)
                .offeredAt(now)
                .build();
        waitlistRepository.save(offered);

        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(now)
                .operator(settings.currentOperator())
                .action("WAITLIST_OFFER")
                .entityType("WAITLIST")
                .entityId(offered.id())
                .concept("Oferta de hueco por lista de espera")
                .build());
        return Result.success(new OfferResult(
                offered, preview.renderedMessage(), preview.clientPhone()));
    }

    public static final class OfferPreview {
        private final WaitlistEntry entry;
        private final String renderedMessage;
        private final String clientPhone;

        public OfferPreview(WaitlistEntry entry, String renderedMessage, String clientPhone) {
            this.entry = entry;
            this.renderedMessage = renderedMessage;
            this.clientPhone = clientPhone == null ? "" : clientPhone;
        }

        public WaitlistEntry entry() {
            return entry;
        }

        public String renderedMessage() {
            return renderedMessage;
        }

        public String clientPhone() {
            return clientPhone;
        }
    }

    public static final class OfferResult {
        private final WaitlistEntry entry;
        private final String renderedMessage;
        private final String clientPhone;

        public OfferResult(WaitlistEntry entry, String renderedMessage, String clientPhone) {
            this.entry = entry;
            this.renderedMessage = renderedMessage;
            this.clientPhone = clientPhone == null ? "" : clientPhone;
        }

        public WaitlistEntry entry() {
            return entry;
        }

        public String renderedMessage() {
            return renderedMessage;
        }

        public String clientPhone() {
            return clientPhone;
        }
    }
}
