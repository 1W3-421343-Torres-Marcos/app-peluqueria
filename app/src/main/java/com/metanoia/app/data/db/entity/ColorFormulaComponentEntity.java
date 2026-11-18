package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para cada componente de mezcla de una fórmula de color.
 */
@Entity(
        tableName = "color_formula_components",
        foreignKeys = {
                @ForeignKey(
                        entity = ColorFormulaEntity.class,
                        parentColumns = "id",
                        childColumns = "formula_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"formula_id", "position"})
        }
)
public final class ColorFormulaComponentEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "formula_id")
    @NonNull
    public final String formulaId;

    @NonNull
    public final String brand;

    @ColumnInfo(name = "product_line")
    @Nullable
    public final String productLine;

    @NonNull
    public final String shade;

    @ColumnInfo(name = "amount_tenths")
    public final int amountTenths;

    @NonNull
    public final String unit;

    public final int position;

    public ColorFormulaComponentEntity(
            @NonNull String id,
            @NonNull String formulaId,
            @NonNull String brand,
            @Nullable String productLine,
            @NonNull String shade,
            int amountTenths,
            @NonNull String unit,
            int position
    ) {
        this.id = id;
        this.formulaId = formulaId;
        this.brand = brand;
        this.productLine = productLine;
        this.shade = shade;
        this.amountTenths = amountTenths;
        this.unit = unit;
        this.position = position;
    }
}
