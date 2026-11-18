package com.metanoia.app.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.dao.SettingsDao;
import com.metanoia.app.data.db.entity.SalonHoursEntity;
import com.metanoia.app.data.db.entity.SettingsEntity;
import com.metanoia.app.data.mapper.SettingsMapper;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Settings;

import java.util.List;

/**
 * ViewModel para lectura reactiva y guardado validado de la configuración del salón.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MediatorLiveData<Settings> settingsLiveData = new MediatorLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();

    private SettingsEntity latestEntity;
    private List<SalonHoursEntity> latestHours;
    private boolean initialized;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        SettingsDao dao = container.database().settingsDao();

        settingsLiveData.addSource(dao.observeSettings(), entity -> {
            latestEntity = entity;
            if (entity == null && !initialized) {
                initialized = true;
                container.executors().io().execute(() -> container.getSettingsUseCase().execute());
                return;
            }
            emitIfReady();
        });

        settingsLiveData.addSource(dao.observeHours(), hours -> {
            latestHours = hours;
            emitIfReady();
        });
    }

    public LiveData<Settings> observeSettings() {
        return settingsLiveData;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public void save(Settings candidate) {
        container.executors().io().execute(() -> {
            Result<Settings> result = container.updateSettingsUseCase().execute(candidate);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.settings_saved));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.settings_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.settings_error_unexpected));
            }
        });
    }

    private void emitIfReady() {
        if (latestEntity != null && latestHours != null) {
            settingsLiveData.setValue(SettingsMapper.toDomain(latestEntity, latestHours));
        }
    }
}
