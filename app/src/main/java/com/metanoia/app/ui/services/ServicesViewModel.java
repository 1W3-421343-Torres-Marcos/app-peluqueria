package com.metanoia.app.ui.services;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.usecase.SaveSalonServiceUseCase;

import java.util.List;

/**
 * ViewModel del catálogo de servicios con filtro activos/todos y guardado en segundo plano.
 */
public class ServicesViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Boolean> includeInactive = new MutableLiveData<>(false);
    private final LiveData<List<SalonServiceEntity>> services;
    private final LiveData<Integer> slotMinutes;
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeFormEvent = new MutableLiveData<>();

    public ServicesViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        services = Transformations.switchMap(
                includeInactive,
                include -> container.database().salonServiceDao().observeAll(Boolean.TRUE.equals(include))
        );
        slotMinutes = Transformations.map(
                container.database().settingsDao().observeSettings(),
                entity -> entity == null ? 15 : entity.slotMinutes
        );
    }

    public LiveData<List<SalonServiceEntity>> observeServices() {
        return services;
    }

    public LiveData<Integer> observeSlotMinutes() {
        return slotMinutes;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeCloseForm() {
        return closeFormEvent;
    }

    public void setIncludeInactive(boolean include) {
        if (!Boolean.valueOf(include).equals(includeInactive.getValue())) {
            includeInactive.setValue(include);
        }
    }

    public void saveService(
            String id,
            String name,
            String category,
            long priceCents,
            int durationMin,
            boolean colorService,
            boolean active,
            int sortOrder
    ) {
        container.executors().io().execute(() -> {
            SaveSalonServiceUseCase.Request request = new SaveSalonServiceUseCase.Request(
                    id, name, category, Money.ofCents(priceCents),
                    durationMin, colorService, active, sortOrder);
            Result<SalonService> result = container.saveSalonServiceUseCase().execute(request);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.service_saved));
                closeFormEvent.postValue(new Event<>(true));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.service_error_conflict));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.service_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.service_error_unexpected));
            }
        });
    }
}
