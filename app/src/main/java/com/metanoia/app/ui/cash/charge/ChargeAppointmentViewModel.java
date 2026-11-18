package com.metanoia.app.ui.cash.charge;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.rules.AppointmentPaymentCalculator;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;
import com.metanoia.domain.usecase.ChargeAppointmentUseCase;
import com.metanoia.domain.usecase.CompleteAndChargeUseCase;

import java.util.List;
import java.util.Optional;

/**
 * ViewModel para el diálogo de cobro de turnos (parcial, total, dividido o con ajuste).
 */
public class ChargeAppointmentViewModel extends AndroidViewModel {

    public static final class ChargeUiSummary {
        public final Appointment appointment;
        public final String clientName;
        public final AppointmentBalance balance;

        public ChargeUiSummary(
                @NonNull Appointment appointment,
                @NonNull String clientName,
                @NonNull AppointmentBalance balance
        ) {
            this.appointment = appointment;
            this.clientName = clientName;
            this.balance = balance;
        }
    }

    private final AppContainer container;
    private final MutableLiveData<ChargeUiSummary> summaryState = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeSheetEvent = new MutableLiveData<>();
    private boolean busy;

    public ChargeAppointmentViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
    }

    public LiveData<ChargeUiSummary> observeSummary() {
        return summaryState;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeCloseSheet() {
        return closeSheetEvent;
    }

    public void loadAppointment(@NonNull String appointmentId) {
        container.executors().io().execute(() -> {
            Optional<Appointment> found = container.appointmentRepository().findById(appointmentId);
            if (!found.isPresent()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_detail_not_found));
                closeSheetEvent.postValue(new Event<>(true));
                return;
            }
            Appointment apt = found.get();
            String clientName = container.clientRepository().findById(apt.clientId())
                    .map(Client::name)
                    .orElse("");
            List<CashMovement> movements = container.cashRepository()
                    .findMovementsByAppointmentId(apt.id());
            AppointmentBalance balance = AppointmentPaymentCalculator.balanceOf(apt, movements);
            summaryState.postValue(new ChargeUiSummary(apt, clientName, balance));
        });
    }

    public void submitCharge(
            @NonNull String appointmentId,
            @Nullable Money requestedAdjustment,
            @Nullable String adjustmentReason,
            @NonNull List<PaymentSplit> splits
    ) {
        if (busy) {
            return;
        }
        busy = true;
        container.executors().io().execute(() -> {
            Result<ChargeAppointmentUseCase.ChargeResult> result = container
                    .chargeAppointmentUseCase()
                    .execute(new ChargeAppointmentUseCase.Request(
                            appointmentId, requestedAdjustment, adjustmentReason, splits));
            busy = false;
            handleResult(result.isSuccess(), result.getError());
        });
    }

    public void submitCompleteAndCharge(
            @NonNull String appointmentId,
            @Nullable Money requestedAdjustment,
            @Nullable String adjustmentReason,
            @NonNull List<PaymentSplit> splits
    ) {
        if (busy) {
            return;
        }
        busy = true;
        container.executors().io().execute(() -> {
            Result<ChargeAppointmentUseCase.ChargeResult> result = container
                    .completeAndChargeUseCase()
                    .execute(new CompleteAndChargeUseCase.Request(
                            appointmentId, requestedAdjustment, adjustmentReason, splits));
            busy = false;
            handleResult(result.isSuccess(), result.getError());
        });
    }

    protected void handleResult(boolean success, @Nullable DomainError error) {
        if (success) {
            feedbackEvent.postValue(new Event<>(R.string.charge_success));
            closeSheetEvent.postValue(new Event<>(true));
        } else if (error == DomainError.CONFLICT) {
            feedbackEvent.postValue(new Event<>(R.string.charge_error_conflict));
        } else if (error == DomainError.VALIDATION_ERROR) {
            feedbackEvent.postValue(new Event<>(R.string.charge_error_validation));
        } else {
            feedbackEvent.postValue(new Event<>(R.string.charge_error_unexpected));
        }
    }
}
