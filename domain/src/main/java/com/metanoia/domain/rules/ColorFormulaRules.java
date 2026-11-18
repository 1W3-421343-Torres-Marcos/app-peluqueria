package com.metanoia.domain.rules;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.ColorComponentUnit;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reglas puras de validación, ordenamiento y versionado de fórmulas de coloración.
 */
public final class ColorFormulaRules {

    private ColorFormulaRules() {
    }

    public static Result<List<ColorFormulaComponent>> validateAndNormalizeComponents(
            String formulaId,
            List<ComponentDraft> drafts,
            IdGenerator idGenerator
    ) {
        if (drafts == null || drafts.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Agregá al menos un componente con marca y tono");
        }
        String safeFormulaId = formulaId == null ? "" : formulaId.trim();
        List<ColorFormulaComponent> components = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            ComponentDraft draft = drafts.get(index);
            if (draft == null) {
                return Result.failure(DomainError.VALIDATION_ERROR, "Componente inválido");
            }
            String brand = trimToEmpty(draft.brand);
            String shade = trimToEmpty(draft.shade);
            if (brand.isEmpty() || shade.isEmpty()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Cada componente requiere marca y tono");
            }
            if (brand.length() > ColorFormulaComponent.MAX_BRAND_LENGTH) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "La marca excede los 80 caracteres");
            }
            String productLine = trimToNull(draft.productLine);
            if (productLine != null
                    && productLine.length() > ColorFormulaComponent.MAX_PRODUCT_LINE_LENGTH) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "La línea excede los 80 caracteres");
            }
            if (shade.length() > ColorFormulaComponent.MAX_SHADE_LENGTH) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El tono excede los 40 caracteres");
            }
            if (draft.amountTenths <= 0
                    || draft.amountTenths > ColorFormulaComponent.MAX_AMOUNT_TENTHS) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "La cantidad de cada componente debe ser mayor a cero");
            }
            if (draft.unit == null) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Seleccioná la unidad del componente");
            }
            components.add(new ColorFormulaComponent.Builder()
                    .id(idGenerator.nextId())
                    .formulaId(safeFormulaId)
                    .brand(brand)
                    .productLine(productLine)
                    .shade(shade)
                    .amountTenths(draft.amountTenths)
                    .unit(draft.unit)
                    .position(index)
                    .build());
        }
        return Result.success(components);
    }

    public static boolean isLockedByAppointmentStatus(Optional<AppointmentStatus> status) {
        return status.isPresent()
                && (status.get() == AppointmentStatus.COMPLETED
                || status.get() == AppointmentStatus.PAID);
    }

    public static Optional<ColorFormula> selectLatestActiveFormula(List<ColorFormula> formulas) {
        if (formulas == null || formulas.isEmpty()) {
            return Optional.empty();
        }
        return formulas.stream()
                .filter(Objects::nonNull)
                .filter(f -> !f.isDeleted())
                .min(Comparator
                        .comparingLong(ColorFormula::day).reversed()
                        .thenComparing(Comparator.comparingLong(ColorFormula::createdAt).reversed())
                        .thenComparing(Comparator.comparing(ColorFormula::id).reversed()));
    }

    public static ColorFormula duplicateAsNewDraft(
            ColorFormula source,
            String newFormulaId,
            String targetAppointmentId,
            String targetProfessionalId,
            long targetDay,
            long nowMillis,
            IdGenerator idGenerator
    ) {
        List<ColorFormulaComponent> copiedComponents = new ArrayList<>(source.components().size());
        List<ColorFormulaComponent> sourceComponents = source.components();
        for (int index = 0; index < sourceComponents.size(); index++) {
            ColorFormulaComponent src = sourceComponents.get(index);
            copiedComponents.add(new ColorFormulaComponent.Builder()
                    .id(idGenerator.nextId())
                    .formulaId(newFormulaId)
                    .brand(src.brand())
                    .productLine(src.productLine().orElse(null))
                    .shade(src.shade())
                    .amountTenths(src.amountTenths())
                    .unit(src.unit())
                    .position(index)
                    .build());
        }
        String resolvedProId = trimToNull(targetProfessionalId);
        if (resolvedProId == null) {
            resolvedProId = source.professionalId().orElse(null);
        }
        return new ColorFormula.Builder()
                .id(newFormulaId)
                .clientId(source.clientId())
                .appointmentId(trimToNull(targetAppointmentId))
                .professionalId(resolvedProId)
                .day(targetDay)
                .exposureMin(source.exposureMin())
                .developerVolume(source.developerVolume().orElse(null))
                .notes(source.notes())
                .result(source.result().orElse(null))
                .deletedAt(null)
                .createdAt(nowMillis)
                .components(copiedComponents)
                .build();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Borrador de entrada para validar un componente antes de persistirlo.
     */
    public static final class ComponentDraft {
        public final String brand;
        public final String productLine;
        public final String shade;
        public final int amountTenths;
        public final ColorComponentUnit unit;

        public ComponentDraft(
                String brand,
                String productLine,
                String shade,
                int amountTenths,
                ColorComponentUnit unit
        ) {
            this.brand = brand;
            this.productLine = productLine;
            this.shade = shade;
            this.amountTenths = amountTenths;
            this.unit = unit;
        }
    }
}
