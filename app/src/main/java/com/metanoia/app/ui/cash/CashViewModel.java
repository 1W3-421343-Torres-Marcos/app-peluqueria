package com.metanoia.app.ui.cash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;

/**
 * ViewModel principal de la pestaña Caja.
 */
public class CashViewModel extends AndroidViewModel {

    private final LiveData<Integer> receivablesCount;

    public CashViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = ((MetanoiaApp) application).getContainer();
        receivablesCount = container.database().appointmentDao().observeReceivablesCount();
    }

    public LiveData<Integer> getReceivablesCount() {
        return receivablesCount;
    }
}
