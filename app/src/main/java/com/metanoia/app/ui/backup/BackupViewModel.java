package com.metanoia.app.ui.backup;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.backup.BackupFolderAccess;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderStatus;
import com.metanoia.app.data.backup.BackupManager.BackupCreated;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupTrigger;

/**
 * ViewModel para configuración de carpeta SAF, parámetros automáticos y estado de backups.
 */
public class BackupViewModel extends AndroidViewModel {

    public static final class BackupUiState {
        public final FolderStatus folderStatus;
        public final long lastBackupSuccessMillis;
        public final String lastBackupFileName;
        public final String lastBackupTrigger;
        public final String lastBackupError;
        public final int autoFrequencyHours;
        public final int retentionDays;
        public final int onBackgroundHours;
        public final int overdueWarningDays;
        public final boolean busy;

        public BackupUiState(
                @NonNull FolderStatus folderStatus,
                long lastBackupSuccessMillis,
                @NonNull String lastBackupFileName,
                @NonNull String lastBackupTrigger,
                @NonNull String lastBackupError,
                int autoFrequencyHours,
                int retentionDays,
                int onBackgroundHours,
                int overdueWarningDays,
                boolean busy
        ) {
            this.folderStatus = folderStatus;
            this.lastBackupSuccessMillis = lastBackupSuccessMillis;
            this.lastBackupFileName = lastBackupFileName;
            this.lastBackupTrigger = lastBackupTrigger;
            this.lastBackupError = lastBackupError;
            this.autoFrequencyHours = autoFrequencyHours;
            this.retentionDays = retentionDays;
            this.onBackgroundHours = onBackgroundHours;
            this.overdueWarningDays = overdueWarningDays;
            this.busy = busy;
        }
    }

    protected final AppContainer container;
    private final MutableLiveData<BackupUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private boolean busy;

    public BackupViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        refresh();
    }

    public LiveData<BackupUiState> observeUiState() {
        return uiState;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public void refresh() {
        DevicePrefs prefs = container.devicePrefs();
        FolderStatus status = BackupFolderAccess.inspect(getApplication(), prefs);
        uiState.postValue(new BackupUiState(
                status,
                prefs.getLastBackupSuccessMillis(),
                prefs.getLastBackupFileName(),
                prefs.getLastBackupTrigger(),
                prefs.getLastBackupError(),
                prefs.getAutoFrequencyHours(),
                prefs.getRetentionDays(),
                prefs.getOnBackgroundThresholdHours(),
                prefs.getOverdueWarningDays(),
                busy
        ));
    }

    public void onFolderSelected(@NonNull Uri treeUri) {
        boolean persisted = BackupFolderAccess.persistTreePermission(
                getApplication(), treeUri, container.devicePrefs());
        feedbackEvent.setValue(new Event<>(persisted
                ? R.string.backup_folder_saved
                : R.string.backup_folder_permission_failed));
        refresh();
    }

    public void saveSettings(
            int frequencyHours,
            int retentionDays,
            int onBackgroundHours,
            int overdueDays
    ) {
        if (retentionDays < 1 || retentionDays > 60
                || onBackgroundHours < 1 || onBackgroundHours > 72
                || overdueDays < 1 || overdueDays > 30) {
            feedbackEvent.setValue(new Event<>(R.string.backup_settings_error_validation));
            return;
        }
        DevicePrefs prefs = container.devicePrefs();
        prefs.setAutoFrequencyHours(frequencyHours);
        prefs.setRetentionDays(retentionDays);
        prefs.setOnBackgroundThresholdHours(onBackgroundHours);
        prefs.setOverdueWarningDays(overdueDays);
        feedbackEvent.setValue(new Event<>(R.string.backup_settings_saved));
        refresh();
    }

    public void runManualBackup() {
        if (busy) {
            return;
        }
        setBusy(true);
        postFeedback(R.string.backup_now_running);
        container.executors().io().execute(() -> {
            Result<BackupCreated> result = container.backupManager()
                    .createBackupToConfiguredFolder(BackupTrigger.MANUAL);
            if (result.isSuccess()) {
                postFeedback(R.string.backup_now_success);
            } else if (result.getError() == DomainError.CONFLICT) {
                postFeedback(R.string.backup_now_folder_required);
            } else {
                postFeedback(R.string.backup_now_failed);
            }
            setBusy(false);
        });
    }

    protected void setBusy(boolean busy) {
        this.busy = busy;
        refresh();
    }

    protected void postFeedback(int stringResId) {
        feedbackEvent.postValue(new Event<>(stringResId));
    }
}
