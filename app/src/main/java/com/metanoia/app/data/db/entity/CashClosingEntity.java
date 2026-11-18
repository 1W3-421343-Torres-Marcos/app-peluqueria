package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para cierres diarios de caja con índice único por fecha.
 */
@Entity(
        tableName = "cash_closings",
        indices = {
                @Index(value = {"day"}, unique = true)
        }
)
public final class CashClosingEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    public final long day;

    @ColumnInfo(name = "expected_cash_cents")
    public final long expectedCashCents;

    @ColumnInfo(name = "expected_transfer_cents")
    public final long expectedTransferCents;

    @ColumnInfo(name = "expected_card_cents")
    public final long expectedCardCents;

    @ColumnInfo(name = "expected_mp_cents")
    public final long expectedMpCents;

    @ColumnInfo(name = "counted_cash_cents")
    public final long countedCashCents;

    @ColumnInfo(name = "counted_transfer_cents")
    public final long countedTransferCents;

    @ColumnInfo(name = "counted_card_cents")
    public final long countedCardCents;

    @ColumnInfo(name = "counted_mp_cents")
    public final long countedMpCents;

    @ColumnInfo(name = "difference_cents")
    public final long differenceCents;

    @NonNull
    public final String notes;

    @ColumnInfo(name = "closed_at")
    public final long closedAt;

    @NonNull
    public final String operator;

    public CashClosingEntity(
            @NonNull String id,
            long day,
            long expectedCashCents,
            long expectedTransferCents,
            long expectedCardCents,
            long expectedMpCents,
            long countedCashCents,
            long countedTransferCents,
            long countedCardCents,
            long countedMpCents,
            long differenceCents,
            @NonNull String notes,
            long closedAt,
            @NonNull String operator
    ) {
        this.id = id;
        this.day = day;
        this.expectedCashCents = expectedCashCents;
        this.expectedTransferCents = expectedTransferCents;
        this.expectedCardCents = expectedCardCents;
        this.expectedMpCents = expectedMpCents;
        this.countedCashCents = countedCashCents;
        this.countedTransferCents = countedTransferCents;
        this.countedCardCents = countedCardCents;
        this.countedMpCents = countedMpCents;
        this.differenceCents = differenceCents;
        this.notes = notes;
        this.closedAt = closedAt;
        this.operator = operator;
    }
}
