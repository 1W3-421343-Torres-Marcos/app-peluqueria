package com.metanoia.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Ítem individual de un turno con instantánea inmutable de nombre, precio, duración y comisión.
 */
public final class AppointmentItem {

    private static final int MAX_COMMISSION_BASIS_POINTS = 10_000;

    private final String id;
    private final String appointmentId;
    private final String serviceId;
    private final String serviceNameSnapshot;
    private final Money priceSnapshot;
    private final int durationMinSnapshot;
    private final Integer commissionBasisPointsSnapshot;
    private final String colorFormulaId;
    private final int position;

    private AppointmentItem(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.appointmentId = Objects.requireNonNull(builder.appointmentId, "appointmentId");
        this.serviceId = Objects.requireNonNull(builder.serviceId, "serviceId");
        this.serviceNameSnapshot = Objects.requireNonNull(
                builder.serviceNameSnapshot, "serviceNameSnapshot");
        this.priceSnapshot = Objects.requireNonNull(builder.priceSnapshot, "priceSnapshot");
        if (this.priceSnapshot.isNegative()) {
            throw new IllegalArgumentException("El precio snapshot no puede ser negativo");
        }
        if (builder.durationMinSnapshot <= 0) {
            throw new IllegalArgumentException(
                    "La duración snapshot debe ser positiva: " + builder.durationMinSnapshot);
        }
        this.durationMinSnapshot = builder.durationMinSnapshot;
        if (builder.commissionBasisPointsSnapshot != null
                && (builder.commissionBasisPointsSnapshot < 0
                || builder.commissionBasisPointsSnapshot > MAX_COMMISSION_BASIS_POINTS)) {
            throw new IllegalArgumentException(
                    "Comisión snapshot fuera de rango [0, 10000]: "
                            + builder.commissionBasisPointsSnapshot);
        }
        this.commissionBasisPointsSnapshot = builder.commissionBasisPointsSnapshot;
        this.colorFormulaId = (builder.colorFormulaId == null
                || builder.colorFormulaId.trim().isEmpty())
                ? null
                : builder.colorFormulaId.trim();
        if (builder.position < 0) {
            throw new IllegalArgumentException("position negativa: " + builder.position);
        }
        this.position = builder.position;
    }

    public String id() {
        return id;
    }

    public String appointmentId() {
        return appointmentId;
    }

    public String serviceId() {
        return serviceId;
    }

    public String serviceNameSnapshot() {
        return serviceNameSnapshot;
    }

    public Money priceSnapshot() {
        return priceSnapshot;
    }

    public int durationMinSnapshot() {
        return durationMinSnapshot;
    }

    public Optional<Integer> commissionBasisPointsSnapshot() {
        return Optional.ofNullable(commissionBasisPointsSnapshot);
    }

    public Optional<String> colorFormulaId() {
        return Optional.ofNullable(colorFormulaId);
    }

    public int position() {
        return position;
    }

    public AppointmentItem withFrozenCommission(int commissionBasisPoints) {
        return toBuilder().commissionBasisPointsSnapshot(commissionBasisPoints).build();
    }

    public AppointmentItem withClearedCommission() {
        return toBuilder().commissionBasisPointsSnapshot(null).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .appointmentId(appointmentId)
                .serviceId(serviceId)
                .serviceNameSnapshot(serviceNameSnapshot)
                .priceSnapshot(priceSnapshot)
                .durationMinSnapshot(durationMinSnapshot)
                .commissionBasisPointsSnapshot(commissionBasisPointsSnapshot)
                .colorFormulaId(colorFormulaId)
                .position(position);
    }

    public static final class Builder {
        private String id;
        private String appointmentId;
        private String serviceId;
        private String serviceNameSnapshot;
        private Money priceSnapshot = Money.zero();
        private int durationMinSnapshot;
        private Integer commissionBasisPointsSnapshot;
        private String colorFormulaId;
        private int position;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder appointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
            return this;
        }

        public Builder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder serviceNameSnapshot(String serviceNameSnapshot) {
            this.serviceNameSnapshot = serviceNameSnapshot;
            return this;
        }

        public Builder priceSnapshot(Money priceSnapshot) {
            this.priceSnapshot = priceSnapshot;
            return this;
        }

        public Builder durationMinSnapshot(int durationMinSnapshot) {
            this.durationMinSnapshot = durationMinSnapshot;
            return this;
        }

        public Builder commissionBasisPointsSnapshot(Integer commissionBasisPointsSnapshot) {
            this.commissionBasisPointsSnapshot = commissionBasisPointsSnapshot;
            return this;
        }

        public Builder colorFormulaId(String colorFormulaId) {
            this.colorFormulaId = colorFormulaId;
            return this;
        }

        public Builder position(int position) {
            this.position = position;
            return this;
        }

        public AppointmentItem build() {
            return new AppointmentItem(this);
        }
    }
}
