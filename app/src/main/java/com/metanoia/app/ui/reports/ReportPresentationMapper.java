package com.metanoia.app.ui.reports;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.R;
import com.metanoia.app.ui.cash.CashUiLabels;
import com.metanoia.app.ui.reports.ReportRowsAdapter.ReportRowItem;
import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.LoadedPayload;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsUiState;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.TopServicesReport;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapea los reportes crudos del repositorio en KPIs, resúmenes secundarios y filas de tabla UI.
 */
final class ReportPresentationMapper {

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM uuuu", Formats.ES_AR);

    private ReportPresentationMapper() {
    }

    @NonNull
    static ReportsUiState mapToUiState(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        switch (filter.reportType) {
            case TOP_SERVICES:
                return mapTopServices(context, filter, payload);
            case FREQUENT_CLIENTS:
                return mapFrequentClients(context, filter, payload);
            case OCCUPANCY:
                return ReportOperationalMapper.mapOccupancy(context, filter, payload);
            case PROFESSIONAL_PERFORMANCE:
                return ReportOperationalMapper.mapPerformance(context, filter, payload);
            case RECEIVABLES:
                return ReportOperationalMapper.mapReceivables(context, filter, payload);
            case INCOME:
            default:
                return mapIncome(context, filter, payload);
        }
    }

    @NonNull
    private static ReportsUiState mapIncome(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        IncomeReport report = payload.income;
        long net = report == null ? 0L : report.netCents();
        long income = report == null ? 0L : report.totalIncomeCents();
        long expense = report == null ? 0L : report.totalExpenseCents();

        List<String> methodLines = buildIncomeMethodLines(context, report);
        List<ReportRowItem> rows = buildIncomeRows(context, filter, report);
        ReportChartMapper.ChartData chart = ReportChartMapper.buildChart(context, filter, payload);

        return new ReportsUiState(
                filter,
                false,
                context.getString(R.string.reports_kpi_income_net),
                Formats.formatMoney(net),
                context.getString(R.string.reports_kpi_income_inflow_outflow),
                context.getString(R.string.reports_value_pair_format,
                        Formats.formatMoney(income), Formats.formatMoney(expense)),
                methodLines.isEmpty() ? null : context.getString(R.string.reports_section_methods),
                methodLines,
                rows,
                chart.title,
                chart.entries,
                chart.maxValueOverride,
                R.string.reports_empty_income,
                payload
        );
    }

    @NonNull
    private static List<String> buildIncomeMethodLines(
            @NonNull Context context,
            @Nullable IncomeReport report
    ) {
        if (report == null || report.byMethod().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(report.byMethod().size());
        for (IncomeReport.MethodRow row : report.byMethod()) {
            lines.add(context.getString(
                    R.string.reports_method_row_format,
                    CashUiLabels.methodLabel(context, row.method()),
                    Formats.formatMoney(row.netCents()),
                    Formats.formatMoney(row.incomeCents()),
                    Formats.formatMoney(row.expenseCents())));
        }
        return lines;
    }

    @NonNull
    private static List<ReportRowItem> buildIncomeRows(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @Nullable IncomeReport report
    ) {
        if (report == null) {
            return Collections.emptyList();
        }
        List<ReportRowItem> rows = new ArrayList<>();
        if (filter.incomeGranularity == IncomeGranularity.MONTH) {
            for (IncomeReport.MonthlyRow row : report.byMonth()) {
                rows.add(new ReportRowItem(
                        "m-" + row.year() + "-" + row.month(),
                        formatMonthYear(row.year(), row.month()),
                        context.getString(R.string.reports_income_row_meta_format,
                                Formats.formatMoney(row.incomeCents()),
                                Formats.formatMoney(row.expenseCents()),
                                row.movementCount()),
                        null,
                        Formats.formatMoney(row.netCents()),
                        null,
                        false));
            }
        } else {
            for (IncomeReport.DailyRow row : report.byDay()) {
                rows.add(new ReportRowItem(
                        "d-" + row.day(),
                        Formats.formatDayWithShortWeekday(row.day()),
                        context.getString(R.string.reports_income_row_meta_format,
                                Formats.formatMoney(row.incomeCents()),
                                Formats.formatMoney(row.expenseCents()),
                                row.movementCount()),
                        null,
                        Formats.formatMoney(row.netCents()),
                        null,
                        false));
            }
        }
        return rows;
    }

    @NonNull
    private static ReportsUiState mapTopServices(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        TopServicesReport report = payload.topServices;
        long totalRevenue = report == null ? 0L : report.totalRevenueCents();
        int totalCount = report == null ? 0 : report.totalServiceCount();

        List<ReportRowItem> rows = new ArrayList<>();
        if (report != null && filter.serviceGrouping == ServiceGrouping.CATEGORY) {
            for (TopServicesReport.CategoryRow row : report.byCategory()) {
                String label = row.category().isEmpty()
                        ? context.getString(R.string.reports_service_category_empty)
                        : row.category();
                rows.add(new ReportRowItem(
                        "cat-" + label,
                        label,
                        context.getString(R.string.reports_service_row_subtitle_format,
                                label, row.count()),
                        null,
                        Formats.formatMoney(row.revenueCents()),
                        String.valueOf(row.count()),
                        false));
            }
        } else if (report != null) {
            for (TopServicesReport.ServiceRow row : report.byService()) {
                String catLabel = row.category().isEmpty()
                        ? context.getString(R.string.reports_service_category_empty)
                        : row.category();
                rows.add(new ReportRowItem(
                        "svc-" + row.serviceId() + "-" + row.serviceName(),
                        row.serviceName(),
                        context.getString(R.string.reports_service_row_subtitle_format,
                                catLabel, row.count()),
                        null,
                        Formats.formatMoney(row.revenueCents()),
                        String.valueOf(row.count()),
                        false));
            }
        }

        ReportChartMapper.ChartData chart = ReportChartMapper.buildChart(context, filter, payload);

        return new ReportsUiState(
                filter,
                false,
                context.getString(R.string.reports_kpi_services_total),
                Formats.formatMoney(totalRevenue),
                context.getString(R.string.reports_kpi_services_count),
                String.valueOf(totalCount),
                null,
                Collections.emptyList(),
                rows,
                chart.title,
                chart.entries,
                chart.maxValueOverride,
                R.string.reports_empty_top_services,
                payload
        );
    }

    @NonNull
    private static ReportsUiState mapFrequentClients(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        FrequentClientsReport report = payload.frequentClients;
        List<FrequentClientsReport.ClientRow> source = Collections.emptyList();
        if (report != null) {
            source = filter.clientRankingMode == ClientRankingMode.SPEND
                    ? report.topBySpend()
                    : report.topByVisits();
        }

        int totalVisits = 0;
        List<ReportRowItem> rows = new ArrayList<>(source.size());
        for (FrequentClientsReport.ClientRow row : source) {
            totalVisits += row.visitCount();
            String lastDate = Formats.formatDate(row.lastVisitDay());
            String subtitle = filter.includeClientPhone && !row.phone().isEmpty()
                    ? context.getString(R.string.reports_client_row_subtitle_phone_format,
                            row.phone(), row.visitCount(), lastDate)
                    : context.getString(R.string.reports_client_row_subtitle_format,
                            row.visitCount(), lastDate);
            rows.add(new ReportRowItem(
                    "cli-" + row.clientId(),
                    row.clientName(),
                    subtitle,
                    null,
                    Formats.formatMoney(row.totalSpentCents()),
                    null,
                    false));
        }

        ReportChartMapper.ChartData chart = ReportChartMapper.buildChart(context, filter, payload);

        return new ReportsUiState(
                filter,
                false,
                context.getString(R.string.reports_kpi_clients_top_count),
                String.valueOf(source.size()),
                context.getString(R.string.reports_kpi_clients_visits_sum),
                String.valueOf(totalVisits),
                null,
                Collections.emptyList(),
                rows,
                chart.title,
                chart.entries,
                chart.maxValueOverride,
                R.string.reports_empty_frequent_clients,
                payload
        );
    }

    @NonNull
    static String formatDayOfWeek(int dayOfWeek) {
        String raw = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Formats.ES_AR);
        return capitalizeFirst(raw);
    }

    @NonNull
    static String formatMonthYear(int year, int month) {
        String raw = LocalDate.of(year, month, 1).format(MONTH_YEAR_FORMATTER);
        return capitalizeFirst(raw);
    }

    @NonNull
    private static String capitalizeFirst(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
