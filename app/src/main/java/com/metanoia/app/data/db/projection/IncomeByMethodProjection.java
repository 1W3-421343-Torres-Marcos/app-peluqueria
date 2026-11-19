package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de ingresos y egresos de caja agrupados por método de pago.
 */
public final class IncomeByMethodProjection {

    @NonNull
    public final String method;

    @ColumnInfo(name = "income_cents")
    public final long incomeCents;

    @ColumnInfo(name = "expense_cents")
    public final long expenseCents;

    @ColumnInfo(name = "movement_count")
    public final int movementCount;

    public IncomeByMethodProjection(
            @NonNull String method,
            long incomeCents,
            long expenseCents,
            int movementCount
    ) {
        this.method = method;
        this.incomeCents = incomeCents;
        this.expenseCents = expenseCents;
        this.movementCount = movementCount;
    }
}
