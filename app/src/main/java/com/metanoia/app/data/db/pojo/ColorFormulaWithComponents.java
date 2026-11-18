package com.metanoia.app.data.db.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.metanoia.app.data.db.entity.ColorFormulaComponentEntity;
import com.metanoia.app.data.db.entity.ColorFormulaEntity;

import java.util.List;

/**
 * Proyección Room de una fórmula de color junto con sus componentes ordenados.
 */
public final class ColorFormulaWithComponents {

    @Embedded
    public ColorFormulaEntity formula;

    @Relation(parentColumn = "id", entityColumn = "formula_id")
    public List<ColorFormulaComponentEntity> components;
}
