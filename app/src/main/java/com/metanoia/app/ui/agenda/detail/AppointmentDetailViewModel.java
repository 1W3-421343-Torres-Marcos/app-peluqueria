package com.metanoia.app.ui.agenda.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.mapper.AppointmentMapper;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader.SlotLoadResult;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.usecase.CancelRecurringSeriesUseCase;
import com.metanoia.domain.usecase.RescheduleAppointmentUseCase;

import java.util.List;
import java.util.Optional;

/**
 * ViewModel de detalle de turno: resuelve participantes, transiciones de estado y reprogramación.
 */
public class AppointmentDetailViewModel extends AndroidViewModel {

    public static final class DetailUiState {
        public final Appointment appointment;
        public final String clientName;
        public final String professionalName;

        public DetailUiState(
                Appointment appointment,
                String clientName,
                String professionalName
        ) {
            this.appointment = appointment;
            this.clientName = clientName;
            this.professionalName = professionalName;
        }
    }

    private final AppContainer container;
    private final MutableLiveData<DetailUiState> detailState = new MutableLiveData<>();
    private final LiveData<List<ProfessionalWithDetails>> activeProfessionals;
    private final MutableLiveData<SlotLoadResult> rescheduleSlots = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeRescheduleEvent = new MutableLiveData<>();

    public AppointmentDetailViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        activeProfessionals = container.database().professionalDao().observeAllWithDetails(false);
    }

    public LiveData<AppointmentWithItems> observeRawAppointment(@NonNull String appointmentId) {
        return container.database().appointmentDao().observeByIdWithItems(appointmentId);
    }

    public LiveData<DetailUiState> observeDetailState() {
        return detailState;
    }

    public LiveData<List<ProfessionalWithDetails>> observeActiveProfessionals() {
        return activeProfessionals;
    }

    public LiveData<SlotLoadResult> observeRescheduleSlots() {
        return rescheduleSlots;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeCloseReschedule() {
        return closeRescheduleEvent;
    }

    public void resolveDetail(@Nullable AppointmentWithItems withItems) {
        if (withItems == null || withItems.appointment == null) {
            detailState.setValue(null);
            return;
        }
        Appointment appointment = AppointmentMapper.toDomain(withItems);
        container.executors().io().execute(() -> {
            Optional<Client> clientOpt = container.clientRepository()
                    .findById(appointment.clientId());
            Optional<Professional> proOpt = container.professionalRepository()
                    .findById(appointment.professionalId());
            String clientName = clientOpt.map(Client::name).orElse("");
            String proName = proOpt.map(Professional::name).orElse("");
            detailState.postValue(new DetailUiState(appointment, clientName, proName));
        });
    }

    public void changeStatus(
            @NonNull String appointmentId,
            @NonNull AppointmentStatus targetStatus,
            @Nullable String cancelReason
    ) {
        container.executors().io().execute(() -> {
            Result<Appointment> result = container.changeAppointmentStatusUseCase()
                    .execute(appointmentId, targetStatus, cancelReason);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_status_updated));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_cancel_reason_required));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_status_conflict));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void cancelRecurring(
            @NonNull String appointmentId,
            @NonNull CancelRecurringSeriesUseCase.Scope scope,
            @Nullable String cancelReason
    ) {
        container.executors().io().execute(() -> {
            Result<List<Appointment>> result = container.cancelRecurringSeriesUseCase()
                    .execute(appointmentId, scope, cancelReason);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_status_updated));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_cancel_reason_required));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_status_conflict));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void loadRescheduleSlots(
            @NonNull String professionalId,
            long dayEpoch,
            int durationMin,
            @NonNull String appointmentId
    ) {
        container.executors().io().execute(() -> rescheduleSlots.postValue(
                SlotAvailabilityLoader.load(
                        container, professionalId, dayEpoch, durationMin, appointmentId)));
    }

    public void reschedule(
            @NonNull String appointmentId,
            @NonNull String professionalId,
            long dayEpoch,
            @Nullable Integer startMin
    ) {
        if (startMin == null) {
            feedbackEvent.setValue(new Event<>(R.string.appointment_error_validation));
            return;
        }
        container.executors().io().execute(() -> {
            Result<Appointment> result = container.rescheduleAppointmentUseCase().execute(
                    new RescheduleAppointmentUseCase.Request(
                            appointmentId, professionalId, dayEpoch, startMin));
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_rescheduled));
                closeRescheduleEvent.postValue(new Event<>(true));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_conflict));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }
}
