package com.metanoia.app.ui.cash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.dao.CashDao;
import com.metanoia.app.data.db.entity.CashClosingEntity;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.usecase.CloseCashDayUseCase;
import com.metanoia.domain.usecase.RecordCashMovementUseCase.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * ViewModel principal de la pestaña Caja: movimientos diarios, totales por medio,
 * cobros pendientes, cierre diario y alertas de días anteriores sin cerrar.
 */
public class CashViewModel extends AndroidViewModel {

    public static final class CashMovementRow {
        @NonNull
        public final CashMovementEntity entity;
        public final long signedAmountCents;
        public final boolean reversed;
        public final boolean canReverse;

        CashMovementRow(
                @NonNull CashMovementEntity entity,
                long signedAmountCents,
                boolean reversed,
                boolean canReverse) {
            this.entity = entity;
            this.signedAmountCents = signedAmountCents;
            this.reversed = reversed;
            this.canReverse = canReverse;
        }
    }

    public static final class CashDayState {
        public final long dayEpoch;
        public final boolean dayClosed;
        public final boolean todayClosed;
        public final long cashCents;
        public final long transferCents;
        public final long cardCents;
        public final long mercadoPagoCents;
        public final long netTotalCents;
        @NonNull
        public final List<CashMovementRow> rows;

        CashDayState(
                long dayEpoch,
                boolean dayClosed,
                boolean todayClosed,
                long cashCents,
                long transferCents,
                long cardCents,
                long mercadoPagoCents,
                long netTotalCents,
                @NonNull List<CashMovementRow> rows) {
            this.dayEpoch = dayEpoch;
            this.dayClosed = dayClosed;
            this.todayClosed = todayClosed;
            this.cashCents = cashCents;
            this.transferCents = transferCents;
            this.cardCents = cardCents;
            this.mercadoPagoCents = mercadoPagoCents;
            this.netTotalCents = netTotalCents;
            this.rows = rows;
        }
    }

    private final AppContainer container;
    private final MutableLiveData<Long> selectedDay;
    private final LiveData<Integer> receivablesCount;
    private final LiveData<List<CashMovementEntity>> rawMovements;
    private final LiveData<List<String>> sameDayReversedIds;
    private final LiveData<CashClosingEntity> closingForSelectedDay;
    private final MutableLiveData<Long> latestUnclosedPreviousDay = new MutableLiveData<>();
    private final MediatorLiveData<CashDayState> dayState = new MediatorLiveData<>();
    private final MutableLiveData<Event<Integer>> messageEvent = new MutableLiveData<>();

    public CashViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
        CashDao cashDao = container.database().cashDao();
        this.selectedDay = new MutableLiveData<>(container.clock().todayEpochDay());
        this.receivablesCount = container.database().appointmentDao().observeReceivablesCount();
        this.rawMovements = Transformations.switchMap(selectedDay, cashDao::observeByDay);
        this.sameDayReversedIds = Transformations.switchMap(
                selectedDay, cashDao::observeReversedIdsOnDay);
        this.closingForSelectedDay = Transformations.switchMap(
                selectedDay, cashDao::observeClosingByDay);

        dayState.addSource(rawMovements, list -> recomputeDayState(list));
        dayState.addSource(sameDayReversedIds, ignored -> recomputeDayState(rawMovements.getValue()));
        dayState.addSource(closingForSelectedDay, ignored -> recomputeDayState(rawMovements.getValue()));
    }

    public long todayEpochDay() {
        return container.clock().todayEpochDay();
    }

    public LiveData<Long> getSelectedDay() {
        return selectedDay;
    }

    public LiveData<Integer> getReceivablesCount() {
        return receivablesCount;
    }

    public LiveData<CashClosingEntity> getClosingForSelectedDay() {
        return closingForSelectedDay;
    }

    public LiveData<Long> getLatestUnclosedPreviousDay() {
        return latestUnclosedPreviousDay;
    }

    public LiveData<CashDayState> getDayState() {
        return dayState;
    }

    public LiveData<Event<Integer>> getMessageEvent() {
        return messageEvent;
    }

    public void previousDay() {
        Long current = selectedDay.getValue();
        long base = current == null ? todayEpochDay() : current;
        selectedDay.setValue(base - 1L);
    }

    public void nextDay() {
        Long current = selectedDay.getValue();
        long base = current == null ? todayEpochDay() : current;
        selectedDay.setValue(base + 1L);
    }

    public void selectToday() {
        selectedDay.setValue(todayEpochDay());
    }

    public void setSelectedDay(long epochDay) {
        selectedDay.setValue(epochDay);
    }

    public void recordMovement(
            @NonNull CashMovementType type,
            @NonNull PaymentMethod method,
            @NonNull Money amount,
            @NonNull String concept,
            @NonNull Consumer<Boolean> callback) {
        container.executors().io().execute(() -> {
            Result<CashMovement> result = container.recordCashMovementUseCase()
                    .execute(new Request(type, method, amount, concept));
            if (result.isSuccess()) {
                selectedDay.postValue(container.clock().todayEpochDay());
                messageEvent.postValue(new Event<>(R.string.cash_form_saved));
                container.executors().mainThread().execute(() -> callback.accept(true));
                return;
            }
            int msgRes;
            if (result.getError() == DomainError.CONFLICT) {
                msgRes = R.string.cash_form_closed_conflict;
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                msgRes = R.string.cash_form_validation_error;
            } else {
                msgRes = R.string.cash_form_unexpected_error;
            }
            messageEvent.postValue(new Event<>(msgRes));
            container.executors().mainThread().execute(() -> callback.accept(false));
        });
    }

    public void reverseMovement(@NonNull String movementId, @NonNull String reason) {
        container.executors().io().execute(() -> {
            Result<CashMovement> result = container.reverseMovementUseCase()
                    .execute(movementId, reason);
            if (result.isSuccess()) {
                messageEvent.postValue(new Event<>(R.string.cash_reverse_success));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                messageEvent.postValue(new Event<>(R.string.cash_reverse_reason_required));
            } else {
                messageEvent.postValue(new Event<>(R.string.cash_reverse_conflict));
            }
        });
    }

    public void closeDay(
            long dayEpoch,
            @NonNull Map<PaymentMethod, Money> countedByMethod,
            @NonNull String notes,
            @NonNull Consumer<Boolean> callback) {
        container.executors().io().execute(() -> {
            Result<CashClosing> result = container.closeCashDayUseCase().execute(
                    new CloseCashDayUseCase.Request(dayEpoch, countedByMethod, notes));
            if (result.isSuccess()) {
                messageEvent.postValue(new Event<>(R.string.cash_closing_success));
                container.executors().mainThread().execute(() -> callback.accept(true));
                return;
            }
            int msgRes;
            if (result.getError() == DomainError.CONFLICT) {
                msgRes = R.string.cash_closing_conflict;
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                msgRes = R.string.cash_closing_validation_error;
            } else {
                msgRes = R.string.cash_closing_unexpected_error;
            }
            messageEvent.postValue(new Event<>(msgRes));
            container.executors().mainThread().execute(() -> callback.accept(false));
        });
    }

    private void recomputeDayState(List<CashMovementEntity> list) {
        List<CashMovementEntity> snapshot = list == null
                ? Collections.emptyList()
                : new ArrayList<>(list);
        Long targetDayObj = selectedDay.getValue();
        long targetDay = targetDayObj == null ? todayEpochDay() : targetDayObj;
        container.executors().io().execute(() -> {
            CashDao cashDao = container.database().cashDao();
            long today = todayEpochDay();
            boolean dayClosed = cashDao.isDayClosedSync(targetDay);
            boolean todayClosed = cashDao.isDayClosedSync(today);
            Long unclosedPrev = cashDao.findLatestUnclosedDayWithMovementsBeforeSync(today);
            latestUnclosedPreviousDay.postValue(unclosedPrev);

            Map<String, CashMovementType> knownTypes = new HashMap<>();
            for (CashMovementEntity e : snapshot) {
                knownTypes.put(e.id, parseType(e.type));
            }
            Set<String> reversedIds = new HashSet<>();
            for (CashMovementEntity e : snapshot) {
                if (e.reversesMovementId != null) {
                    reversedIds.add(e.reversesMovementId);
                }
                if (cashDao.existsReversalForMovementSync(e.id)) {
                    reversedIds.add(e.id);
                }
            }

            EnumMap<PaymentMethod, Long> totals = new EnumMap<>(PaymentMethod.class);
            for (PaymentMethod method : PaymentMethod.values()) {
                totals.put(method, 0L);
            }
            long net = 0L;
            List<CashMovementRow> rows = new ArrayList<>(snapshot.size());
            for (CashMovementEntity e : snapshot) {
                CashMovementType type = knownTypes.get(e.id);
                long signed = computeSignedCents(e, type, knownTypes, cashDao);
                PaymentMethod method = parseMethod(e.method);
                if (method != null) {
                    totals.put(method, totals.get(method) + signed);
                }
                net += signed;
                boolean isReversed = reversedIds.contains(e.id);
                boolean canReverse = !dayClosed
                        && !todayClosed
                        && !isReversed
                        && type != CashMovementType.REVERSAL;
                rows.add(new CashMovementRow(e, signed, isReversed, canReverse));
            }

            Long latestSelected = selectedDay.getValue();
            if (latestSelected != null && latestSelected != targetDay) {
                return;
            }
            dayState.postValue(new CashDayState(
                    targetDay,
                    dayClosed,
                    todayClosed,
                    totals.get(PaymentMethod.CASH),
                    totals.get(PaymentMethod.TRANSFER),
                    totals.get(PaymentMethod.CARD),
                    totals.get(PaymentMethod.MERCADO_PAGO),
                    net,
                    rows));
        });
    }

    private static long computeSignedCents(
            CashMovementEntity entity,
            CashMovementType type,
            Map<String, CashMovementType> knownTypes,
            CashDao cashDao) {
        if (type == null || type.isStandardIncome()) {
            return entity.amountCents;
        }
        if (type.isStandardOutflow()) {
            return -entity.amountCents;
        }
        if (entity.reversesMovementId != null) {
            CashMovementType originalType = knownTypes.get(entity.reversesMovementId);
            if (originalType == null) {
                CashMovementEntity originalEntity = cashDao.findByIdSync(entity.reversesMovementId);
                if (originalEntity != null) {
                    originalType = parseType(originalEntity.type);
                }
            }
            if (originalType != null && originalType.isStandardOutflow()) {
                return entity.amountCents;
            }
        }
        return -entity.amountCents;
    }

    private static CashMovementType parseType(String raw) {
        try {
            return CashMovementType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return CashMovementType.OTHER_INCOME;
        }
    }

    private static PaymentMethod parseMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
