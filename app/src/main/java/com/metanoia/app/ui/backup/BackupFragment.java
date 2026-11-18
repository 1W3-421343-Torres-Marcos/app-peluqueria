package com.metanoia.app.ui.backup;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.metanoia.app.R;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderState;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderStatus;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.FragmentBackupBinding;
import com.metanoia.app.ui.backup.BackupViewModel.BackupUiState;
import com.metanoia.app.util.Formats;

import java.io.File;

/**
 * Pantalla de administración de backups, carpeta de destino SAF, exportación y restauración.
 */
public class BackupFragment extends Fragment {

    private FragmentBackupBinding binding;
    private BackupViewModel viewModel;
    private boolean settingsPopulated;

    private final ActivityResultLauncher<Uri> pickFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    viewModel.onFolderSelected(uri);
                }
            });

    private final ActivityResultLauncher<String> exportBackupLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/zip"),
                    uri -> {
                        if (uri != null) {
                            viewModel.exportBackupToUri(uri);
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBackupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        binding.btnChooseBackupFolder.setOnClickListener(v -> pickFolderLauncher.launch(null));
        binding.btnBackupNow.setOnClickListener(v -> viewModel.runManualBackup());
        binding.btnExportBackup.setOnClickListener(
                v -> exportBackupLauncher.launch(viewModel.suggestExportFileName()));
        binding.btnShareBackup.setOnClickListener(v -> viewModel.prepareShareBackup());
        binding.btnSaveBackupSettings.setOnClickListener(v -> submitSettings());

        viewModel.observeUiState().observe(getViewLifecycleOwner(), this::bindState);
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeShareFile().observe(getViewLifecycleOwner(), event -> {
            File file = event.getContentIfNotHandled();
            if (file != null) {
                shareBackupFile(file);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void bindState(@NonNull BackupUiState state) {
        FolderStatus folder = state.folderStatus;
        if (folder.state == FolderState.READY) {
            binding.backupPermissionBanner.setVisibility(View.GONE);
            binding.txtBackupFolderStatus.setText(
                    getString(R.string.backup_folder_ready, folder.displayName));
            binding.txtBackupFolderDetail.setText(R.string.backup_folder_ready_detail);
            binding.btnChooseBackupFolder.setText(R.string.backup_change_folder_btn);
        } else if (folder.state == FolderState.PERMISSION_LOST) {
            binding.backupPermissionBanner.setVisibility(View.VISIBLE);
            binding.backupPermissionBanner.setMessage(R.string.backup_banner_lost_permission);
            binding.backupPermissionBanner.setAction(
                    R.string.backup_banner_action_choose,
                    v -> pickFolderLauncher.launch(null));
            binding.txtBackupFolderStatus.setText(
                    getString(R.string.backup_folder_permission_lost, folder.displayName));
            binding.txtBackupFolderDetail.setText(R.string.backup_folder_permission_lost_detail);
            binding.btnChooseBackupFolder.setText(R.string.backup_choose_folder_btn);
        } else {
            binding.backupPermissionBanner.setVisibility(View.VISIBLE);
            binding.backupPermissionBanner.setMessage(R.string.backup_banner_missing_folder);
            binding.backupPermissionBanner.setAction(
                    R.string.backup_banner_action_choose,
                    v -> pickFolderLauncher.launch(null));
            binding.txtBackupFolderStatus.setText(R.string.backup_folder_not_configured);
            binding.txtBackupFolderDetail.setText(R.string.backup_folder_not_configured_detail);
            binding.btnChooseBackupFolder.setText(R.string.backup_choose_folder_btn);
        }

        if (state.lastBackupSuccessMillis > 0L) {
            binding.txtLastBackupInfo.setText(getString(
                    R.string.backup_last_summary_format,
                    Formats.formatDateTime(state.lastBackupSuccessMillis),
                    state.lastBackupTrigger,
                    state.lastBackupFileName));
        } else {
            binding.txtLastBackupInfo.setText(R.string.backup_last_none);
        }

        if (!state.lastBackupError.isEmpty()) {
            binding.txtLastBackupError.setVisibility(View.VISIBLE);
            binding.txtLastBackupError.setText(
                    getString(R.string.backup_last_error_format, state.lastBackupError));
        } else {
            binding.txtLastBackupError.setVisibility(View.GONE);
        }

        binding.btnBackupNow.setEnabled(folder.state == FolderState.READY && !state.busy);
        binding.btnExportBackup.setEnabled(!state.busy);
        binding.btnShareBackup.setEnabled(!state.busy);
        binding.btnChooseBackupFolder.setEnabled(!state.busy);
        binding.btnSaveBackupSettings.setEnabled(!state.busy);

        if (!settingsPopulated) {
            settingsPopulated = true;
            binding.backupFrequencyGroup.check(frequencyButtonFor(state.autoFrequencyHours));
            binding.inputBackupRetentionDays.setText(String.valueOf(state.retentionDays));
            binding.inputBackupBackgroundHours.setText(String.valueOf(state.onBackgroundHours));
            binding.inputBackupOverdueDays.setText(String.valueOf(state.overdueWarningDays));
        }
    }

    private void submitSettings() {
        int checkedId = binding.backupFrequencyGroup.getCheckedButtonId();
        int freqHours = DevicePrefs.DEFAULT_AUTO_FREQUENCY_HOURS;
        if (checkedId == R.id.btnFreq12h) {
            freqHours = DevicePrefs.AUTO_FREQ_12H;
        } else if (checkedId == R.id.btnFreq48h) {
            freqHours = DevicePrefs.AUTO_FREQ_48H;
        }
        int retentionDays = parsePositiveInt(binding.inputBackupRetentionDays.getText());
        int bgHours = parsePositiveInt(binding.inputBackupBackgroundHours.getText());
        int overdueDays = parsePositiveInt(binding.inputBackupOverdueDays.getText());
        viewModel.saveSettings(freqHours, retentionDays, bgHours, overdueDays);
    }

    private int frequencyButtonFor(int hours) {
        if (hours == DevicePrefs.AUTO_FREQ_12H) {
            return R.id.btnFreq12h;
        }
        if (hours == DevicePrefs.AUTO_FREQ_48H) {
            return R.id.btnFreq48h;
        }
        return R.id.btnFreq24h;
    }

    private int parsePositiveInt(@Nullable CharSequence raw) {
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void shareBackupFile(@NonNull File file) {
        Uri contentUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("application/zip");
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(
                    sendIntent, getString(R.string.backup_share_chooser_title)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), R.string.backup_share_no_app, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
