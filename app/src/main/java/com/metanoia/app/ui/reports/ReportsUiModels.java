package com.metanoia.app.ui.reports;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.metanoia.app.ui.reports.ReportRowsAdapter.ReportRowItem;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.ReportFilter;
import com.metanoia.domain.model.ReportType;
import com.metanoia.domain.model.TopServicesReport;

import java.util.List;
import java.util.Objects;

/**
 * Modelos inmutables de filtro, payload cargado y estado de UI para la pantalla de Reportes.
 */
public final class ReportsUiModels {

    private ReportsUiModels() {
    }

    public enum PeriodMode {
        TODAY,
        WEEK,
        MONTH,
        RANGE
    }

    public enum IncomeGranularity {
        DAY,
        MONTH
    }

    public enum ServiceGrouping {
        SERVICE,
        CATEGORY
    }

    public enum ClientRankingMode {
        VISITS,
        SPEND
    }

    public enum OccupancyGrouping {
        HOUR,
        DAY
    }

    public static final class ReportsFilterState {
        @NonNull
        public final ReportType reportType;
        @NonNull
        public final PeriodMode periodMode;
        public final long fromDay;
        public final long toDay;
        @Nullable
        public final String professionalId;
        @NonNull
        public final IncomeGranularity incomeGranularity;
        @NonNull
        public final ServiceGrouping serviceGrouping;
        @NonNull
        public final ClientRankingMode clientRankingMode;
        @NonNull
        public final OccupancyGrouping occupancyGrouping;
        public final boolean includeClientPhone;

        public ReportsFilterState(
                @NonNull ReportType reportType,
                @NonNull PeriodMode periodMode,
                long fromDay,
                long toDay,
                @Nullable String professionalId,
                @NonNull IncomeGranularity incomeGranularity,
                @NonNull ServiceGrouping serviceGrouping,
                @NonNull ClientRankingMode clientRankingMode,
                @NonNull OccupancyGrouping occupancyGrouping,
                boolean includeClientPhone
        ) {
            this.reportType = reportType;
            this.periodMode = periodMode;
            this.fromDay = fromDay;
            this.toDay = toDay;
            this.professionalId = professionalId;
            this.incomeGranularity = incomeGranularity;
            this.serviceGrouping = serviceGrouping;
            this.clientRankingMode = clientRankingMode;
            this.occupancyGrouping = occupancyGrouping;
            this.includeClientPhone = includeClientPhone;
        }

        public ReportFilter toDomainFilter() {
            return new ReportFilter(fromDay, toDay, professionalId);
        }

        boolean sameDataQuery(@NonNull ReportsFilterState other) {
            return reportType == other.reportType
                    && fromDay == other.fromDay
                    && toDay == other.toDay
                    && Objects.equals(professionalId, other.professionalId);
        }

        ReportsFilterState withReportType(@NonNull ReportType next) {
            return new ReportsFilterState(next, periodMode, fromDay, toDay, professionalId,
                    incomeGranularity, serviceGrouping, clientRankingMode,
                    occupancyGrouping, includeClientPhone);
        }

        ReportsFilterState withPeriod(
                @NonNull PeriodMode nextMode,
                long nextFrom,
                long nextTo
        ) {
            return new ReportsFilterState(reportType, nextMode, nextFrom, nextTo, professionalId,
                    incomeGranularity, serviceGrouping, clientRankingMode,
                    occupancyGrouping, includeClientPhone);
        }

        ReportsFilterState withProfessionalId(@Nullable String nextProId) {
            return new ReportsFilterState(reportType, periodMode, fromDay, toDay, nextProId,
                    incomeGranularity, serviceGrouping, clientRankingMode,
                    occupancyGrouping, includeClientPhone);
        }

        ReportsFilterState withIncludeClientPhone(boolean nextInclude) {
            return new ReportsFilterState(reportType, periodMode, fromDay, toDay, professionalId,
                    incomeGranularity, serviceGrouping, clientRankingMode,
                    occupancyGrouping, nextInclude);
        }
    }

    static final class LoadedPayload {
        @Nullable
        final IncomeReport income;
        @Nullable
        final TopServicesReport topServices;
        @Nullable
        final FrequentClientsReport frequentClients;
        @Nullable
        final OccupancyReport occupancy;
        @Nullable
        final ProfessionalPerformanceReport performance;
        @Nullable
        final ReceivablesReport receivables;

        LoadedPayload(
                @Nullable IncomeReport income,
                @Nullable TopServicesReport topServices,
                @Nullable FrequentClientsReport frequentClients,
                @Nullable OccupancyReport occupancy,
                @Nullable ProfessionalPerformanceReport performance,
                @Nullable ReceivablesReport receivables
        ) {
            this.income = income;
            this.topServices = topServices;
            this.frequentClients = frequentClients;
            this.occupancy = occupancy;
            this.performance = performance;
            this.receivables = receivables;
        }
    }

    public static final class ReportsUiState {
        @NonNull
        public final ReportsFilterState filter;
        public final boolean loading;
        @NonNull
        public final String kpiPrimaryLabel;
        @NonNull
        public final String kpiPrimaryValue;
        @NonNull
        public final String kpiSecondaryLabel;
        @NonNull
        public final String kpiSecondaryValue;
        @Nullable
        public final String secondarySectionTitle;
        @NonNull
        public final List<String> secondaryLines;
        @NonNull
        public final List<ReportRowItem> rows;
        @StringRes
        public final int emptyMessageRes;
        @NonNull
        final LoadedPayload payload;

        ReportsUiState(
                @NonNull ReportsFilterState filter,
                boolean loading,
                @NonNull String kpiPrimaryLabel,
                @NonNull String kpiPrimaryValue,
                @NonNull String kpiSecondaryLabel,
                @NonNull String kpiSecondaryValue,
                @Nullable String secondarySectionTitle,
                @NonNull List<String> secondaryLines,
                @NonNull List<ReportRowItem> rows,
                @StringRes int emptyMessageRes,
                @NonNull LoadedPayload payload
        ) {
            this.filter = filter;
            this.loading = loading;
            this.kpiPrimaryLabel = kpiPrimaryLabel;
            this.kpiPrimaryValue = kpiPrimaryValue;
            this.kpiSecondaryLabel = kpiSecondaryLabel;
            this.kpiSecondaryValue = kpiSecondaryValue;
            this.secondarySectionTitle = secondarySectionTitle;
            this.secondaryLines = secondaryLines;
            this.rows = rows;
            this.emptyMessageRes = emptyMessageRes;
            this.payload = payload;
        }
    }
}
