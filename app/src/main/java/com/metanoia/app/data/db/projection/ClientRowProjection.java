package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL liviana para la lista, filtros y búsqueda de clientes (evita cargar notas).
 */
public final class ClientRowProjection {

    @NonNull
    public final String id;

    @NonNull
    public final String name;

    @NonNull
    public final String phone;

    @ColumnInfo(name = "birth_day")
    @Nullable
    public final Integer birthDay;

    @ColumnInfo(name = "birth_month")
    @Nullable
    public final Integer birthMonth;

    @ColumnInfo(name = "no_show_count")
    public final int noShowCount;

    @NonNull
    public final String origin;

    @ColumnInfo(name = "created_from_contact_at")
    @Nullable
    public final Long createdFromContactAt;

    public final boolean active;

    public ClientRowProjection(
            @NonNull String id,
            @NonNull String name,
            @NonNull String phone,
            @Nullable Integer birthDay,
            @Nullable Integer birthMonth,
            int noShowCount,
            @NonNull String origin,
            @Nullable Long createdFromContactAt,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.birthDay = birthDay;
        this.birthMonth = birthMonth;
        this.noShowCount = noShowCount;
        this.origin = origin;
        this.createdFromContactAt = createdFromContactAt;
        this.active = active;
    }

    public boolean hasBirthday() {
        return birthDay != null && birthMonth != null;
    }
}
