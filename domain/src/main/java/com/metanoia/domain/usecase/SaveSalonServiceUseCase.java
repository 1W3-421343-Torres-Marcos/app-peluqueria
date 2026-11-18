package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.SalonServiceValidator;
import com.metanoia.domain.rules.TextNormalizer;

import java.util.Objects;
import java.util.Optional;

/**
 * Valida y guarda (alta o edición) un servicio del catálogo respetando bloque mínimo y unicidad.
 */
public final class SaveSalonServiceUseCase {

    private final SalonServiceRepository serviceRepository;
    private final SettingsRepository settingsRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SaveSalonServiceUseCase(
            SalonServiceRepository serviceRepository,
            SettingsRepository settingsRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<SalonService> execute(Request request) {
        if (request == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud nula");
        }
        if (!SalonServiceValidator.isValidName(request.name)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El nombre debe tener entre 2 y 80 caracteres");
        }
        if (!SalonServiceValidator.isValidCategory(request.category)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La categoría debe tener entre 2 y 40 caracteres");
        }
        if (!SalonServiceValidator.isValidPrice(request.price)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El precio no puede ser negativo");
        }
        int slotMinutes = settingsRepository.getSettings().slotMinutes();
        if (!SalonServiceValidator.isValidDuration(request.durationMin, slotMinutes)) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La duración debe ser positiva y múltiplo de " + slotMinutes + " min");
        }

        String cleanName = request.name.trim();
        String cleanCategory = request.category.trim();
        long now = clock.nowMillis();
        boolean isEdit = request.id != null && !request.id.trim().isEmpty();

        SalonService existing = null;
        if (isEdit) {
            Optional<SalonService> found = serviceRepository.findById(request.id.trim());
            if (!found.isPresent()) {
                return Result.failure(DomainError.NOT_FOUND, "Servicio no encontrado");
            }
            existing = found.get();
        }

        if (request.active) {
            Optional<SalonService> duplicate = serviceRepository
                    .findActiveByCategoryAndNormalizedName(
                            TextNormalizer.normalize(cleanCategory),
                            TextNormalizer.normalize(cleanName));
            if (duplicate.isPresent()
                    && (!isEdit || !duplicate.get().id().equals(existing.id()))) {
                return Result.failure(DomainError.CONFLICT,
                        "Ya existe un servicio activo con ese nombre en la categoría");
            }
        }

        SalonService service = new SalonService.Builder()
                .id(isEdit ? existing.id() : idGenerator.nextId())
                .name(cleanName)
                .category(cleanCategory)
                .price(request.price)
                .durationMin(request.durationMin)
                .colorService(request.colorService)
                .active(request.active)
                .sortOrder(request.sortOrder)
                .createdAt(isEdit ? existing.createdAt() : now)
                .updatedAt(now)
                .build();

        serviceRepository.save(service);
        return Result.success(service);
    }

    public static final class Request {
        private final String id;
        private final String name;
        private final String category;
        private final Money price;
        private final int durationMin;
        private final boolean colorService;
        private final boolean active;
        private final int sortOrder;

        public Request(
                String id,
                String name,
                String category,
                Money price,
                int durationMin,
                boolean colorService,
                boolean active,
                int sortOrder
        ) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.durationMin = durationMin;
            this.colorService = colorService;
            this.active = active;
            this.sortOrder = sortOrder;
        }
    }
}
