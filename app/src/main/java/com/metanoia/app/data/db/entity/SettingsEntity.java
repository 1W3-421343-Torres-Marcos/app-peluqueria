package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room con los datos generales del negocio y parámetros de operación.
 */
@Entity(tableName = "settings")
public final class SettingsEntity {

    public static final String DEFAULT_ID = "default";

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "business_name")
    @NonNull
    public final String businessName;

    @NonNull
    public final String subtitle;

    @NonNull
    public final String address;

    @NonNull
    public final String phone;

    @NonNull
    public final String cuit;

    @ColumnInfo(name = "slot_minutes")
    public final int slotMinutes;

    @ColumnInfo(name = "current_operator")
    @NonNull
    public final String currentOperator;

    @ColumnInfo(name = "operators_csv")
    @NonNull
    public final String operatorsCsv;

    @ColumnInfo(name = "waitlist_expiry_days")
    public final int waitlistExpiryDays;

    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public SettingsEntity(
            @NonNull String id,
            @NonNull String businessName,
            @NonNull String subtitle,
            @NonNull String address,
            @NonNull String phone,
            @NonNull String cuit,
            int slotMinutes,
            @NonNull String currentOperator,
            @NonNull String operatorsCsv,
            int waitlistExpiryDays,
            long updatedAt
    ) {
        this.id = id;
        this.businessName = businessName;
        this.subtitle = subtitle;
        this.address = address;
        this.phone = phone;
        this.cuit = cuit;
        this.slotMinutes = slotMinutes;
        this.currentOperator = currentOperator;
        this.operatorsCsv = operatorsCsv;
        this.waitlistExpiryDays = waitlistExpiryDays;
        this.updatedAt = updatedAt;
    }
}
