package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para reglas de recurrencia de turnos.
 */
@Entity(tableName = "recurrence_rules")
public final class RecurrenceRuleEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "interval_weeks")
    public final int intervalWeeks;

    @ColumnInfo(name = "until_day")
    @Nullable
    public final Long untilDay;

    @ColumnInfo(name = "max_occurrences")
    @Nullable
    public final Integer maxOccurrences;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    public RecurrenceRuleEntity(
            @NonNull String id,
            int intervalWeeks,
            @Nullable Long untilDay,
            @Nullable Integer maxOccurrences,
            long createdAt
    ) {
        this.id = id;
        this.intervalWeeks = intervalWeeks;
        this.untilDay = untilDay;
        this.maxOccurrences = maxOccurrences;
        this.createdAt = createdAt;
    }
}
