package com.metanoia.app.ui.reports;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.R;
import com.metanoia.app.ui.reports.ReportRowsAdapter.ReportRowItem;
import com.metanoia.app.ui.reports.ReportsUiModels.LoadedPayload;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsUiState;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapea los reportes operativos (ocupación, rendimiento por profesional y saldos por cobrar) a UI.
 */
final class ReportOperationalMapper {

    private ReportOperationalMapper() {
    }

    @NonNull
    static ReportsUiState mapOccupancy(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        OccupancyReport report = payload.occupancy;
        int overallBp = report == null ? 0 : report.overallOccupancyBasisPoints();
        long occMin = report == null ? 0L : report.totalOccupiedMinutes();
        long workMin = report == null ? 0L : report.totalWorkingMinutes();

        List<String> lowestLines = buildLowestDemandLines(context, report);
        List<ReportRowItem> rows = buildOccupancyRows(context, filter, report);
        ReportChartMapper.ChartData chart = ReportChartMapper.buildChart(context, filter, payload);

        return new ReportsUiState(
                filter,
                false,
                context.getString(R.string.reports_kpi_occupancy_rate),
                Formats.formatPercent(overallBp),
                context.getString(R.string.reports_kpi_occupancy_minutes),
                context.getString(R.string.reports_value_pair_format,
                        String.valueOf(occMin), String.valueOf(workMin)),
                lowestLines.isEmpty()
                        ? null
                        : context.getString(R.string.reports_section_lowest_demand),
                lowestLines,
                rows,
                chart.title,
                chart.entries,
                chart.maxValueOverride,
                R.string.reports_empty_occupancy,
                payload
        );
    }

    @NonNull
    private static List<String> buildLowestDemandLines(
            @NonNull Context context,
            @Nullable OccupancyReport report
    ) {
        if (report == null || report.lowestDemandWindows().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(report.lowestDemandWindows().size());
        for (OccupancyReport.WindowRow w : report.lowestDemandWindows()) {
            String hourLabel = Formats.formatTime(w.slotHour() * 60);
            lines.add(context.getString(
                    R.string.reports_lowest_demand_line_format,
                    ReportPresentationMapper.formatDayOfWeek(w.dayOfWeek()),
                    hourLabel,
                    Formats.formatPercent(w.occupancyBasisPoints()),
                    w.occupiedMinutes(),
                    w.workingMinutes()));
        }
        return lines;
    }

    @NonNull
    private static List<ReportRowItem> buildOccupancyRows(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @Nullable OccupancyReport report
    ) {
        if (report == null) {
            return Collections.emptyList();
        }
        List<ReportRowItem> rows = new ArrayList<>();
        if (filter.occupancyGrouping == OccupancyGrouping.DAY) {
            for (OccupancyReport.DayOfWeekRow row : report.byDayOfWeek()) {
                rows.add(new ReportRowItem(
                        "dow-" + row.dayOfWeek(),
                        ReportPresentationMapper.formatDayOfWeek(row.dayOfWeek()),
                        context.getString(R.string.reports_occupancy_row_subtitle_format,
                                row.occupiedMinutes(), row.workingMinutes(),
                                row.appointmentCount()),
                        null,
                        Formats.formatPercent(row.occupancyBasisPoints()),
                        null,
                        false));
            }
        } else {
            for (OccupancyReport.HourSlotRow row : report.byHourSlot()) {
                String start = Formats.formatTime(row.slotHour() * 60);
                String end = row.slotHour() == 23
                        ? "24:00"
                        : Formats.formatTime((row.slotHour() + 1) * 60);
                rows.add(new ReportRowItem(
                        "hr-" + row.slotHour(),
                        context.getString(R.string.reports_occupancy_hour_format, start, end),
                        context.getString(R.string.reports_occupancy_row_subtitle_format,
                                row.occupiedMinutes(), row.workingMinutes(),
                                row.appointmentCount()),
                        null,
                        Formats.formatPercent(row.occupancyBasisPoints()),
                        null,
                        false));
            }
        }
        return rows;
    }

    @NonNull
    static ReportsUiState mapPerformance(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        ProfessionalPerformanceReport report = payload.performance;
        long totalBilled = report == null ? 0L : report.totalBilledCents();
        long avgTicket = report == null ? 0L : report.overallAverageTicketCents();

        List<ReportRowItem> rows = new ArrayList<>();
        if (report != null) {
            for (ProfessionalPerformanceReport.ProfessionalRow row : report.rows()) {
                rows.add(new ReportRowItem(
                        "pro-" + row.professionalId(),
                        row.professionalName(),
                        context.getString(R.string.reports_performance_subtitle_format,
                                row.completedAppointments(),
                                Formats.formatMoney(row.averageTicketCents()),
                                row.noShowCount()),
                        null,
                        Formats.formatMoney(row.billedCents()),
                        context.getString(R.string.reports_performance_commission_format,
                                Formats.formatMoney(row.commissionCents())),
                        false));
            }
        }

        ReportChartMapper.ChartData chart = ReportChartMapper.buildChart(context, filter, payload);

        return new ReportsUiState(
                filter,
                false,
                context.getString(R.string.reports_kpi_performance_billed),
                Formats.formatMoney(totalBilled),
                context.getString(R.string.reports_kpi_performance_ticket),
                Formats.formatMoney(avgTicket),
                null,
                Collections.emptyList(),
                rows,
                chart.title,
                chart.entries,
                chart.maxValueOverride,
                R.string.reports_empty_performance,
                payload
        );
    }

    @NonNull
    static ReportsUiState mapReceivables(
            @NonNull Context context,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        ReceivablesReport report = payload.receivables;
        long balance = report == null ? 0L : report.totalBalanceCents();
        int clientCount = report == null ? 0 : report.clientCount();
        int appts = report == null ? 0 : report.totalAppointmentCount();

        List<String> secondaryLines = Collections.emptyList();
        String secondaryTitle = null;
        if (report != null && report.historicalTotalBalanceCents() > report.totalBalanceCents()) {
            secondaryTitle = context.getString(R.string.reports_section_receivables_historical);
            secondaryLines = Collections.singletonList(context.getString(
                    R.string.reports_receivable_historical_format,
                    Formats.formatMoney(report.historicalTotalBalanceCents()),
                    report.historicalAppointmentCount()));
        }

        List<ReportRowItem> rows = new ArrayList<>();
        if (report != null) {
            for (ReceivablesReport.ClientReceivableRow row : report.byClient()) {
                rows.add(new ReportRowItem(
                        "rec-" + row.clientId(),
                        row.clientName(),
                        context.getString(R.string.reports_receivable_subtitle_format,
                                row.appointmentCount(),
                                Formats.formatDate(row.oldestDay())),
                        null,
                        Formats.formatMoney(row.balanceCents()),
                        null,
                        false));
            }
        }

        ReportChartMapper.ChartData chart = ReportChartMapper.buildChart(context, filter, payload);

        return new ReportsUiState(
                filter,
                false,
                context.getString(R.string.reports_kpi_receivables_balance),
                Formats.formatMoney(balance),
                context.getString(R.string.reports_kpi_receivables_clients),
                context.getString(R.string.reports_value_pair_format,
                        String.valueOf(clientCount), String.valueOf(appts)),
                secondaryTitle,
                secondaryLines,
                rows,
                chart.title,
                chart.entries,
                chart.maxValueOverride,
                R.string.reports_empty_receivables,
                payload
        );
    }
}
