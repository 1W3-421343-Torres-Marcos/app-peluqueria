package com.metanoia.app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Ítem de turno con instantáneas congeladas de servicio, precio, duración y comisión.
 */
@Entity(
        tableName = "appointment_items",
        foreignKeys = {
                @ForeignKey(
                        entity = AppointmentEntity.class,
                        parentColumns = "id",
                        childColumns = "appointment_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = SalonServiceEntity.class,
                        parentColumns = "id",
                        childColumns = "service_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(value = {"appointment_id"}),
                @Index(value = {"service_id"})
        }
)
public final class AppointmentItemEntity {

    @PrimaryKey
    @NonNull
    public final String id;

    @ColumnInfo(name = "appointment_id")
    @NonNull
    public final String appointmentId;

    @ColumnInfo(name = "service_id")
    @NonNull
    public final String serviceId;

    @ColumnInfo(name = "service_name_snapshot")
    @NonNull
    public final String serviceNameSnapshot;

    @ColumnInfo(name = "price_cents_snapshot")
    public final long priceCentsSnapshot;

    @ColumnInfo(name = "duration_min_snapshot")
    public final int durationMinSnapshot;

    @ColumnInfo(name = "commission_bp_snapshot")
    @Nullable
    public final Integer commissionBasisPointsSnapshot;

    @ColumnInfo(name = "color_formula_id")
    @Nullable
    public final String colorFormulaId;

    public final int position;

    public AppointmentItemEntity(
            @NonNull String id,
            @NonNull String appointmentId,
            @NonNull String serviceId,
            @NonNull String serviceNameSnapshot,
            long priceCentsSnapshot,
            int durationMinSnapshot,
            @Nullable Integer commissionBasisPointsSnapshot,
            @Nullable String colorFormulaId,
            int position
    ) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.serviceId = serviceId;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.priceCentsSnapshot = priceCentsSnapshot;
        this.durationMinSnapshot = durationMinSnapshot;
        this.commissionBasisPointsSnapshot = commissionBasisPointsSnapshot;
        this.colorFormulaId = colorFormulaId;
        this.position = position;
    }
}
