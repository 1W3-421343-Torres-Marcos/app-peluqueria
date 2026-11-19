package com.metanoia.app.util;

import android.content.Context;

import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.ReportType;
import com.metanoia.domain.model.TopServicesReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Exportador CSV acorde a {@code docs/design.md} §9:
 * UTF-8 con BOM, separador {@code ;}, decimales con coma y fechas {@code dd/MM/yyyy}.
 */
public final class CsvExporter {

    public static final byte[] UTF8_BOM = new byte[]{
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBF
    };
    private static final String DELIMITER = ";";
    private static final String CRLF = "\r\n";
    private static final String SHARED_DIR = "shared_reports";
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private CsvExporter() {
    }

    public static byte[] exportIncome(IncomeReport report, IncomeGranularity granularity) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, "Periodo", "Ingresos", "Egresos", "Neto", "Movimientos");
        if (granularity == IncomeGranularity.MONTH) {
            for (IncomeReport.MonthlyRow row : report.byMonth()) {
                appendRow(sb,
                        String.format(Locale.ROOT, "%02d/%04d", row.month(), row.year()),
                        decimalCents(row.incomeCents()),
                        decimalCents(row.expenseCents()),
                        decimalCents(row.netCents()),
                        String.valueOf(row.movementCount()));
            }
        } else {
            for (IncomeReport.DailyRow row : report.byDay()) {
                appendRow(sb,
                        Formats.formatDate(row.day()),
                        decimalCents(row.incomeCents()),
                        decimalCents(row.expenseCents()),
                        decimalCents(row.netCents()),
                        String.valueOf(row.movementCount()));
            }
        }
        appendRow(sb,
                "TOTAL",
                decimalCents(report.totalIncomeCents()),
                decimalCents(report.totalExpenseCents()),
                decimalCents(report.netCents()),
                String.valueOf(report.totalMovementCount()));

        if (!report.byMethod().isEmpty()) {
            sb.append(CRLF);
            appendRow(sb, "Metodo de pago", "Ingresos", "Egresos", "Neto", "Movimientos");
            for (IncomeReport.MethodRow row : report.byMethod()) {
                appendRow(sb,
                        methodLabel(row.method()),
                        decimalCents(row.incomeCents()),
                        decimalCents(row.expenseCents()),
                        decimalCents(row.netCents()),
                        String.valueOf(row.movementCount()));
            }
        }
        return toBomUtf8Bytes(sb.toString());
    }

    public static byte[] exportTopServices(TopServicesReport report, ServiceGrouping grouping) {
        StringBuilder sb = new StringBuilder();
        if (grouping == ServiceGrouping.CATEGORY) {
            appendRow(sb, "Categoria", "Cantidad", "Facturacion");
            for (TopServicesReport.CategoryRow row : report.byCategory()) {
                String cat = row.category().isEmpty() ? "Sin categoria" : row.category();
                appendRow(sb, cat, String.valueOf(row.count()), decimalCents(row.revenueCents()));
            }
            appendRow(sb, "TOTAL", String.valueOf(report.totalServiceCount()),
                    decimalCents(report.totalRevenueCents()));
        } else {
            appendRow(sb, "Servicio", "Categoria", "Cantidad", "Facturacion");
            for (TopServicesReport.ServiceRow row : report.byService()) {
                String cat = row.category().isEmpty() ? "Sin categoria" : row.category();
                appendRow(sb,
                        row.serviceName(),
                        cat,
                        String.valueOf(row.count()),
                        decimalCents(row.revenueCents()));
            }
            appendRow(sb, "TOTAL", "", String.valueOf(report.totalServiceCount()),
                    decimalCents(report.totalRevenueCents()));
        }
        return toBomUtf8Bytes(sb.toString());
    }

    public static byte[] exportFrequentClients(
            FrequentClientsReport report,
            ClientRankingMode rankingMode,
            boolean includePhone
    ) {
        StringBuilder sb = new StringBuilder();
        if (includePhone) {
            appendRow(sb, "Cliente", "Telefono", "Visitas", "Gasto total", "Ultima visita");
        } else {
            appendRow(sb, "Cliente", "Visitas", "Gasto total", "Ultima visita");
        }
        List<FrequentClientsReport.ClientRow> rows = rankingMode == ClientRankingMode.SPEND
                ? report.topBySpend()
                : report.topByVisits();
        for (FrequentClientsReport.ClientRow row : rows) {
            if (includePhone) {
                appendRow(sb,
                        row.clientName(),
                        row.phone(),
                        String.valueOf(row.visitCount()),
                        decimalCents(row.totalSpentCents()),
                        Formats.formatDate(row.lastVisitDay()));
            } else {
                appendRow(sb,
                        row.clientName(),
                        String.valueOf(row.visitCount()),
                        decimalCents(row.totalSpentCents()),
                        Formats.formatDate(row.lastVisitDay()));
            }
        }
        return toBomUtf8Bytes(sb.toString());
    }

    public static byte[] exportOccupancy(OccupancyReport report, OccupancyGrouping grouping) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, grouping == OccupancyGrouping.DAY ? "Dia" : "Franja horaria",
                "Minutos ocupados", "Minutos laborables", "Ocupacion %", "Turnos");
        if (grouping == OccupancyGrouping.DAY) {
            for (OccupancyReport.DayOfWeekRow row : report.byDayOfWeek()) {
                appendRow(sb,
                        dayName(row.dayOfWeek()),
                        String.valueOf(row.occupiedMinutes()),
                        String.valueOf(row.workingMinutes()),
                        decimalCents(row.occupancyBasisPoints()),
                        String.valueOf(row.appointmentCount()));
            }
        } else {
            for (OccupancyReport.HourSlotRow row : report.byHourSlot()) {
                appendRow(sb,
                        hourRangeLabel(row.slotHour()),
                        String.valueOf(row.occupiedMinutes()),
                        String.valueOf(row.workingMinutes()),
                        decimalCents(row.occupancyBasisPoints()),
                        String.valueOf(row.appointmentCount()));
            }
        }
        appendRow(sb,
                "TOTAL",
                String.valueOf(report.totalOccupiedMinutes()),
                String.valueOf(report.totalWorkingMinutes()),
                decimalCents(report.overallOccupancyBasisPoints()),
                "");

        if (!report.lowestDemandWindows().isEmpty()) {
            sb.append(CRLF);
            appendRow(sb, "Horarios con menor demanda", "Horario",
                    "Minutos ocupados", "Minutos laborables", "Ocupacion %");
            for (OccupancyReport.WindowRow w : report.lowestDemandWindows()) {
                appendRow(sb,
                        dayName(w.dayOfWeek()),
                        hourRangeLabel(w.slotHour()),
                        String.valueOf(w.occupiedMinutes()),
                        String.valueOf(w.workingMinutes()),
                        decimalCents(w.occupancyBasisPoints()));
            }
        }
        return toBomUtf8Bytes(sb.toString());
    }

    public static byte[] exportProfessionalPerformance(ProfessionalPerformanceReport report) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, "Profesional", "Turnos realizados", "Facturacion",
                "Ticket promedio", "Ausencias", "Comision");
        for (ProfessionalPerformanceReport.ProfessionalRow row : report.rows()) {
            appendRow(sb,
                    row.professionalName(),
                    String.valueOf(row.completedAppointments()),
                    decimalCents(row.billedCents()),
                    decimalCents(row.averageTicketCents()),
                    String.valueOf(row.noShowCount()),
                    decimalCents(row.commissionCents()));
        }
        appendRow(sb,
                "TOTAL",
                String.valueOf(report.totalCompletedAppointments()),
                decimalCents(report.totalBilledCents()),
                decimalCents(report.overallAverageTicketCents()),
                String.valueOf(report.totalNoShowCount()),
                decimalCents(report.totalCommissionCents()));
        return toBomUtf8Bytes(sb.toString());
    }

    public static byte[] exportReceivables(ReceivablesReport report) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, "Cliente", "Turnos pendientes", "Saldo pendiente", "Turno mas antiguo");
        for (ReceivablesReport.ClientReceivableRow row : report.byClient()) {
            appendRow(sb,
                    row.clientName(),
                    String.valueOf(row.appointmentCount()),
                    decimalCents(row.balanceCents()),
                    Formats.formatDate(row.oldestDay()));
        }
        appendRow(sb,
                "TOTAL PERIODO",
                String.valueOf(report.totalAppointmentCount()),
                decimalCents(report.totalBalanceCents()),
                "");
        if (report.historicalTotalBalanceCents() > report.totalBalanceCents()) {
            appendRow(sb,
                    "HISTORICO ACUMULADO",
                    String.valueOf(report.historicalAppointmentCount()),
                    decimalCents(report.historicalTotalBalanceCents()),
                    "");
        }
        return toBomUtf8Bytes(sb.toString());
    }

    public static String suggestFileName(
            ReportType type,
            long fromDay,
            long toDay,
            String extension
    ) {
        String slug;
        switch (type) {
            case TOP_SERVICES:
                slug = "servicios_top";
                break;
            case FREQUENT_CLIENTS:
                slug = "clientes_frecuentes";
                break;
            case OCCUPANCY:
                slug = "ocupacion";
                break;
            case PROFESSIONAL_PERFORMANCE:
                slug = "rendimiento_profesionales";
                break;
            case RECEIVABLES:
                slug = "por_cobrar";
                break;
            case INCOME:
            default:
                slug = "ingresos";
                break;
        }
        String fromStr = LocalDate.ofEpochDay(fromDay).format(FILE_DATE);
        String toStr = LocalDate.ofEpochDay(toDay).format(FILE_DATE);
        return "metanoia_" + slug + "_" + fromStr + "_" + toStr + "." + extension;
    }

    public static void writeBytes(byte[] bytes, OutputStream outputStream) throws IOException {
        outputStream.write(bytes);
        outputStream.flush();
    }

    public static File writeTempShareFile(Context context, String fileName, byte[] bytes)
            throws IOException {
        File dir = new File(context.getCacheDir(), SHARED_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta temporal de reportes");
        }
        File outFile = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            writeBytes(bytes, fos);
        }
        return outFile;
    }

    static String decimalCents(long valueInHundredths) {
        BigDecimal decimal = BigDecimal.valueOf(valueInHundredths).movePointLeft(2);
        return decimal.toPlainString().replace('.', ',');
    }

    static String formatCell(String raw, boolean protectFormula) {
        String value = raw == null ? "" : raw;
        if (protectFormula && !value.isEmpty()) {
            char first = value.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@'
                    || first == '\t' || first == '\r') {
                value = "'" + value;
            }
        }
        boolean needsQuotes = value.contains(DELIMITER)
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static void appendRow(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            String cell = cells[i] == null ? "" : cells[i];
            boolean numericLike = isNumericDecimalOrInteger(cell);
            sb.append(formatCell(cell, !numericLike));
        }
        sb.append(CRLF);
    }

    private static boolean isNumericDecimalOrInteger(String value) {
        if (value.isEmpty()) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        boolean seenComma = false;
        for (int i = start; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ',') {
                if (seenComma || i == start || i == value.length() - 1) {
                    return false;
                }
                seenComma = true;
            } else if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    private static byte[] toBomUtf8Bytes(String content) {
        byte[] utf8 = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + utf8.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(utf8, 0, result, UTF8_BOM.length, utf8.length);
        return result;
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

    private static String dayName(int dayOfWeek) {
        String raw = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Formats.ES_AR);
        return raw.isEmpty() ? "" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static String hourRangeLabel(int slotHour) {
        String start = Formats.formatTime(slotHour * 60);
        String end = slotHour == 23 ? "24:00" : Formats.formatTime((slotHour + 1) * 60);
        return start + " - " + end;
    }
}
