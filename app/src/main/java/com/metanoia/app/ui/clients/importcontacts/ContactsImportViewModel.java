package com.metanoia.app.ui.clients.importcontacts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.contacts.ContactSyncScheduler;
import com.metanoia.app.data.contacts.DeviceContactsReader;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.ContactImportPreview;
import com.metanoia.domain.model.ContactSyncOutcome;
import com.metanoia.domain.model.DeviceContactCandidate;
import com.metanoia.domain.usecase.SyncContactsUseCase;

import java.util.Collections;
import java.util.List;

/**
 * Coordina la explicación de permiso, vista previa e importación transaccional desde contactos.
 */
public class ContactsImportViewModel extends AndroidViewModel {

    public enum Step {
        EXPLAIN,
        LOADING,
        PREVIEW,
        IMPORTING,
        DONE,
        ERROR
    }

    public static final class UiState {
        public final Step step;
        @Nullable
        public final ContactImportPreview preview;
        @Nullable
        public final ContactSyncOutcome outcome;
        public final boolean permissionDenied;
        @StringRes
        public final int errorResId;

        UiState(
                Step step,
                @Nullable ContactImportPreview preview,
                @Nullable ContactSyncOutcome outcome,
                boolean permissionDenied,
                @StringRes int errorResId
        ) {
            this.step = step;
            this.preview = preview;
            this.outcome = outcome;
            this.permissionDenied = permissionDenied;
            this.errorResId = errorResId;
        }
    }

    private final AppContainer container;
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private List<DeviceContactCandidate> cachedCandidates = Collections.emptyList();

    public ContactsImportViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
    }

    public LiveData<UiState> observeState() {
        return state;
    }

    public void initialize() {
        if (state.getValue() != null) {
            return;
        }
        if (DeviceContactsReader.hasPermission(getApplication())) {
            loadPreview();
        } else {
            state.setValue(new UiState(Step.EXPLAIN, null, null, false, 0));
        }
    }

    public void onPermissionResult(boolean granted) {
        if (granted) {
            loadPreview();
        } else {
            state.setValue(new UiState(Step.EXPLAIN, null, null, true, 0));
        }
    }

    public void onResumeCheckPermission() {
        UiState current = state.getValue();
        if (current != null
                && current.step == Step.EXPLAIN
                && DeviceContactsReader.hasPermission(getApplication())) {
            loadPreview();
        }
    }

    public void loadPreview() {
        state.setValue(new UiState(Step.LOADING, null, null, false, 0));
        container.executors().io().execute(() -> {
            List<DeviceContactCandidate> read = container.deviceContactsReader().readAllContacts();
            Result<ContactImportPreview> result =
                    container.previewContactImportUseCase().execute(read);
            if (!result.isSuccess()) {
                state.postValue(new UiState(
                        Step.ERROR, null, null, false, R.string.contacts_import_error));
                return;
            }
            cachedCandidates = read;
            state.postValue(new UiState(Step.PREVIEW, result.getValue(), null, false, 0));
        });
    }

    public void confirmImportAll() {
        state.setValue(new UiState(Step.IMPORTING, null, null, false, 0));
        container.executors().io().execute(() -> {
            Result<ContactSyncOutcome> result = container.syncContactsUseCase().execute(
                    cachedCandidates, SyncContactsUseCase.ACTION_BULK_IMPORT);
            if (!result.isSuccess()) {
                state.postValue(new UiState(
                        Step.ERROR, null, null, false, R.string.contacts_import_error));
                return;
            }
            ContactSyncOutcome outcome = result.getValue();
            DevicePrefs prefs = container.devicePrefs();
            prefs.markInitialContactsImportDone();
            prefs.setInitialSetupDismissed(true);
            prefs.setContactSyncEnabled(true);
            prefs.recordContactSyncResult(
                    outcome.executedAtMillis(),
                    outcome.importedCount(),
                    DevicePrefs.SYNC_STATUS_OK);
            prefs.setLastContactSyncAttemptMillis(outcome.executedAtMillis());
            ContactSyncScheduler.scheduleContentUriTrigger(getApplication(), true);
            state.postValue(new UiState(Step.DONE, null, outcome, false, 0));
        });
    }
}
