package com.metanoia.app.ui.commissions;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.CommissionSettlementEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.ui.common.Event;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.rules.CommissionCalculator;
import com.metanoia.domain.rules.CommissionCalculator.AppointmentCalculation;
import com.metanoia.domain.rules.CommissionCalculator.ItemCalculation;
import com.metanoia.domain.rules.CommissionCalculator.PeriodCalculation;
import com.metanoia.domain.rules.CommissionCalculator.ReversalAdjustmentLine;
import com.metanoia.domain.usecase.SettleCommissionUseCase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * ViewModel para consulta de comisiones por profesional, período (semana, mes o rango) y detalle.
 */
public class CommissionsViewModel extends AndroidViewModel {

    public enum PeriodMode {
        WEEK,
        MONTH,
        RANGE
    }

    public enum CommissionRowKind {
        LIQUIDABLE,
        REVERSAL_ADJUSTMENT,
        PENDING_COLLECTION
    }

    public static final class CommissionLineItem {
        @NonNull
        public final String key;
        @NonNull
        public final String appointmentId;
        public final long day;
        public final int startMin;
        @NonNull
        public final String clientName;
        @NonNull
        public final CommissionRowKind kind;
        @NonNull
        public final List<String> itemSummaries;
        public final long baseCents;
        public final long commissionCents;

        public CommissionLineItem(
                @NonNull String key,
                @NonNull String appointmentId,
                long day,
                int startMin,
                @NonNull String clientName,
                @NonNull CommissionRowKind kind,
                @NonNull List<String> itemSummaries,
                long baseCents,
                long commissionCents
        ) {
            this.key = key;
            this.appointmentId = appointmentId;
            this.day = day;
            this.startMin = startMin;
            this.clientName = clientName;
            this.kind = kind;
            this.itemSummaries = itemSummaries;
            this.baseCents = baseCents;
            this.commissionCents = commissionCents;
        }
    }

    public static final class CommissionsFilter {
        @Nullable
        public final String professionalId;
        @NonNull
        public final PeriodMode mode;
        public final long fromDay;
        public final long toDay;

        public CommissionsFilter(
                @Nullable String professionalId,
                @NonNull PeriodMode mode,
                long fromDay,
                long toDay
        ) {
            this.professionalId = professionalId;
            this.mode = mode;
            this.fromDay = fromDay;
            this.toDay = toDay;
        }
    }

    public static final class CommissionsUiState {
        @NonNull
        public final CommissionsFilter filter;
        @Nullable
        public final PeriodCalculation calculation;
        @NonNull
        public final List<CommissionLineItem> rows;

        public CommissionsUiState(
                @NonNull CommissionsFilter filter,
                @Nullable PeriodCalculation calculation,
                @NonNull List<CommissionLineItem> rows
        ) {
            this.filter = filter;
            this.calculation = calculation;
            this.rows = rows;
        }
    }

    private final AppContainer container;
    private final LiveData<List<ProfessionalWithDetails>> allProfessionals;
    private final MutableLiveData<CommissionsFilter> filterState;
    private final MutableLiveData<String> selectedProId = new MutableLiveData<>();
    private final LiveData<List<CommissionSettlementEntity>> settlementHistory;
    private final MutableLiveData<CommissionsUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> messageEvent = new MutableLiveData<>();
    private final AtomicInteger computationSeq = new AtomicInteger();

    public CommissionsViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
        this.allProfessionals = container.database().professionalDao().observeAllWithDetails(true);
        LocalDate today = LocalDate.ofEpochDay(container.clock().todayEpochDay());
        long monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay();
        this.filterState = new MutableLiveData<>(
                new CommissionsFilter(null, PeriodMode.WEEK, monday, monday + 6L));
        this.settlementHistory = Transformations.switchMap(selectedProId, proId -> {
            if (proId == null || proId.trim().isEmpty()) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return container.database().commissionDao().observeSettlementsByProfessional(proId);
        });
    }

    public LiveData<List<ProfessionalWithDetails>> observeProfessionals() {
        return allProfessionals;
    }

    public LiveData<CommissionsFilter> observeFilter() {
        return filterState;
    }

    public LiveData<CommissionsUiState> observeUiState() {
        return uiState;
    }

    public LiveData<List<CommissionSettlementEntity>> observeSettlementHistory() {
        return settlementHistory;
    }

    public LiveData<Event<Integer>> observeMessage() {
        return messageEvent;
    }

    @Nullable
    public CommissionsFilter currentFilter() {
        return filterState.getValue();
    }

    public void settleCurrentPeriod(
            @NonNull PaymentMethod method,
            @NonNull Consumer<Boolean> callback
    ) {
        CommissionsFilter filter = filterState.getValue();
        if (filter == null || filter.professionalId == null) {
            callback.accept(false);
            return;
        }
        container.executors().io().execute(() -> {
            Result<SettleCommissionUseCase.Outcome> result = container.settleCommissionUseCase()
                    .execute(new SettleCommissionUseCase.Request(
                            filter.professionalId, filter.fromDay, filter.toDay, method));
            if (result.isSuccess()) {
                messageEvent.postValue(new Event<>(R.string.commissions_settle_success));
                computeAsync(filter);
                container.executors().mainThread().execute(() -> callback.accept(true));
                return;
            }
            int msgRes;
            if (result.getError() == DomainError.CONFLICT) {
                boolean cashClosed = container.database().cashDao()
                        .isDayClosedSync(container.clock().todayEpochDay());
                msgRes = cashClosed
                        ? R.string.commissions_settle_conflict_cash_closed
                        : R.string.commissions_settle_conflict_overlap;
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                msgRes = R.string.commissions_settle_error_empty;
            } else {
                msgRes = R.string.commissions_settle_error_unexpected;
            }
            messageEvent.postValue(new Event<>(msgRes));
            container.executors().mainThread().execute(() -> callback.accept(false));
        });
    }

    public void selectProfessional(@NonNull String professionalId) {
        CommissionsFilter current = filterState.getValue();
        if (current == null || Objects.equals(current.professionalId, professionalId)) {
            return;
        }
        updateFilter(new CommissionsFilter(
                professionalId, current.mode, current.fromDay, current.toDay));
    }

    public void ensureDefaultProfessional(@NonNull List<ProfessionalWithDetails> professionals) {
        CommissionsFilter current = filterState.getValue();
        if (current == null || professionals.isEmpty()) {
            return;
        }
        if (current.professionalId != null) {
            for (ProfessionalWithDetails item : professionals) {
                if (item.professional.id.equals(current.professionalId)) {
                    return;
                }
            }
        }
        String chosenId = professionals.get(0).professional.id;
        for (ProfessionalWithDetails item : professionals) {
            if (item.professional.active) {
                chosenId = item.professional.id;
                break;
            }
        }
        updateFilter(new CommissionsFilter(
                chosenId, current.mode, current.fromDay, current.toDay));
    }

    public void setPeriodMode(@NonNull PeriodMode mode) {
        CommissionsFilter current = filterState.getValue();
        if (current == null || current.mode == mode) {
            return;
        }
        LocalDate anchor = LocalDate.ofEpochDay(current.fromDay);
        long newFrom;
        long newTo;
        if (mode == PeriodMode.WEEK) {
            newFrom = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay();
            newTo = newFrom + 6L;
        } else if (mode == PeriodMode.MONTH) {
            YearMonth ym = YearMonth.from(anchor);
            newFrom = ym.atDay(1).toEpochDay();
            newTo = ym.atEndOfMonth().toEpochDay();
        } else {
            newFrom = current.fromDay;
            newTo = current.toDay;
        }
        updateFilter(new CommissionsFilter(current.professionalId, mode, newFrom, newTo));
    }

    public void shiftPeriod(int direction) {
        CommissionsFilter current = filterState.getValue();
        if (current == null || current.mode == PeriodMode.RANGE) {
            return;
        }
        long newFrom;
        long newTo;
        if (current.mode == PeriodMode.WEEK) {
            newFrom = current.fromDay + direction * 7L;
            newTo = newFrom + 6L;
        } else {
            YearMonth shifted = YearMonth.from(LocalDate.ofEpochDay(current.fromDay))
                    .plusMonths(direction);
            newFrom = shifted.atDay(1).toEpochDay();
            newTo = shifted.atEndOfMonth().toEpochDay();
        }
        updateFilter(new CommissionsFilter(
                current.professionalId, current.mode, newFrom, newTo));
    }

    public void setCustomRange(long fromDay, long toDay) {
        CommissionsFilter current = filterState.getValue();
        if (current == null) {
            return;
        }
        long safeFrom = Math.min(fromDay, toDay);
        long safeTo = Math.max(fromDay, toDay);
        updateFilter(new CommissionsFilter(
                current.professionalId, PeriodMode.RANGE, safeFrom, safeTo));
    }

    public void refresh() {
        CommissionsFilter current = filterState.getValue();
        if (current != null) {
            computeAsync(current);
        }
    }

    private void updateFilter(@NonNull CommissionsFilter next) {
        if (!Objects.equals(selectedProId.getValue(), next.professionalId)) {
            selectedProId.setValue(next.professionalId);
        }
        filterState.setValue(next);
        computeAsync(next);
    }

    private void computeAsync(@NonNull CommissionsFilter filter) {
        if (filter.professionalId == null) {
            uiState.setValue(new CommissionsUiState(filter, null, Collections.emptyList()));
            return;
        }
        int seq = computationSeq.incrementAndGet();
        container.executors().io().execute(() -> {
            List<CommissionSettlement> existingSettlements = container.commissionRepository()
                    .findSettlementsByProfessional(filter.professionalId);
            List<Appointment> candidates = SettleCommissionUseCase.loadCandidateAppointments(
                    filter.professionalId,
                    filter.fromDay,
                    filter.toDay,
                    existingSettlements,
                    container.appointmentRepository(),
                    container.commissionRepository()
            );

            PeriodCalculation calc = CommissionCalculator.calculatePeriod(
                    filter.professionalId,
                    filter.fromDay,
                    filter.toDay,
                    candidates,
                    existingSettlements
            );

            Set<String> clientIds = new LinkedHashSet<>();
            for (AppointmentCalculation ac : calc.liquidableAppointments()) {
                clientIds.add(ac.appointment().clientId());
            }
            for (ReversalAdjustmentLine rev : calc.reversalAdjustments()) {
                clientIds.add(rev.appointment().clientId());
            }
            for (AppointmentCalculation pc : calc.pendingCollectionAppointments()) {
                clientIds.add(pc.appointment().clientId());
            }

            Map<String, String> clientNames = new HashMap<>();
            if (!clientIds.isEmpty()) {
                List<ClientRowProjection> summaries = container.database().clientDao()
                        .findSummariesByIdsSync(new ArrayList<>(clientIds));
                for (ClientRowProjection projection : summaries) {
                    clientNames.put(projection.id, projection.name);
                }
            }

            List<CommissionLineItem> rows = buildRows(getApplication(), calc, clientNames);
            if (computationSeq.get() == seq) {
                uiState.postValue(new CommissionsUiState(filter, calc, rows));
            }
        });
    }

    private static List<CommissionLineItem> buildRows(
            @NonNull Context context,
            @NonNull PeriodCalculation calc,
            @NonNull Map<String, String> clientNames
    ) {
        List<CommissionLineItem> rows = new ArrayList<>();
        for (AppointmentCalculation ac : calc.liquidableAppointments()) {
            Appointment apt = ac.appointment();
            rows.add(new CommissionLineItem(
                    "liq-" + apt.id(),
                    apt.id(),
                    apt.day(),
                    apt.startMin(),
                    clientNames.getOrDefault(apt.clientId(), ""),
                    CommissionRowKind.LIQUIDABLE,
                    formatItemSummaries(context, ac.items()),
                    ac.adjustedBase().cents(),
                    ac.commission().cents()
            ));
        }
        for (ReversalAdjustmentLine rev : calc.reversalAdjustments()) {
            Appointment apt = rev.appointment();
            rows.add(new CommissionLineItem(
                    "rev-" + apt.id(),
                    apt.id(),
                    apt.day(),
                    apt.startMin(),
                    clientNames.getOrDefault(apt.clientId(), ""),
                    CommissionRowKind.REVERSAL_ADJUSTMENT,
                    Collections.singletonList(context.getString(R.string.commissions_reversal_badge)),
                    rev.baseCents(),
                    rev.commissionCents()
            ));
        }
        for (AppointmentCalculation pc : calc.pendingCollectionAppointments()) {
            Appointment apt = pc.appointment();
            rows.add(new CommissionLineItem(
                    "pen-" + apt.id(),
                    apt.id(),
                    apt.day(),
                    apt.startMin(),
                    clientNames.getOrDefault(apt.clientId(), ""),
                    CommissionRowKind.PENDING_COLLECTION,
                    formatItemSummaries(context, pc.items()),
                    pc.adjustedBase().cents(),
                    pc.commission().cents()
            ));
        }
        return rows;
    }

    private static List<String> formatItemSummaries(
            @NonNull Context context,
            @NonNull List<ItemCalculation> items
    ) {
        List<String> lines = new ArrayList<>(items.size());
        for (ItemCalculation item : items) {
            lines.add(context.getString(
                    R.string.commissions_item_detail_format,
                    item.serviceName(),
                    Formats.formatMoney(item.adjustedBase()),
                    Formats.formatPercent(item.commissionBasisPoints()),
                    Formats.formatMoney(item.commission())
            ));
        }
        return lines;
    }
}
