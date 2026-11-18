package com.metanoia.app.data.mapper;

import com.metanoia.app.data.db.entity.ColorFormulaComponentEntity;
import com.metanoia.app.data.db.entity.ColorFormulaEntity;
import com.metanoia.app.data.db.pojo.ColorFormulaWithComponents;
import com.metanoia.domain.model.ColorComponentUnit;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Convierte entre entidades Room de fichas de color y modelos de dominio.
 */
public final class ColorFormulaMapper {

    private ColorFormulaMapper() {
    }

    public static ColorFormula toDomain(ColorFormulaWithComponents row) {
        ColorFormulaEntity entity = row.formula;
        List<ColorFormulaComponentEntity> rawComponents = row.components == null
                ? Collections.emptyList()
                : new ArrayList<>(row.components);
        rawComponents.sort(Comparator.comparingInt(c -> c.position));

        List<ColorFormulaComponent> components = new ArrayList<>(rawComponents.size());
        for (ColorFormulaComponentEntity comp : rawComponents) {
            components.add(toDomainComponent(comp));
        }

        return new ColorFormula.Builder()
                .id(entity.id)
                .clientId(entity.clientId)
                .appointmentId(entity.appointmentId)
                .professionalId(entity.professionalId)
                .day(entity.day)
                .exposureMin(entity.exposureMin)
                .developerVolume(entity.developerVolume)
                .notes(entity.notes)
                .result(entity.result)
                .deletedAt(entity.deletedAt)
                .createdAt(entity.createdAt)
                .components(components)
                .build();
    }

    public static List<ColorFormula> toDomainList(List<ColorFormulaWithComponents> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ColorFormula> result = new ArrayList<>(rows.size());
        for (ColorFormulaWithComponents row : rows) {
            if (row != null && row.components != null && !row.components.isEmpty()) {
                result.add(toDomain(row));
            }
        }
        return result;
    }

    public static ColorFormulaComponent toDomainComponent(ColorFormulaComponentEntity entity) {
        return new ColorFormulaComponent.Builder()
                .id(entity.id)
                .formulaId(entity.formulaId)
                .brand(entity.brand)
                .productLine(entity.productLine)
                .shade(entity.shade)
                .amountTenths(entity.amountTenths)
                .unit(ColorComponentUnit.valueOf(entity.unit))
                .position(entity.position)
                .build();
    }

    public static ColorFormulaEntity toEntity(ColorFormula formula) {
        return new ColorFormulaEntity(
                formula.id(),
                formula.clientId(),
                formula.appointmentId().orElse(null),
                formula.professionalId().orElse(null),
                formula.day(),
                formula.exposureMin(),
                formula.developerVolume().orElse(null),
                formula.notes(),
                formula.result().orElse(null),
                formula.deletedAt().orElse(null),
                formula.createdAt()
        );
    }

    public static List<ColorFormulaComponentEntity> toComponentEntities(ColorFormula formula) {
        List<ColorFormulaComponent> components = formula.components();
        List<ColorFormulaComponentEntity> entities = new ArrayList<>(components.size());
        for (ColorFormulaComponent comp : components) {
            entities.add(new ColorFormulaComponentEntity(
                    comp.id(),
                    formula.id(),
                    comp.brand(),
                    comp.productLine().orElse(null),
                    comp.shade(),
                    comp.amountTenths(),
                    comp.unit().name(),
                    comp.position()
            ));
        }
        return entities;
    }
}
