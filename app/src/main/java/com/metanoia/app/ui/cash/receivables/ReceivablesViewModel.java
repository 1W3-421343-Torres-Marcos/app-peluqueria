package com.metanoia.app.ui.cash.receivables;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;

import java.util.List;

/**
 * ViewModel para el listado reactivo de turnos completados con saldo pendiente.
 */
public class ReceivablesViewModel extends AndroidViewModel {

    private final LiveData<List<AppointmentCardProjection>> receivables;

    public ReceivablesViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = ((MetanoiaApp) application).getContainer();
        receivables = container.database().appointmentDao().observeReceivableCards();
    }

    public LiveData<List<AppointmentCardProjection>> getReceivables() {
        return receivables;
    }
}
