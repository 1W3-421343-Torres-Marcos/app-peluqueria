package com.metanoia.domain.model;

import static org.junit.Assert.assertEquals;

import com.metanoia.domain.model.IncomeReport.DailyRow;
import com.metanoia.domain.model.IncomeReport.MethodRow;
import com.metanoia.domain.model.IncomeReport.MonthlyRow;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class IncomeReportTest {

    @Test
    public void fromDailyAndMethodRows_computesTotalsAndChronologicalMonthlyRollups() {
        long dec10 = LocalDate.of(2025, 12, 10).toEpochDay();
        long dec20 = LocalDate.of(2025, 12, 20).toEpochDay();
        long jan05 = LocalDate.of(2026, 1, 5).toEpochDay();

        List<DailyRow> dailyRows = Arrays.asList(
                new DailyRow(dec10, 1_000_00L, 200_00L, 3),
                new DailyRow(dec20, 500_00L, 100_00L, 2),
                new DailyRow(jan05, 800_00L, 900_00L, 4)
        );
        List<MethodRow> methodRows = Arrays.asList(
                new MethodRow(PaymentMethod.CASH, 1_500_00L, 1_200_00L, 7),
                new MethodRow(PaymentMethod.TRANSFER, 800_00L, 0L, 2)
        );

        IncomeReport report = IncomeReport.fromDailyAndMethodRows(dailyRows, methodRows);

        assertEquals(2_300_00L, report.totalIncomeCents());
        assertEquals(1_200_00L, report.totalExpenseCents());
        assertEquals(1_100_00L, report.netCents());
        assertEquals(9, report.totalMovementCount());

        List<MonthlyRow> months = report.byMonth();
        assertEquals(2, months.size());

        assertEquals(2025, months.get(0).year());
        assertEquals(12, months.get(0).month());
        assertEquals(1_500_00L, months.get(0).incomeCents());
        assertEquals(300_00L, months.get(0).expenseCents());
        assertEquals(1_200_00L, months.get(0).netCents());
        assertEquals(5, months.get(0).movementCount());

        assertEquals(2026, months.get(1).year());
        assertEquals(1, months.get(1).month());
        assertEquals(800_00L, months.get(1).incomeCents());
        assertEquals(900_00L, months.get(1).expenseCents());
        assertEquals(-100_00L, months.get(1).netCents());
        assertEquals(4, months.get(1).movementCount());
    }
}
