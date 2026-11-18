package com.metanoia.app.ui.professionals;

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
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.usecase.SaveProfessionalUseCase;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ViewModel para listado y edición de profesionales, horarios, servicios y días libres.
 */
public class ProfessionalsViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Boolean> includeInactive = new MutableLiveData<>(false);
    private final LiveData<List<ProfessionalWithDetails>> professionals;
    private final LiveData<List<SalonServiceEntity>> activeServices;
    private final MutableLiveData<Professional> loadedProfessional = new MutableLiveData<>();
    private final MutableLiveData<List<TimeRange>> defaultSalonSchedule = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> savedSuccessEvent = new MutableLiveData<>();

    public ProfessionalsViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        professionals = Transformations.switchMap(
                includeInactive,
                include -> container.database().professionalDao()
                        .observeAllWithDetails(Boolean.TRUE.equals(include))
        );
        activeServices = container.database().salonServiceDao().observeAll(false);
    }

    public LiveData<List<ProfessionalWithDetails>> observeProfessionals() {
        return professionals;
    }

    public LiveData<List<SalonServiceEntity>> observeActiveServices() {
        return activeServices;
    }

    public LiveData<Professional> observeLoadedProfessional() {
        return loadedProfessional;
    }

    public LiveData<List<TimeRange>> observeDefaultSalonSchedule() {
        return defaultSalonSchedule;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeSavedSuccess() {
        return savedSuccessEvent;
    }

    public void setIncludeInactive(boolean include) {
        if (!Boolean.valueOf(include).equals(includeInactive.getValue())) {
            includeInactive.setValue(include);
        }
    }

    public void loadForForm(@Nullable String professionalId) {
        container.executors().io().execute(() -> {
            if (professionalId != null && !professionalId.trim().isEmpty()) {
                Optional<Professional> found = container.professionalRepository()
                        .findById(professionalId.trim());
                found.ifPresent(loadedProfessional::postValue);
            } else {
                defaultSalonSchedule.postValue(
                        container.settingsRepository().getSettings().generalSchedule());
            }
        });
    }

    public void save(
            @Nullable String id,
            String name,
            String phone,
            int commissionBasisPoints,
            boolean active,
            List<TimeRange> schedules,
            Set<String> serviceIds,
            Set<Long> daysOff
    ) {
        container.executors().io().execute(() -> {
            SaveProfessionalUseCase.Request request = new SaveProfessionalUseCase.Request(
                    id, name, phone, "", commissionBasisPoints,
                    active, schedules, serviceIds, daysOff);
            Result<Professional> result = container.saveProfessionalUseCase().execute(request);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.professional_saved));
                savedSuccessEvent.postValue(new Event<>(true));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.professional_error_conflict));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.professional_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.professional_error_unexpected));
            }
        });
    }
}
