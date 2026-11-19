package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reporte inmutable de servicios y categorías más solicitados sobre turnos realizados.
 */
public final class TopServicesReport {

    public static final class ServiceRow {
        private final String serviceId;
        private final String serviceName;
        private final String category;
        private final int count;
        private final long revenueCents;

        public ServiceRow(
                String serviceId,
                String serviceName,
                String category,
                int count,
                long revenueCents
        ) {
            this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
            this.serviceName = Objects.requireNonNull(serviceName, "serviceName");
            this.category = category == null ? "" : category;
            this.count = count;
            this.revenueCents = revenueCents;
        }

        public String serviceId() {
            return serviceId;
        }

        public String serviceName() {
            return serviceName;
        }

        public String category() {
            return category;
        }

        public int count() {
            return count;
        }

        public long revenueCents() {
            return revenueCents;
        }
    }

    public static final class CategoryRow {
        private final String category;
        private final int count;
        private final long revenueCents;

        public CategoryRow(String category, int count, long revenueCents) {
            this.category = category == null ? "" : category;
            this.count = count;
            this.revenueCents = revenueCents;
        }

        public String category() {
            return category;
        }

        public int count() {
            return count;
        }

        public long revenueCents() {
            return revenueCents;
        }
    }

    private final int totalServiceCount;
    private final long totalRevenueCents;
    private final List<ServiceRow> byService;
    private final List<CategoryRow> byCategory;

    public TopServicesReport(List<ServiceRow> byService, List<CategoryRow> byCategory) {
        this.byService = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byService, "byService")));
        this.byCategory = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byCategory, "byCategory")));
        int totalCount = 0;
        long totalRevenue = 0L;
        for (ServiceRow row : this.byService) {
            totalCount += row.count();
            totalRevenue = Math.addExact(totalRevenue, row.revenueCents());
        }
        this.totalServiceCount = totalCount;
        this.totalRevenueCents = totalRevenue;
    }

    public int totalServiceCount() {
        return totalServiceCount;
    }

    public long totalRevenueCents() {
        return totalRevenueCents;
    }

    public List<ServiceRow> byService() {
        return byService;
    }

    public List<CategoryRow> byCategory() {
        return byCategory;
    }
}
