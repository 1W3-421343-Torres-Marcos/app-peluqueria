package com.metanoia.app.data.db.projection;

import androidx.room.ColumnInfo;

/**
 * Proyección SQL agregada de ingresos y egresos de caja agrupados por día.
 */
public final class IncomeByDayProjection {

    public final long day;

    @ColumnInfo(name = "income_cents")
    public final long incomeCents;

    @ColumnInfo(name = "expense_cents")
    public final long expenseCents;

    @ColumnInfo(name = "movement_count")
    public final int movementCount;

    public IncomeByDayProjection(
            long day,
            long incomeCents,
            long expenseCents,
            int movementCount
    ) {
        this.day = day;
        this.incomeCents = incomeCents;
        this.expenseCents = expenseCents;
        this.movementCount = movementCount;
    }
}
