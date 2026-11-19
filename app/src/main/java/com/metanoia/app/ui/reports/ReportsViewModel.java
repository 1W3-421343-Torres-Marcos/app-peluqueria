package com.metanoia.app.ui.reports;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.ui.common.Event;
import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.LoadedPayload;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.PeriodMode;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsUiState;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.ReportFilter;
import com.metanoia.domain.model.ReportType;
import com.metanoia.domain.model.TopServicesReport;
import com.metanoia.domain.repository.ReportsRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ViewModel de Reportes: coordina filtros, consultas agregadas en segundo plano y estado de UI.
 */
public class ReportsViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final LiveData<List<ProfessionalWithDetails>> activeProfessionals;
    private final MutableLiveData<ReportsFilterState> filterState;
    private final MutableLiveData<ReportsUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> messageEvent = new MutableLiveData<>();
    private final AtomicLong requestSeq = new AtomicLong();
    @Nullable
    private ReportsFilterState loadedFilter;
    @Nullable
    private LoadedPayload loadedPayload;

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
        this.activeProfessionals = container.database().professionalDao().observeAllWithDetails(false);

        LocalDate today = LocalDate.ofEpochDay(container.clock().todayEpochDay());
        YearMonth ym = YearMonth.from(today);
        ReportsFilterState initial = new ReportsFilterState(
                ReportType.INCOME,
                PeriodMode.MONTH,
                ym.atDay(1).toEpochDay(),
                ym.atEndOfMonth().toEpochDay(),
                null,
                IncomeGranularity.DAY,
                ServiceGrouping.SERVICE,
                ClientRankingMode.VISITS,
                OccupancyGrouping.HOUR,
                false
        );
        this.filterState = new MutableLiveData<>(initial);
        applyFilter(initial, true);
    }

    public LiveData<List<ProfessionalWithDetails>> observeProfessionals() {
        return activeProfessionals;
    }

    public LiveData<ReportsFilterState> observeFilter() {
        return filterState;
    }

    public LiveData<ReportsUiState> observeUiState() {
        return uiState;
    }

    public LiveData<Event<Integer>> observeMessage() {
        return messageEvent;
    }

    @Nullable
    public ReportsFilterState currentFilter() {
        return filterState.getValue();
    }

    public void selectReportType(@NonNull ReportType type) {
        ReportsFilterState current = currentFilter();
        if (current != null && current.reportType != type) {
            applyFilter(current.withReportType(type), false);
        }
    }

    public void setPeriodMode(@NonNull PeriodMode mode) {
        ReportsFilterState current = currentFilter();
        if (current == null || current.periodMode == mode) {
            return;
        }
        LocalDate anchor = LocalDate.ofEpochDay(current.fromDay);
        long nextFrom;
        long nextTo;
        if (mode == PeriodMode.TODAY) {
            long today = container.clock().todayEpochDay();
            nextFrom = today;
            nextTo = today;
        } else if (mode == PeriodMode.WEEK) {
            long today = container.clock().todayEpochDay();
            LocalDate base = LocalDate.ofEpochDay(today);
            nextFrom = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay();
            nextTo = nextFrom + 6L;
        } else if (mode == PeriodMode.MONTH) {
            YearMonth ym = YearMonth.from(anchor);
            nextFrom = ym.atDay(1).toEpochDay();
            nextTo = ym.atEndOfMonth().toEpochDay();
        } else {
            nextFrom = current.fromDay;
            nextTo = current.toDay;
        }
        applyFilter(current.withPeriod(mode, nextFrom, nextTo), false);
    }

    public void shiftPeriod(int delta) {
        ReportsFilterState current = currentFilter();
        if (current == null || current.periodMode == PeriodMode.RANGE) {
            return;
        }
        long nextFrom;
        long nextTo;
        if (current.periodMode == PeriodMode.TODAY) {
            nextFrom = current.fromDay + delta;
            nextTo = nextFrom;
        } else if (current.periodMode == PeriodMode.WEEK) {
            nextFrom = current.fromDay + delta * 7L;
            nextTo = nextFrom + 6L;
        } else {
            YearMonth ym = YearMonth.from(LocalDate.ofEpochDay(current.fromDay)).plusMonths(delta);
            nextFrom = ym.atDay(1).toEpochDay();
            nextTo = ym.atEndOfMonth().toEpochDay();
        }
        applyFilter(current.withPeriod(current.periodMode, nextFrom, nextTo), false);
    }

    public void setCustomRange(long fromDay, long toDay) {
        ReportsFilterState current = currentFilter();
        if (current == null) {
            return;
        }
        long safeFrom = Math.min(fromDay, toDay);
        long safeTo = Math.max(fromDay, toDay);
        applyFilter(current.withPeriod(PeriodMode.RANGE, safeFrom, safeTo), false);
    }

    public void selectProfessional(@Nullable String professionalId) {
        ReportsFilterState current = currentFilter();
        if (current != null && !Objects.equals(current.professionalId, professionalId)) {
            applyFilter(current.withProfessionalId(professionalId), false);
        }
    }

    public void selectSubMode(boolean secondOption) {
        ReportsFilterState current = currentFilter();
        if (current == null) {
            return;
        }
        ReportsFilterState next = new ReportsFilterState(
                current.reportType,
                current.periodMode,
                current.fromDay,
                current.toDay,
                current.professionalId,
                current.reportType == ReportType.INCOME
                        ? (secondOption ? IncomeGranularity.MONTH : IncomeGranularity.DAY)
                        : current.incomeGranularity,
                current.reportType == ReportType.TOP_SERVICES
                        ? (secondOption ? ServiceGrouping.CATEGORY : ServiceGrouping.SERVICE)
                        : current.serviceGrouping,
                current.reportType == ReportType.FREQUENT_CLIENTS
                        ? (secondOption ? ClientRankingMode.SPEND : ClientRankingMode.VISITS)
                        : current.clientRankingMode,
                current.reportType == ReportType.OCCUPANCY
                        ? (secondOption ? OccupancyGrouping.DAY : OccupancyGrouping.HOUR)
                        : current.occupancyGrouping,
                current.includeClientPhone
        );
        applyFilter(next, false);
    }

    public void setIncludeClientPhone(boolean include) {
        ReportsFilterState current = currentFilter();
        if (current != null && current.includeClientPhone != include) {
            applyFilter(current.withIncludeClientPhone(include), false);
        }
    }

    private void applyFilter(@NonNull ReportsFilterState next, boolean forceQuery) {
        filterState.setValue(next);
        if (!forceQuery
                && loadedFilter != null
                && loadedPayload != null
                && next.sameDataQuery(loadedFilter)) {
            uiState.setValue(ReportPresentationMapper.mapToUiState(
                    getApplication(), next, loadedPayload));
            return;
        }

        long seq = requestSeq.incrementAndGet();
        container.executors().io().execute(() -> {
            try {
                LoadedPayload payload = fetchPayload(next);
                if (requestSeq.get() != seq) {
                    return;
                }
                loadedFilter = next;
                loadedPayload = payload;
                uiState.postValue(ReportPresentationMapper.mapToUiState(
                        getApplication(), next, payload));
            } catch (RuntimeException ex) {
                if (requestSeq.get() == seq) {
                    messageEvent.postValue(new Event<>(R.string.reports_error_load));
                }
            }
        });
    }

    @NonNull
    private LoadedPayload fetchPayload(@NonNull ReportsFilterState filter) {
        ReportsRepository repo = container.reportsRepository();
        ReportFilter domainFilter = filter.toDomainFilter();
        IncomeReport income = null;
        TopServicesReport topServices = null;
        FrequentClientsReport frequentClients = null;
        OccupancyReport occupancy = null;
        ProfessionalPerformanceReport performance = null;
        ReceivablesReport receivables = null;

        switch (filter.reportType) {
            case TOP_SERVICES:
                topServices = repo.getTopServicesReport(domainFilter);
                break;
            case FREQUENT_CLIENTS:
                frequentClients = repo.getFrequentClientsReport(
                        domainFilter, ReportsRepository.DEFAULT_TOP_CLIENTS_LIMIT);
                break;
            case OCCUPANCY:
                occupancy = repo.getOccupancyReport(domainFilter);
                break;
            case PROFESSIONAL_PERFORMANCE:
                performance = repo.getProfessionalPerformanceReport(domainFilter);
                break;
            case RECEIVABLES:
                receivables = repo.getReceivablesReport(domainFilter);
                break;
            case INCOME:
            default:
                income = repo.getIncomeReport(domainFilter);
                break;
        }
        return new LoadedPayload(
                income, topServices, frequentClients, occupancy, performance, receivables);
    }
}
