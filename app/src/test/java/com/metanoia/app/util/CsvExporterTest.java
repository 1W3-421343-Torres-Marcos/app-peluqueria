package com.metanoia.app.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

public class CsvExporterTest {

    @Test
    public void exportIncome_writesUtf8BomSemicolonCommaDecimalsAndDdMmYyyy() {
        long day = LocalDate.of(2026, 2, 14).toEpochDay();
        IncomeReport report = IncomeReport.fromDailyAndMethodRows(
                Collections.singletonList(
                        new IncomeReport.DailyRow(day, 12_345_50L, 12_500_75L, 4)),
                Collections.singletonList(
                        new IncomeReport.MethodRow(PaymentMethod.CASH, 12_345_50L, 12_500_75L, 4))
        );

        byte[] raw = CsvExporter.exportIncome(report, IncomeGranularity.DAY);
        assertTrue(raw.length > 3);
        assertArrayEquals(CsvExporter.UTF8_BOM, Arrays.copyOfRange(raw, 0, 3));

        String text = new String(raw, 3, raw.length - 3, StandardCharsets.UTF_8);
        String[] lines = text.split("\r\n");
        assertEquals("Periodo;Ingresos;Egresos;Neto;Movimientos", lines[0]);
        assertEquals("14/02/2026;12345,50;12500,75;-155,25;4", lines[1]);
        assertEquals("TOTAL;12345,50;12500,75;-155,25;4", lines[2]);
        assertTrue(text.contains("Efectivo;12345,50;12500,75;-155,25;4"));
    }

    @Test
    public void exportFrequentClients_escapesAndRespectsPhoneOptIn() {
        long day = LocalDate.of(2026, 3, 1).toEpochDay();
        FrequentClientsReport report = new FrequentClientsReport(
                10,
                Collections.singletonList(new FrequentClientsReport.ClientRow(
                        "c1", "Ana; \"VIP\"", "5491122334455", 5, 8_000_00L, day)),
                Collections.singletonList(new FrequentClientsReport.ClientRow(
                        "c1", "=SUM(1+1)", "5491122334455", 5, 8_000_00L, day))
        );

        byte[] withoutPhoneBytes = CsvExporter.exportFrequentClients(
                report, ClientRankingMode.VISITS, false);
        String withoutPhone = decodeWithoutBom(withoutPhoneBytes);
        assertFalse(withoutPhone.contains("5491122334455"));
        assertTrue(withoutPhone.contains("Cliente;Visitas;Gasto total;Ultima visita"));
        assertTrue(withoutPhone.contains("\"Ana; \"\"VIP\"\"\";5;8000,00;01/03/2026"));

        byte[] withPhoneBytes = CsvExporter.exportFrequentClients(
                report, ClientRankingMode.SPEND, true);
        String withPhone = decodeWithoutBom(withPhoneBytes);
        assertTrue(withPhone.contains("Cliente;Telefono;Visitas;Gasto total;Ultima visita"));
        assertTrue(withPhone.contains("'=SUM(1+1);5491122334455;5;8000,00;01/03/2026"));
    }

    @Test
    public void exportRemainingFourReports_formatExpectedColumns() {
        long day = LocalDate.of(2026, 1, 5).toEpochDay();

        TopServicesReport services = new TopServicesReport(
                Collections.singletonList(new TopServicesReport.ServiceRow(
                        "s1", "Corte", "Peluqueria", 3, 3_000_00L)),
                Collections.singletonList(new TopServicesReport.CategoryRow(
                        "Peluqueria", 3, 3_000_00L))
        );
        assertTrue(decodeWithoutBom(CsvExporter.exportTopServices(services, ServiceGrouping.SERVICE))
                .contains("Corte;Peluqueria;3;3000,00"));

        OccupancyReport occ = new OccupancyReport(
                30L,
                60L,
                5000,
                Collections.singletonList(new OccupancyReport.HourSlotRow(9, 30L, 60L, 5000, 1)),
                Collections.singletonList(new OccupancyReport.DayOfWeekRow(1, 30L, 60L, 5000, 1)),
                Collections.singletonList(new OccupancyReport.WindowRow(1, 9, 30L, 60L, 5000, 1))
        );
        String occCsv = decodeWithoutBom(CsvExporter.exportOccupancy(occ, OccupancyGrouping.HOUR));
        assertTrue(occCsv.contains("09:00 - 10:00;30;60;50,00;1"));
        assertTrue(occCsv.contains("Horarios con menor demanda;Horario;Minutos ocupados"));

        ProfessionalPerformanceReport perf = new ProfessionalPerformanceReport(
                Collections.singletonList(new ProfessionalPerformanceReport.ProfessionalRow(
                        "p1", "Ana", 4, 10_000_00L, 1, 4_000_00L))
        );
        assertTrue(decodeWithoutBom(CsvExporter.exportProfessionalPerformance(perf))
                .contains("Ana;4;10000,00;2500,00;1;4000,00"));

        ReceivablesReport rec = new ReceivablesReport(
                2_500_00L,
                2,
                Collections.singletonList(new ReceivablesReport.ClientReceivableRow(
                        "c1", "Sofía", 1, 1_000_00L, day))
        );
        String recCsv = decodeWithoutBom(CsvExporter.exportReceivables(rec));
        assertTrue(recCsv.contains("Sofía;1;1000,00;05/01/2026"));
        assertTrue(recCsv.contains("HISTORICO ACUMULADO;2;2500,00;"));
        assertEquals("metanoia_ingresos_20260105_20260105.csv",
                CsvExporter.suggestFileName(ReportType.INCOME, day, day, "csv"));
    }

    private static String decodeWithoutBom(byte[] raw) {
        return new String(raw, 3, raw.length - 3, StandardCharsets.UTF_8);
    }
}
