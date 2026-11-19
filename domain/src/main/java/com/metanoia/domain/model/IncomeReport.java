package com.metanoia.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Reporte inmutable de ingresos y egresos por día, mes y medio de pago desde movimientos de caja.
 */
public final class IncomeReport {

    public static final class DailyRow {
        private final long day;
        private final long incomeCents;
        private final long expenseCents;
        private final long netCents;
        private final int movementCount;

        public DailyRow(
                long day,
                long incomeCents,
                long expenseCents,
                int movementCount
        ) {
            this(day, incomeCents, expenseCents,
                    Math.subtractExact(incomeCents, expenseCents), movementCount);
        }

        public DailyRow(
                long day,
                long incomeCents,
                long expenseCents,
                long netCents,
                int movementCount
        ) {
            this.day = day;
            this.incomeCents = incomeCents;
            this.expenseCents = expenseCents;
            this.netCents = netCents;
            this.movementCount = movementCount;
        }

        public long day() {
            return day;
        }

        public long incomeCents() {
            return incomeCents;
        }

        public long expenseCents() {
            return expenseCents;
        }

        public long netCents() {
            return netCents;
        }

        public int movementCount() {
            return movementCount;
        }
    }

    public static final class MonthlyRow {
        private final int year;
        private final int month;
        private final long incomeCents;
        private final long expenseCents;
        private final long netCents;
        private final int movementCount;

        public MonthlyRow(
                int year,
                int month,
                long incomeCents,
                long expenseCents,
                long netCents,
                int movementCount
        ) {
            this.year = year;
            this.month = month;
            this.incomeCents = incomeCents;
            this.expenseCents = expenseCents;
            this.netCents = netCents;
            this.movementCount = movementCount;
        }

        public int year() {
            return year;
        }

        public int month() {
            return month;
        }

        public long incomeCents() {
            return incomeCents;
        }

        public long expenseCents() {
            return expenseCents;
        }

        public long netCents() {
            return netCents;
        }

        public int movementCount() {
            return movementCount;
        }
    }

    public static final class MethodRow {
        private final PaymentMethod method;
        private final long incomeCents;
        private final long expenseCents;
        private final long netCents;
        private final int movementCount;

        public MethodRow(
                PaymentMethod method,
                long incomeCents,
                long expenseCents,
                int movementCount
        ) {
            this(method, incomeCents, expenseCents,
                    Math.subtractExact(incomeCents, expenseCents), movementCount);
        }

        public MethodRow(
                PaymentMethod method,
                long incomeCents,
                long expenseCents,
                long netCents,
                int movementCount
        ) {
            this.method = Objects.requireNonNull(method, "method");
            this.incomeCents = incomeCents;
            this.expenseCents = expenseCents;
            this.netCents = netCents;
            this.movementCount = movementCount;
        }

        public PaymentMethod method() {
            return method;
        }

        public long incomeCents() {
            return incomeCents;
        }

        public long expenseCents() {
            return expenseCents;
        }

        public long netCents() {
            return netCents;
        }

        public int movementCount() {
            return movementCount;
        }
    }

    private final long totalIncomeCents;
    private final long totalExpenseCents;
    private final long netCents;
    private final int totalMovementCount;
    private final List<DailyRow> byDay;
    private final List<MonthlyRow> byMonth;
    private final List<MethodRow> byMethod;

    public IncomeReport(
            long totalIncomeCents,
            long totalExpenseCents,
            long netCents,
            int totalMovementCount,
            List<DailyRow> byDay,
            List<MonthlyRow> byMonth,
            List<MethodRow> byMethod
    ) {
        this.totalIncomeCents = totalIncomeCents;
        this.totalExpenseCents = totalExpenseCents;
        this.netCents = netCents;
        this.totalMovementCount = totalMovementCount;
        this.byDay = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byDay, "byDay")));
        this.byMonth = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byMonth, "byMonth")));
        this.byMethod = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(byMethod, "byMethod")));
    }

    public static IncomeReport fromDailyAndMethodRows(
            List<DailyRow> dailyRows,
            List<MethodRow> methodRows
    ) {
        Objects.requireNonNull(dailyRows, "dailyRows");
        Objects.requireNonNull(methodRows, "methodRows");

        long totalIncome = 0L;
        long totalExpense = 0L;
        int totalCount = 0;
        Map<YearMonth, long[]> monthlyMap = new TreeMap<>();

        for (DailyRow row : dailyRows) {
            totalIncome = Math.addExact(totalIncome, row.incomeCents());
            totalExpense = Math.addExact(totalExpense, row.expenseCents());
            totalCount += row.movementCount();

            LocalDate date = LocalDate.ofEpochDay(row.day());
            YearMonth ym = YearMonth.of(date.getYear(), date.getMonthValue());
            long[] acc = monthlyMap.computeIfAbsent(ym, k -> new long[3]);
            acc[0] = Math.addExact(acc[0], row.incomeCents());
            acc[1] = Math.addExact(acc[1], row.expenseCents());
            acc[2] += row.movementCount();
        }

        List<MonthlyRow> monthlyRows = new ArrayList<>(monthlyMap.size());
        for (Map.Entry<YearMonth, long[]> entry : monthlyMap.entrySet()) {
            YearMonth ym = entry.getKey();
            long[] acc = entry.getValue();
            monthlyRows.add(new MonthlyRow(
                    ym.getYear(),
                    ym.getMonthValue(),
                    acc[0],
                    acc[1],
                    Math.subtractExact(acc[0], acc[1]),
                    (int) acc[2]
            ));
        }

        return new IncomeReport(
                totalIncome,
                totalExpense,
                Math.subtractExact(totalIncome, totalExpense),
                totalCount,
                dailyRows,
                monthlyRows,
                methodRows
        );
    }

    public long totalIncomeCents() {
        return totalIncomeCents;
    }

    public long totalExpenseCents() {
        return totalExpenseCents;
    }

    public long netCents() {
        return netCents;
    }

    public int totalMovementCount() {
        return totalMovementCount;
    }

    public List<DailyRow> byDay() {
        return byDay;
    }

    public List<MonthlyRow> byMonth() {
        return byMonth;
    }

    public List<MethodRow> byMethod() {
        return byMethod;
    }
}
