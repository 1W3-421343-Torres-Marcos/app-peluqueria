package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para los clientes del salón con índices sobre nombre normalizado y teléfono.
 */
@Entity(
        tableName = "clients",
        indices = {
                @Index(value = {"name_normalized"}),
                @Index(value = {"phone"})
        }
)
public final class ClientEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @NonNull
    public final String name;

    @ColumnInfo(name = "name_normalized")
    @NonNull
    public final String nameNormalized;

    @NonNull
    public final String phone;

    @ColumnInfo(name = "birth_day")
    @Nullable
    public final Integer birthDay;

    @ColumnInfo(name = "birth_month")
    @Nullable
    public final Integer birthMonth;

    @ColumnInfo(name = "birth_year")
    @Nullable
    public final Integer birthYear;

    @NonNull
    public final String notes;

    @ColumnInfo(name = "no_show_count")
    public final int noShowCount;

    public final boolean active;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public ClientEntity(
            @NonNull String id,
            @NonNull String name,
            @NonNull String nameNormalized,
            @NonNull String phone,
            @Nullable Integer birthDay,
            @Nullable Integer birthMonth,
            @Nullable Integer birthYear,
            @NonNull String notes,
            int noShowCount,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.phone = phone;
        this.birthDay = birthDay;
        this.birthMonth = birthMonth;
        this.birthYear = birthYear;
        this.notes = notes;
        this.noShowCount = noShowCount;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
