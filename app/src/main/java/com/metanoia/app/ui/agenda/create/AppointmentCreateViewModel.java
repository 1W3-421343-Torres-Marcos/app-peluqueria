package com.metanoia.app.ui.agenda.create;

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
import com.metanoia.app.data.db.dao.ClientSearchFilter;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader.SlotLoadResult;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.usecase.CreateAppointmentUseCase;
import com.metanoia.domain.usecase.GenerateRecurringAppointmentsUseCase;
import com.metanoia.domain.usecase.GenerateRecurringAppointmentsUseCase.RecurringResult;
import com.metanoia.domain.usecase.UpdateAppointmentUseCase;

import java.util.List;
import java.util.Optional;

/**
 * ViewModel para alta rápida y edición de turnos respetando disponibilidad y snapshots.
 */
public class AppointmentCreateViewModel extends AndroidViewModel {

    private static final int CLIENT_PICKER_LIMIT = 12;

    private final AppContainer container;
    private final MutableLiveData<String> clientQuery = new MutableLiveData<>("");
    private final LiveData<List<ClientRowProjection>> clientResults;
    private final LiveData<List<SalonServiceEntity>> activeServices;
    private final LiveData<List<ProfessionalWithDetails>> activeProfessionals;
    private final LiveData<Integer> slotMinutes;
    private final MutableLiveData<SlotLoadResult> slotOptions = new MutableLiveData<>();
    private final MutableLiveData<Appointment> editingAppointment = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> savedSuccessEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<RecurringResult>> recurringResultEvent =
            new MutableLiveData<>();

    public AppointmentCreateViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        clientResults = Transformations.switchMap(clientQuery, q -> {
            ClientSearchFilter filter = ClientSearchFilter.fromRawQuery(q, false);
            return container.database().clientDao().observeTopActiveSummaries(
                    filter.query,
                    filter.prefixPattern,
                    filter.wordPrefixPattern,
                    filter.phoneContainsPattern,
                    CLIENT_PICKER_LIMIT
            );
        });
        activeServices = container.database().salonServiceDao().observeAll(false);
        activeProfessionals = container.database().professionalDao().observeAllWithDetails(false);
        slotMinutes = Transformations.map(
                container.database().settingsDao().observeSettings(),
                entity -> entity == null ? 15 : entity.slotMinutes
        );
    }

    public LiveData<List<ClientRowProjection>> observeClientResults() {
        return clientResults;
    }

    public LiveData<List<SalonServiceEntity>> observeActiveServices() {
        return activeServices;
    }

    public LiveData<List<ProfessionalWithDetails>> observeActiveProfessionals() {
        return activeProfessionals;
    }

    public LiveData<Integer> observeSlotMinutes() {
        return slotMinutes;
    }

    public LiveData<SlotLoadResult> observeSlotOptions() {
        return slotOptions;
    }

    public LiveData<Appointment> observeEditingAppointment() {
        return editingAppointment;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeSavedSuccess() {
        return savedSuccessEvent;
    }

    public LiveData<Event<RecurringResult>> observeRecurringResult() {
        return recurringResultEvent;
    }

    public long todayEpochDay() {
        return container.clock().todayEpochDay();
    }

    public void setClientQuery(@Nullable String query) {
        String safe = query == null ? "" : query;
        if (!safe.equals(clientQuery.getValue())) {
            clientQuery.setValue(safe);
        }
    }

    public void loadForEdit(@NonNull String appointmentId) {
        container.executors().io().execute(() -> {
            Optional<Appointment> found = container.appointmentRepository().findById(appointmentId);
            found.ifPresent(editingAppointment::postValue);
        });
    }

    public void reloadSlots(@Nullable String professionalId, long dayEpoch, int durationMin) {
        if (professionalId == null || durationMin <= 0) {
            slotOptions.setValue(null);
            return;
        }
        container.executors().io().execute(() -> slotOptions.postValue(
                SlotAvailabilityLoader.load(container, professionalId, dayEpoch, durationMin, null)
        ));
    }

    public void createAppointment(
            @Nullable String clientId,
            @Nullable String professionalId,
            long dayEpoch,
            @Nullable Integer startMin,
            @NonNull List<String> serviceIds,
            @NonNull String notes,
            boolean confirmed
    ) {
        if (clientId == null || professionalId == null || startMin == null || serviceIds.isEmpty()) {
            feedbackEvent.setValue(new Event<>(R.string.appointment_error_validation));
            return;
        }
        container.executors().io().execute(() -> {
            Result<Appointment> result = container.createAppointmentUseCase().execute(
                    new CreateAppointmentUseCase.Request(
                            clientId,
                            professionalId,
                            dayEpoch,
                            startMin,
                            serviceIds,
                            notes,
                            confirmed
                    )
            );
            handleSaveResult(result);
        });
    }

    public void createRecurringAppointment(
            @Nullable String clientId,
            @Nullable String professionalId,
            long startDayEpoch,
            @Nullable Integer startMin,
            @NonNull List<String> serviceIds,
            @NonNull String notes,
            boolean confirmed,
            int intervalWeeks,
            int maxOccurrences
    ) {
        if (clientId == null || professionalId == null || startMin == null
                || serviceIds.isEmpty() || intervalWeeks < 1 || maxOccurrences < 1) {
            feedbackEvent.setValue(new Event<>(R.string.appointment_error_validation));
            return;
        }
        container.executors().io().execute(() -> {
            Result<RecurringResult> result = container.generateRecurringAppointmentsUseCase()
                    .execute(new GenerateRecurringAppointmentsUseCase.Request(
                            clientId,
                            professionalId,
                            startDayEpoch,
                            startMin,
                            serviceIds,
                            notes,
                            confirmed,
                            intervalWeeks,
                            null,
                            maxOccurrences
                    ));
            if (result.isSuccess()) {
                RecurringResult value = result.getValue();
                if (value.conflicts().isEmpty()) {
                    feedbackEvent.postValue(new Event<>(R.string.appointment_recurring_saved));
                    savedSuccessEvent.postValue(new Event<>(true));
                } else {
                    recurringResultEvent.postValue(new Event<>(value));
                }
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_conflict));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void updateAppointment(
            @NonNull String appointmentId,
            @NonNull List<String> serviceIds,
            @NonNull String notes
    ) {
        if (serviceIds.isEmpty()) {
            feedbackEvent.setValue(new Event<>(R.string.appointment_error_validation));
            return;
        }
        container.executors().io().execute(() -> {
            Result<Appointment> result = container.updateAppointmentUseCase().execute(
                    new UpdateAppointmentUseCase.Request(appointmentId, serviceIds, notes));
            handleSaveResult(result);
        });
    }

    private void handleSaveResult(Result<Appointment> result) {
        if (result.isSuccess()) {
            feedbackEvent.postValue(new Event<>(R.string.appointment_saved));
            savedSuccessEvent.postValue(new Event<>(true));
        } else if (result.getError() == DomainError.CONFLICT) {
            feedbackEvent.postValue(new Event<>(R.string.appointment_error_conflict));
        } else if (result.getError() == DomainError.VALIDATION_ERROR) {
            feedbackEvent.postValue(new Event<>(R.string.appointment_error_validation));
        } else {
            feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
        }
    }
}
