package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.metanoia.domain.model.Settings;

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

    @ColumnInfo(name = "reminder_template", defaultValue = Settings.DEFAULT_REMINDER_TEMPLATE)
    @NonNull
    public final String reminderTemplate;

    @ColumnInfo(name = "birthday_template", defaultValue = Settings.DEFAULT_BIRTHDAY_TEMPLATE)
    @NonNull
    public final String birthdayTemplate;

    @ColumnInfo(name = "waitlist_offer_template", defaultValue = Settings.DEFAULT_WAITLIST_OFFER_TEMPLATE)
    @NonNull
    public final String waitlistOfferTemplate;

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
            @NonNull String reminderTemplate,
            @NonNull String birthdayTemplate,
            @NonNull String waitlistOfferTemplate,
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
        this.reminderTemplate = reminderTemplate;
        this.birthdayTemplate = birthdayTemplate;
        this.waitlistOfferTemplate = waitlistOfferTemplate;
        this.updatedAt = updatedAt;
    }
}
