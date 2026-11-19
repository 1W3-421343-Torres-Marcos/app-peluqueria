package com.metanoia.app.util;

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
import com.metanoia.domain.model.TopServicesReport;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportPdfSpecTest {

    @Test
    public void factories_buildAllSixReportsWithoutLeakingNotesOrDefaultPhones() {
        long day = LocalDate.of(2026, 4, 10).toEpochDay();

        IncomeReport income = IncomeReport.fromDailyAndMethodRows(
                Collections.singletonList(new IncomeReport.DailyRow(day, 5_000_00L, 1_000_00L, 3)),
                Collections.singletonList(new IncomeReport.MethodRow(
                        PaymentMethod.CASH, 5_000_00L, 1_000_00L, 3))
        );
        ReportPdfSpec incomeSpec = ReportPdfSpecFactory.forIncome(
                "Filtro", income, IncomeGranularity.DAY);
        assertEquals(3, incomeSpec.kpis.size());
        assertEquals(2, incomeSpec.sections.size());
        assertEquals(1, incomeSpec.estimatePageCount());

        TopServicesReport services = new TopServicesReport(
                Collections.singletonList(new TopServicesReport.ServiceRow(
                        "s1", "Corte", "Peluqueria", 2, 3_000_00L)),
                Collections.emptyList()
        );
        ReportPdfSpec servicesSpec = ReportPdfSpecFactory.forTopServices(
                "Filtro", services, ServiceGrouping.SERVICE);
        assertEquals("Corte", servicesSpec.sections.get(0).rows.get(0).get(0));

        FrequentClientsReport clients = new FrequentClientsReport(
                20,
                Collections.singletonList(new FrequentClientsReport.ClientRow(
                        "c1", "Sofía", "5491199998888", 4, 9_000_00L, day)),
                Collections.emptyList()
        );
        ReportPdfSpec noPhoneSpec = ReportPdfSpecFactory.forFrequentClients(
                "Filtro", clients, ClientRankingMode.VISITS, false);
        assertEquals(4, noPhoneSpec.sections.get(0).columns.size());
        assertFalse(noPhoneSpec.sections.get(0).rows.get(0).contains("5491199998888"));

        ReportPdfSpec withPhoneSpec = ReportPdfSpecFactory.forFrequentClients(
                "Filtro", clients, ClientRankingMode.VISITS, true);
        assertEquals(5, withPhoneSpec.sections.get(0).columns.size());
        assertTrue(withPhoneSpec.sections.get(0).rows.get(0).contains("5491199998888"));

        OccupancyReport occ = new OccupancyReport(
                60L, 120L, 5000,
                Collections.singletonList(new OccupancyReport.HourSlotRow(9, 60L, 120L, 5000, 1)),
                Collections.emptyList(),
                Collections.singletonList(new OccupancyReport.WindowRow(1, 9, 60L, 120L, 5000, 1))
        );
        ReportPdfSpec occSpec = ReportPdfSpecFactory.forOccupancy(
                "Filtro", occ, OccupancyGrouping.HOUR);
        assertEquals(2, occSpec.sections.size());

        ProfessionalPerformanceReport perf = new ProfessionalPerformanceReport(
                Collections.singletonList(new ProfessionalPerformanceReport.ProfessionalRow(
                        "p1", "Ana", 3, 6_000_00L, 0, 2_400_00L))
        );
        assertEquals(1, ReportPdfSpecFactory.forProfessionalPerformance("Filtro", perf)
                .sections.get(0).rows.size());

        ReceivablesReport rec = new ReceivablesReport(
                2_000_00L,
                2,
                Collections.singletonList(new ReceivablesReport.ClientReceivableRow(
                        "c1", "Sofía", 1, 500_00L, day))
        );
        ReportPdfSpec recSpec = ReportPdfSpecFactory.forReceivables("Filtro", rec);
        assertEquals(1, recSpec.notesFooter.size());
    }

    @Test
    public void paginate_splitsLargeTableAcrossMultiplePages() {
        long baseDay = LocalDate.of(2026, 1, 1).toEpochDay();
        List<IncomeReport.DailyRow> manyDays = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            manyDays.add(new IncomeReport.DailyRow(baseDay + i, 1_000_00L, 200_00L, 2));
        }
        IncomeReport report = IncomeReport.fromDailyAndMethodRows(manyDays, Collections.emptyList());
        ReportPdfSpec spec = ReportPdfSpecFactory.forIncome(
                "60 días", report, IncomeGranularity.DAY);

        List<List<ReportPdfSpec.TableSection>> pages = spec.paginate();
        assertTrue(pages.size() >= 2);
        int totalRowsAcrossPages = 0;
        for (List<ReportPdfSpec.TableSection> pageSections : pages) {
            for (ReportPdfSpec.TableSection s : pageSections) {
                totalRowsAcrossPages += s.rows.size();
            }
        }
        assertEquals(60, totalRowsAcrossPages);
    }
}
