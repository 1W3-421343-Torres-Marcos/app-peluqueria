package com.metanoia.app.data.db.projection;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Proyección SQL liviana para la lista y búsqueda de clientes (evita cargar notas y metadatos).
 */
public final class ClientRowProjection {

    @NonNull
    public final String id;

    @NonNull
    public final String name;

    @NonNull
    public final String phone;

    @ColumnInfo(name = "no_show_count")
    public final int noShowCount;

    public final boolean active;

    public ClientRowProjection(
            @NonNull String id,
            @NonNull String name,
            @NonNull String phone,
            int noShowCount,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.noShowCount = noShowCount;
        this.active = active;
    }
}
