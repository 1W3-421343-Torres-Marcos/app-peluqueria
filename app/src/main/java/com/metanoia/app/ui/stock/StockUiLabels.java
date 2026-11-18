package com.metanoia.app.ui.stock;

import android.content.Context;

import androidx.annotation.NonNull;

import com.metanoia.app.R;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;

/**
 * Etiquetas y formateo de tipos, unidades y cantidades de stock para la interfaz.
 */
public final class StockUiLabels {

    private StockUiLabels() {
    }

    @NonNull
    public static String typeLabel(@NonNull Context context, @NonNull ProductType type) {
        switch (type) {
            case SALE:
                return context.getString(R.string.stock_type_sale);
            case BOTH:
                return context.getString(R.string.stock_type_both);
            case USE:
            default:
                return context.getString(R.string.stock_type_use);
        }
    }

    @NonNull
    public static String unitShortLabel(@NonNull Context context, @NonNull ProductUnit unit) {
        switch (unit) {
            case GRAM:
                return context.getString(R.string.stock_unit_short_gram);
            case MILLILITER:
                return context.getString(R.string.stock_unit_short_milliliter);
            case UNIT:
            default:
                return context.getString(R.string.stock_unit_short_unit);
        }
    }

    @NonNull
    public static String formatQuantityWithUnit(
            @NonNull Context context,
            int quantity,
            @NonNull ProductUnit unit
    ) {
        return context.getString(
                R.string.stock_quantity_unit_format,
                quantity,
                unitShortLabel(context, unit)
        );
    }

    @NonNull
    public static String formatSignedQuantityWithUnit(
            @NonNull Context context,
            int quantity,
            @NonNull ProductUnit unit
    ) {
        String shortUnit = unitShortLabel(context, unit);
        if (quantity > 0) {
            return context.getString(R.string.stock_signed_positive_format, quantity, shortUnit);
        }
        return context.getString(R.string.stock_signed_negative_format, quantity, shortUnit);
    }

    @NonNull
    public static ProductType parseType(@NonNull String raw) {
        try {
            return ProductType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return ProductType.USE;
        }
    }

    @NonNull
    public static ProductUnit parseUnit(@NonNull String raw) {
        try {
            return ProductUnit.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return ProductUnit.UNIT;
        }
    }
}
