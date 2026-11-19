package com.metanoia.app.ui.reports;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.R;
import com.metanoia.app.ui.common.BarChartView;
import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.LoadedPayload;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.TopServicesReport;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Construye series monocromáticas para {@link BarChartView} a partir de los 6 reportes.
 */
final class ReportChartMapper {

    private static final int MAX_BARS_RANKING = 8;
    private static final long MAX_OCCUPANCY_BP = 10_000L;

    static final class ChartData {
        @NonNull
        final String title;
        @NonNull
        final List<BarChartView.Entry> entries;
        @Nullable
        final Long maxValueOverride;

        ChartData(
                @NonNull String title,
                @NonNull List<BarChartView.Entry> entries,
                @Nullable Long maxValueOverride
        ) {
            this.title = title;
            this.entries = entries;
            this.maxValueOverride = maxValueOverride;
        }
    }

    private ReportChartMapper() {
    }

    @NonNull
    static ChartData buildChart(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        switch (filter.reportType) {
            case TOP_SERVICES:
                return buildServicesChart(context, filter, payload.topServices);
            case FREQUENT_CLIENTS:
                return buildClientsChart(context, filter, payload.frequentClients);
            case OCCUPANCY:
                return buildOccupancyChart(context, filter, payload.occupancy);
            case PROFESSIONAL_PERFORMANCE:
                return buildPerformanceChart(context, payload.performance);
            case RECEIVABLES:
                return buildReceivablesChart(context, payload.receivables);
            case INCOME:
            default:
                return buildIncomeChart(context, filter, payload.income);
        }
    }

    @NonNull
    private static ChartData buildIncomeChart(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @Nullable IncomeReport report
    ) {
        String title = context.getString(R.string.reports_chart_title_income);
        if (report == null) {
            return new ChartData(title, Collections.emptyList(), null);
        }
        List<BarChartView.Entry> entries = new ArrayList<>();
        if (filter.incomeGranularity == IncomeGranularity.MONTH) {
            for (IncomeReport.MonthlyRow row : report.byMonth()) {
                String label = String.format(Locale.ROOT, "%02d/%02d",
                        row.month(), row.year() % 100);
                entries.add(new BarChartView.Entry(
                        label, row.netCents(), compactMoney(row.netCents()), row.netCents() < 0L));
            }
        } else {
            for (IncomeReport.DailyRow row : report.byDay()) {
                LocalDate date = LocalDate.ofEpochDay(row.day());
                String label = String.format(Locale.ROOT, "%02d/%02d",
                        date.getDayOfMonth(), date.getMonthValue());
                entries.add(new BarChartView.Entry(
                        label, row.netCents(), compactMoney(row.netCents()), row.netCents() < 0L));
            }
        }
        return new ChartData(title, entries, null);
    }

    @NonNull
    private static ChartData buildServicesChart(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @Nullable TopServicesReport report
    ) {
        String title = context.getString(R.string.reports_chart_title_services);
        if (report == null) {
            return new ChartData(title, Collections.emptyList(), null);
        }
        List<BarChartView.Entry> entries = new ArrayList<>();
        if (filter.serviceGrouping == ServiceGrouping.CATEGORY) {
            int limit = Math.min(MAX_BARS_RANKING, report.byCategory().size());
            for (int i = 0; i < limit; i++) {
                TopServicesReport.CategoryRow row = report.byCategory().get(i);
                String label = row.category().isEmpty()
                        ? context.getString(R.string.reports_service_category_empty)
                        : row.category();
                entries.add(new BarChartView.Entry(
                        label, row.revenueCents(), compactMoney(row.revenueCents()), false));
            }
        } else {
            int limit = Math.min(MAX_BARS_RANKING, report.byService().size());
            for (int i = 0; i < limit; i++) {
                TopServicesReport.ServiceRow row = report.byService().get(i);
                entries.add(new BarChartView.Entry(
                        row.serviceName(),
                        row.revenueCents(),
                        compactMoney(row.revenueCents()),
                        false));
            }
        }
        return new ChartData(title, entries, null);
    }

    @NonNull
    private static ChartData buildClientsChart(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @Nullable FrequentClientsReport report
    ) {
        boolean bySpend = filter.clientRankingMode == ClientRankingMode.SPEND;
        String title = context.getString(bySpend
                ? R.string.reports_chart_title_clients_spend
                : R.string.reports_chart_title_clients_visits);
        if (report == null) {
            return new ChartData(title, Collections.emptyList(), null);
        }
        List<FrequentClientsReport.ClientRow> source =
                bySpend ? report.topBySpend() : report.topByVisits();
        int limit = Math.min(MAX_BARS_RANKING, source.size());
        List<BarChartView.Entry> entries = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            FrequentClientsReport.ClientRow row = source.get(i);
            long value = bySpend ? row.totalSpentCents() : row.visitCount();
            String formatted = bySpend ? compactMoney(value) : String.valueOf(row.visitCount());
            entries.add(new BarChartView.Entry(row.clientName(), value, formatted, false));
        }
        return new ChartData(title, entries, null);
    }

    @NonNull
    private static ChartData buildOccupancyChart(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @Nullable OccupancyReport report
    ) {
        String title = context.getString(R.string.reports_chart_title_occupancy);
        if (report == null) {
            return new ChartData(title, Collections.emptyList(), MAX_OCCUPANCY_BP);
        }
        Set<Integer> lowHours = new HashSet<>();
        Set<Integer> lowDays = new HashSet<>();
        for (OccupancyReport.WindowRow w : report.lowestDemandWindows()) {
            lowHours.add(w.slotHour());
            lowDays.add(w.dayOfWeek());
        }

        List<BarChartView.Entry> entries = new ArrayList<>();
        if (filter.occupancyGrouping == OccupancyGrouping.DAY) {
            for (OccupancyReport.DayOfWeekRow row : report.byDayOfWeek()) {
                String shortDow = DayOfWeek.of(row.dayOfWeek())
                        .getDisplayName(TextStyle.SHORT, Formats.ES_AR);
                entries.add(new BarChartView.Entry(
                        shortDow,
                        row.occupancyBasisPoints(),
                        (row.occupancyBasisPoints() / 100) + "%",
                        lowDays.contains(row.dayOfWeek())));
            }
        } else {
            for (OccupancyReport.HourSlotRow row : report.byHourSlot()) {
                String label = String.format(Locale.ROOT, "%02dh", row.slotHour());
                entries.add(new BarChartView.Entry(
                        label,
                        row.occupancyBasisPoints(),
                        (row.occupancyBasisPoints() / 100) + "%",
                        lowHours.contains(row.slotHour())));
            }
        }
        return new ChartData(title, entries, MAX_OCCUPANCY_BP);
    }

    @NonNull
    private static ChartData buildPerformanceChart(
            @NonNull Context context,
            @Nullable ProfessionalPerformanceReport report
    ) {
        String title = context.getString(R.string.reports_chart_title_performance);
        if (report == null) {
            return new ChartData(title, Collections.emptyList(), null);
        }
        List<BarChartView.Entry> entries = new ArrayList<>(report.rows().size());
        for (ProfessionalPerformanceReport.ProfessionalRow row : report.rows()) {
            entries.add(new BarChartView.Entry(
                    row.professionalName(),
                    row.billedCents(),
                    compactMoney(row.billedCents()),
                    false));
        }
        return new ChartData(title, entries, null);
    }

    @NonNull
    private static ChartData buildReceivablesChart(
            @NonNull Context context,
            @Nullable ReceivablesReport report
    ) {
        String title = context.getString(R.string.reports_chart_title_receivables);
        if (report == null) {
            return new ChartData(title, Collections.emptyList(), null);
        }
        int limit = Math.min(MAX_BARS_RANKING, report.byClient().size());
        List<BarChartView.Entry> entries = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            ReceivablesReport.ClientReceivableRow row = report.byClient().get(i);
            entries.add(new BarChartView.Entry(
                    row.clientName(),
                    row.balanceCents(),
                    compactMoney(row.balanceCents()),
                    false));
        }
        return new ChartData(title, entries, null);
    }

    @NonNull
    private static String compactMoney(long cents) {
        long pesos = Math.round(cents / 100.0);
        if (Math.abs(pesos) >= 1_000_000L) {
            return String.format(Locale.ROOT, "$%.1fM", pesos / 1_000_000.0);
        }
        if (Math.abs(pesos) >= 1_000L) {
            return String.format(Locale.ROOT, "$%.0fk", pesos / 1_000.0);
        }
        return "$" + pesos;
    }
}
