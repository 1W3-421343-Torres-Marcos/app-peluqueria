package com.metanoia.app.data.repository;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.dao.ReportsDao;
import com.metanoia.app.data.db.projection.FrequentClientProjection;
import com.metanoia.app.data.db.projection.IncomeByDayProjection;
import com.metanoia.app.data.db.projection.IncomeByMethodProjection;
import com.metanoia.app.data.db.projection.OccupiedSlotProjection;
import com.metanoia.app.data.db.projection.ProfessionalPerformanceProjection;
import com.metanoia.app.data.db.projection.ReceivableClientProjection;
import com.metanoia.app.data.db.projection.ReceivableTotalsProjection;
import com.metanoia.app.data.db.projection.TopServiceCategoryProjection;
import com.metanoia.app.data.db.projection.TopServiceProjection;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.ReportFilter;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.model.TopServicesReport;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.ReportsRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.OccupancyCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementación Room de {@link ReportsRepository} apoyada en agregaciones SQL acotadas y reglas puras.
 */
public final class ReportsRepositoryImpl implements ReportsRepository {

    private final ReportsDao reportsDao;
    private final ProfessionalRepository professionalRepository;
    private final SettingsRepository settingsRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;

    public ReportsRepositoryImpl(
            @NonNull ReportsDao reportsDao,
            @NonNull ProfessionalRepository professionalRepository,
            @NonNull SettingsRepository settingsRepository,
            @NonNull ScheduleBlockRepository scheduleBlockRepository
    ) {
        this.reportsDao = Objects.requireNonNull(reportsDao, "reportsDao");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.scheduleBlockRepository = Objects.requireNonNull(
                scheduleBlockRepository, "scheduleBlockRepository");
    }

    @Override
    public IncomeReport getIncomeReport(ReportFilter filter) {
        Objects.requireNonNull(filter, "filter");
        String proId = filter.professionalId().orElse(null);
        List<IncomeByDayProjection> dayRows = reportsDao.queryIncomeByDaySync(
                filter.fromDay(), filter.toDay(), proId);
        List<IncomeByMethodProjection> methodRows = reportsDao.queryIncomeByMethodSync(
                filter.fromDay(), filter.toDay(), proId);

        List<IncomeReport.DailyRow> domainDays = new ArrayList<>(dayRows.size());
        for (IncomeByDayProjection row : dayRows) {
            domainDays.add(new IncomeReport.DailyRow(
                    row.day, row.incomeCents, row.expenseCents, row.movementCount));
        }

        List<IncomeReport.MethodRow> domainMethods = new ArrayList<>(methodRows.size());
        for (IncomeByMethodProjection row : methodRows) {
            PaymentMethod method = parsePaymentMethod(row.method);
            domainMethods.add(new IncomeReport.MethodRow(
                    method, row.incomeCents, row.expenseCents, row.movementCount));
        }

        return IncomeReport.fromDailyAndMethodRows(domainDays, domainMethods);
    }

    @Override
    public TopServicesReport getTopServicesReport(ReportFilter filter) {
        Objects.requireNonNull(filter, "filter");
        String proId = filter.professionalId().orElse(null);
        List<TopServiceProjection> serviceRows = reportsDao.queryTopServicesSync(
                filter.fromDay(), filter.toDay(), proId);
        List<TopServiceCategoryProjection> categoryRows = reportsDao.queryTopServiceCategoriesSync(
                filter.fromDay(), filter.toDay(), proId);

        List<TopServicesReport.ServiceRow> services = new ArrayList<>(serviceRows.size());
        for (TopServiceProjection row : serviceRows) {
            services.add(new TopServicesReport.ServiceRow(
                    row.serviceId, row.serviceName, row.category,
                    row.serviceCount, row.revenueCents));
        }

        List<TopServicesReport.CategoryRow> categories = new ArrayList<>(categoryRows.size());
        for (TopServiceCategoryProjection row : categoryRows) {
            categories.add(new TopServicesReport.CategoryRow(
                    row.category, row.serviceCount, row.revenueCents));
        }
        return new TopServicesReport(services, categories);
    }

    @Override
    public FrequentClientsReport getFrequentClientsReport(ReportFilter filter, int limit) {
        Objects.requireNonNull(filter, "filter");
        int effectiveLimit = limit > 0 ? limit : DEFAULT_TOP_CLIENTS_LIMIT;
        String proId = filter.professionalId().orElse(null);
        List<FrequentClientProjection> byVisits = reportsDao.queryFrequentClientsByVisitsSync(
                filter.fromDay(), filter.toDay(), proId, effectiveLimit);
        List<FrequentClientProjection> bySpend = reportsDao.queryFrequentClientsBySpendSync(
                filter.fromDay(), filter.toDay(), proId, effectiveLimit);

        return new FrequentClientsReport(
                effectiveLimit,
                mapFrequentClients(byVisits),
                mapFrequentClients(bySpend)
        );
    }

    @Override
    public OccupancyReport getOccupancyReport(ReportFilter filter) {
        Objects.requireNonNull(filter, "filter");
        String proId = filter.professionalId().orElse(null);

        List<Professional> professionals;
        if (proId != null) {
            professionals = professionalRepository.findById(proId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            professionals = professionalRepository.findAll(false);
        }

        List<TimeRange> salonSchedule = settingsRepository.getSettings().generalSchedule();
        List<ScheduleBlock> activeBlocks = scheduleBlockRepository.findActiveOverlappingRange(
                filter.fromDay(), filter.toDay());
        List<OccupiedSlotProjection> rawBuckets = reportsDao.queryOccupiedSlotMinutesSync(
                filter.fromDay(), filter.toDay(), proId);

        List<OccupancyReport.OccupiedBucket> buckets = new ArrayList<>(rawBuckets.size());
        for (OccupiedSlotProjection row : rawBuckets) {
            buckets.add(new OccupancyReport.OccupiedBucket(
                    row.dayOfWeek, row.slotHour, row.occupiedMinutes, row.appointmentCount));
        }

        return OccupancyCalculator.calculate(
                filter.fromDay(),
                filter.toDay(),
                professionals,
                salonSchedule,
                activeBlocks,
                buckets
        );
    }

    @Override
    public ProfessionalPerformanceReport getProfessionalPerformanceReport(ReportFilter filter) {
        Objects.requireNonNull(filter, "filter");
        String proId = filter.professionalId().orElse(null);
        List<ProfessionalPerformanceProjection> rawRows =
                reportsDao.queryProfessionalPerformanceSync(filter.fromDay(), filter.toDay(), proId);

        List<ProfessionalPerformanceReport.ProfessionalRow> rows = new ArrayList<>(rawRows.size());
        for (ProfessionalPerformanceProjection row : rawRows) {
            rows.add(new ProfessionalPerformanceReport.ProfessionalRow(
                    row.professionalId,
                    row.professionalName,
                    row.completedCount,
                    row.billedCents,
                    row.noShowCount,
                    row.commissionCents
            ));
        }
        return new ProfessionalPerformanceReport(rows);
    }

    @Override
    public ReceivablesReport getReceivablesReport(ReportFilter filter) {
        Objects.requireNonNull(filter, "filter");
        String proId = filter.professionalId().orElse(null);
        List<ReceivableClientProjection> clientRows = reportsDao.queryReceivablesByClientSync(
                filter.fromDay(), filter.toDay(), proId);
        ReceivableTotalsProjection historicalTotals =
                reportsDao.queryHistoricalReceivablesTotalsSync(proId);

        List<ReceivablesReport.ClientReceivableRow> rows = new ArrayList<>(clientRows.size());
        for (ReceivableClientProjection row : clientRows) {
            rows.add(new ReceivablesReport.ClientReceivableRow(
                    row.clientId,
                    row.clientName,
                    row.appointmentCount,
                    row.balanceCents,
                    row.oldestDay
            ));
        }

        long historicalBalance = historicalTotals == null ? 0L : historicalTotals.totalBalanceCents;
        int historicalCount = historicalTotals == null ? 0 : historicalTotals.appointmentCount;
        return new ReceivablesReport(historicalBalance, historicalCount, rows);
    }

    private static List<FrequentClientsReport.ClientRow> mapFrequentClients(
            List<FrequentClientProjection> rows
    ) {
        List<FrequentClientsReport.ClientRow> mapped = new ArrayList<>(rows.size());
        for (FrequentClientProjection row : rows) {
            mapped.add(new FrequentClientsReport.ClientRow(
                    row.clientId,
                    row.clientName,
                    row.phone,
                    row.visitCount,
                    row.totalSpentCents,
                    row.lastVisitDay
            ));
        }
        return mapped;
    }

    private static PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return PaymentMethod.CASH;
        }
    }
}
