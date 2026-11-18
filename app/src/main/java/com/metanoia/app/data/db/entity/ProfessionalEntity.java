package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para los profesionales del salón.
 */
@Entity(
        tableName = "professionals",
        indices = {@Index(value = {"active", "name"})}
)
public final class ProfessionalEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @NonNull
    public final String name;

    @NonNull
    public final String phone;

    @ColumnInfo(name = "color_tag")
    @NonNull
    public final String colorTag;

    @ColumnInfo(name = "commission_basis_points")
    public final int commissionBasisPoints;

    public final boolean active;

    @ColumnInfo(name = "created_at")
    public final long createdAt;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public ProfessionalEntity(
            @NonNull String id,
            @NonNull String name,
            @NonNull String phone,
            @NonNull String colorTag,
            int commissionBasisPoints,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.colorTag = colorTag;
        this.commissionBasisPoints = commissionBasisPoints;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
