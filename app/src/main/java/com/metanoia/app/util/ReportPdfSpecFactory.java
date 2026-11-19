package com.metanoia.app.util;

import androidx.annotation.NonNull;

import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.app.util.ReportPdfSpec.KpiPair;
import com.metanoia.app.util.ReportPdfSpec.TableColumn;
import com.metanoia.app.util.ReportPdfSpec.TableSection;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.TopServicesReport;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Fábrica de especificaciones {@link ReportPdfSpec} para los 6 reportes de Metanoia.
 */
public final class ReportPdfSpecFactory {

    private ReportPdfSpecFactory() {
    }

    @NonNull
    public static ReportPdfSpec forIncome(
            @NonNull String filterSummary,
            @NonNull IncomeReport report,
            @NonNull IncomeGranularity granularity
    ) {
        List<KpiPair> kpis = Arrays.asList(
                new KpiPair("Resultado neto", Formats.formatMoney(report.netCents())),
                new KpiPair("Ingresos", Formats.formatMoney(report.totalIncomeCents())),
                new KpiPair("Egresos", Formats.formatMoney(report.totalExpenseCents()))
        );
        List<List<String>> rows = new ArrayList<>();
        if (granularity == IncomeGranularity.MONTH) {
            for (IncomeReport.MonthlyRow row : report.byMonth()) {
                rows.add(Arrays.asList(
                        String.format(Locale.ROOT, "%02d/%04d", row.month(), row.year()),
                        Formats.formatMoney(row.incomeCents()),
                        Formats.formatMoney(row.expenseCents()),
                        Formats.formatMoney(row.netCents())));
            }
        } else {
            for (IncomeReport.DailyRow row : report.byDay()) {
                rows.add(Arrays.asList(
                        Formats.formatDate(row.day()),
                        Formats.formatMoney(row.incomeCents()),
                        Formats.formatMoney(row.expenseCents()),
                        Formats.formatMoney(row.netCents())));
            }
        }
        List<TableSection> sections = new ArrayList<>();
        sections.add(new TableSection(
                granularity == IncomeGranularity.MONTH ? "Detalle mensual" : "Detalle diario",
                Arrays.asList(
                        new TableColumn("Período", 2.2f, false),
                        new TableColumn("Ingresos", 1.8f, true),
                        new TableColumn("Egresos", 1.8f, true),
                        new TableColumn("Neto", 1.8f, true)),
                rows));
        if (!report.byMethod().isEmpty()) {
            List<List<String>> methodRows = new ArrayList<>();
            for (IncomeReport.MethodRow m : report.byMethod()) {
                methodRows.add(Arrays.asList(
                        methodLabel(m.method()),
                        Formats.formatMoney(m.incomeCents()),
                        Formats.formatMoney(m.expenseCents()),
                        Formats.formatMoney(m.netCents())));
            }
            sections.add(new TableSection(
                    "Desglose por método de pago",
                    Arrays.asList(
                            new TableColumn("Método", 2.2f, false),
                            new TableColumn("Ingresos", 1.8f, true),
                            new TableColumn("Egresos", 1.8f, true),
                            new TableColumn("Neto", 1.8f, true)),
                    methodRows));
        }
        return new ReportPdfSpec("Reporte de Ingresos", filterSummary, kpis, sections,
                Collections.emptyList());
    }

    @NonNull
    public static ReportPdfSpec forTopServices(
            @NonNull String filterSummary,
            @NonNull TopServicesReport report,
            @NonNull ServiceGrouping grouping
    ) {
        List<KpiPair> kpis = Arrays.asList(
                new KpiPair("Facturación ítems", Formats.formatMoney(report.totalRevenueCents())),
                new KpiPair("Servicios realizados", String.valueOf(report.totalServiceCount()))
        );
        List<TableColumn> cols;
        List<List<String>> rows = new ArrayList<>();
        if (grouping == ServiceGrouping.CATEGORY) {
            cols = Arrays.asList(
                    new TableColumn("Categoría", 3.5f, false),
                    new TableColumn("Cantidad", 1.2f, true),
                    new TableColumn("Facturación", 2.0f, true));
            for (TopServicesReport.CategoryRow r : report.byCategory()) {
                rows.add(Arrays.asList(
                        r.category().isEmpty() ? "Sin categoría" : r.category(),
                        String.valueOf(r.count()),
                        Formats.formatMoney(r.revenueCents())));
            }
        } else {
            cols = Arrays.asList(
                    new TableColumn("Servicio", 2.8f, false),
                    new TableColumn("Categoría", 1.8f, false),
                    new TableColumn("Cant.", 1.0f, true),
                    new TableColumn("Facturación", 1.8f, true));
            for (TopServicesReport.ServiceRow r : report.byService()) {
                rows.add(Arrays.asList(
                        r.serviceName(),
                        r.category().isEmpty() ? "Sin categoría" : r.category(),
                        String.valueOf(r.count()),
                        Formats.formatMoney(r.revenueCents())));
            }
        }
        return new ReportPdfSpec("Servicios más solicitados", filterSummary, kpis,
                Collections.singletonList(new TableSection(null, cols, rows)),
                Collections.emptyList());
    }

    @NonNull
    public static ReportPdfSpec forFrequentClients(
            @NonNull String filterSummary,
            @NonNull FrequentClientsReport report,
            @NonNull ClientRankingMode mode,
            boolean includePhone
    ) {
        List<FrequentClientsReport.ClientRow> source = mode == ClientRankingMode.SPEND
                ? report.topBySpend()
                : report.topByVisits();
        int totalVisits = 0;
        for (FrequentClientsReport.ClientRow r : source) {
            totalVisits += r.visitCount();
        }
        List<KpiPair> kpis = Arrays.asList(
                new KpiPair("Clientes en ranking", String.valueOf(source.size())),
                new KpiPair("Visitas acumuladas", String.valueOf(totalVisits))
        );
        List<TableColumn> cols = includePhone
                ? Arrays.asList(
                        new TableColumn("Cliente", 2.4f, false),
                        new TableColumn("Teléfono", 1.8f, false),
                        new TableColumn("Visitas", 1.0f, true),
                        new TableColumn("Gasto total", 1.8f, true),
                        new TableColumn("Última", 1.4f, true))
                : Arrays.asList(
                        new TableColumn("Cliente", 3.2f, false),
                        new TableColumn("Visitas", 1.1f, true),
                        new TableColumn("Gasto total", 2.0f, true),
                        new TableColumn("Última visita", 1.5f, true));
        List<List<String>> rows = new ArrayList<>(source.size());
        for (FrequentClientsReport.ClientRow r : source) {
            if (includePhone) {
                rows.add(Arrays.asList(r.clientName(), r.phone(), String.valueOf(r.visitCount()),
                        Formats.formatMoney(r.totalSpentCents()),
                        Formats.formatDate(r.lastVisitDay())));
            } else {
                rows.add(Arrays.asList(r.clientName(), String.valueOf(r.visitCount()),
                        Formats.formatMoney(r.totalSpentCents()),
                        Formats.formatDate(r.lastVisitDay())));
            }
        }
        return new ReportPdfSpec("Clientes frecuentes", filterSummary, kpis,
                Collections.singletonList(new TableSection(null, cols, rows)),
                Collections.emptyList());
    }

    @NonNull
    public static ReportPdfSpec forOccupancy(
            @NonNull String filterSummary,
            @NonNull OccupancyReport report,
            @NonNull OccupancyGrouping grouping
    ) {
        List<KpiPair> kpis = Arrays.asList(
                new KpiPair("Ocupación global",
                        Formats.formatPercent(report.overallOccupancyBasisPoints())),
                new KpiPair("Minutos ocupados", String.valueOf(report.totalOccupiedMinutes())),
                new KpiPair("Minutos laborables", String.valueOf(report.totalWorkingMinutes()))
        );
        List<List<String>> rows = new ArrayList<>();
        if (grouping == OccupancyGrouping.DAY) {
            for (OccupancyReport.DayOfWeekRow r : report.byDayOfWeek()) {
                rows.add(Arrays.asList(dayName(r.dayOfWeek()),
                        String.valueOf(r.occupiedMinutes()),
                        String.valueOf(r.workingMinutes()),
                        Formats.formatPercent(r.occupancyBasisPoints()),
                        String.valueOf(r.appointmentCount())));
            }
        } else {
            for (OccupancyReport.HourSlotRow r : report.byHourSlot()) {
                rows.add(Arrays.asList(hourRange(r.slotHour()),
                        String.valueOf(r.occupiedMinutes()),
                        String.valueOf(r.workingMinutes()),
                        Formats.formatPercent(r.occupancyBasisPoints()),
                        String.valueOf(r.appointmentCount())));
            }
        }
        List<TableSection> sections = new ArrayList<>();
        sections.add(new TableSection(
                grouping == OccupancyGrouping.DAY ? "Ocupación por día" : "Ocupación por franja",
                Arrays.asList(
                        new TableColumn(grouping == OccupancyGrouping.DAY ? "Día" : "Franja", 2.2f, false),
                        new TableColumn("Ocup. (m)", 1.2f, true),
                        new TableColumn("Lab. (m)", 1.2f, true),
                        new TableColumn("Ocup. %", 1.3f, true),
                        new TableColumn("Turnos", 1.0f, true)),
                rows));
        if (!report.lowestDemandWindows().isEmpty()) {
            List<List<String>> lowRows = new ArrayList<>();
            for (OccupancyReport.WindowRow w : report.lowestDemandWindows()) {
                lowRows.add(Arrays.asList(
                        dayName(w.dayOfWeek()),
                        hourRange(w.slotHour()),
                        Formats.formatPercent(w.occupancyBasisPoints())));
            }
            sections.add(new TableSection(
                    "Horarios con menor demanda",
                    Arrays.asList(
                            new TableColumn("Día", 2.2f, false),
                            new TableColumn("Franja", 2.2f, false),
                            new TableColumn("Ocupación %", 1.6f, true)),
                    lowRows));
        }
        return new ReportPdfSpec("Reporte de Ocupación", filterSummary, kpis, sections,
                Collections.emptyList());
    }

    @NonNull
    public static ReportPdfSpec forProfessionalPerformance(
            @NonNull String filterSummary,
            @NonNull ProfessionalPerformanceReport report
    ) {
        List<KpiPair> kpis = Arrays.asList(
                new KpiPair("Facturación total", Formats.formatMoney(report.totalBilledCents())),
                new KpiPair("Ticket promedio",
                        Formats.formatMoney(report.overallAverageTicketCents())),
                new KpiPair("Comisiones", Formats.formatMoney(report.totalCommissionCents()))
        );
        List<List<String>> rows = new ArrayList<>();
        for (ProfessionalPerformanceReport.ProfessionalRow r : report.rows()) {
            rows.add(Arrays.asList(
                    r.professionalName(),
                    String.valueOf(r.completedAppointments()),
                    Formats.formatMoney(r.billedCents()),
                    Formats.formatMoney(r.averageTicketCents()),
                    String.valueOf(r.noShowCount()),
                    Formats.formatMoney(r.commissionCents())));
        }
        return new ReportPdfSpec("Rendimiento por profesional", filterSummary, kpis,
                Collections.singletonList(new TableSection(null,
                        Arrays.asList(
                                new TableColumn("Profesional", 2.1f, false),
                                new TableColumn("Turnos", 0.9f, true),
                                new TableColumn("Facturado", 1.6f, true),
                                new TableColumn("Ticket", 1.4f, true),
                                new TableColumn("Aus.", 0.7f, true),
                                new TableColumn("Comisión", 1.5f, true)),
                        rows)),
                Collections.emptyList());
    }

    @NonNull
    public static ReportPdfSpec forReceivables(
            @NonNull String filterSummary,
            @NonNull ReceivablesReport report
    ) {
        List<KpiPair> kpis = Arrays.asList(
                new KpiPair("Saldo por cobrar", Formats.formatMoney(report.totalBalanceCents())),
                new KpiPair("Clientes / Turnos",
                        report.clientCount() + " / " + report.totalAppointmentCount())
        );
        List<List<String>> rows = new ArrayList<>();
        for (ReceivablesReport.ClientReceivableRow r : report.byClient()) {
            rows.add(Arrays.asList(
                    r.clientName(),
                    String.valueOf(r.appointmentCount()),
                    Formats.formatDate(r.oldestDay()),
                    Formats.formatMoney(r.balanceCents())));
        }
        List<String> footer = report.historicalTotalBalanceCents() > report.totalBalanceCents()
                ? Collections.singletonList("Histórico acumulado (todos los períodos): "
                        + Formats.formatMoney(report.historicalTotalBalanceCents())
                        + " en " + report.historicalAppointmentCount() + " turnos.")
                : Collections.emptyList();
        return new ReportPdfSpec("Saldos por cobrar", filterSummary, kpis,
                Collections.singletonList(new TableSection(null,
                        Arrays.asList(
                                new TableColumn("Cliente", 3.0f, false),
                                new TableColumn("Turnos", 1.1f, true),
                                new TableColumn("Más antiguo", 1.5f, true),
                                new TableColumn("Saldo", 1.8f, true)),
                        rows)),
                footer);
    }

    private static String methodLabel(PaymentMethod method) {
        switch (method) {
            case TRANSFER:
                return "Transferencia";
            case CARD:
                return "Tarjeta";
            case MERCADO_PAGO:
                return "Mercado Pago";
            case CASH:
            default:
                return "Efectivo";
        }
    }

    private static String dayName(int dow) {
        String raw = DayOfWeek.of(dow).getDisplayName(TextStyle.FULL, Formats.ES_AR);
        return raw.isEmpty() ? "" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static String hourRange(int slotHour) {
        String start = Formats.formatTime(slotHour * 60);
        String end = slotHour == 23 ? "24:00" : Formats.formatTime((slotHour + 1) * 60);
        return start + " – " + end;
    }
}
