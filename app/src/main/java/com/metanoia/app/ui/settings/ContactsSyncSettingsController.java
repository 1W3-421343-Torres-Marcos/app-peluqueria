package com.metanoia.app.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.contacts.ContactSyncCoordinator;
import com.metanoia.app.data.contacts.ContactSyncScheduler;
import com.metanoia.app.data.contacts.DeviceContactsReader;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.FragmentSettingsBinding;
import com.metanoia.app.util.Formats;

/**
 * Controla el interruptor, aviso por permiso revocado y estado de la sincronización automática.
 */
final class ContactsSyncSettingsController {

    private final Fragment host;
    private final ActivityResultLauncher<String> permissionLauncher;
    @Nullable
    private FragmentSettingsBinding binding;
    private boolean updatingUi;

    ContactsSyncSettingsController(@NonNull Fragment host) {
        this.host = host;
        this.permissionLauncher = host.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::onPermissionResult);
    }

    void bind(@NonNull FragmentSettingsBinding fragmentBinding) {
        this.binding = fragmentBinding;
        fragmentBinding.switchContactsAutoSync.setOnCheckedChangeListener((btn, isChecked) -> {
            if (updatingUi) {
                return;
            }
            handleSwitchToggled(isChecked);
        });
        fragmentBinding.bannerContactsSyncPermission.setAction(
                R.string.settings_contacts_sync_banner_action,
                v -> requestOrOpenPermissionSettings());
        refresh();
    }

    void unbind() {
        this.binding = null;
    }

    void refresh() {
        if (binding == null || !host.isAdded()) {
            return;
        }
        Context context = host.requireContext();
        DevicePrefs prefs = MetanoiaApp.container(context).devicePrefs();
        boolean enabled = prefs.isContactSyncEnabled();
        boolean hasPermission = DeviceContactsReader.hasPermission(context);

        if (enabled && hasPermission) {
            ContactSyncScheduler.scheduleContentUriTrigger(context, false);
        }

        updatingUi = true;
        binding.switchContactsAutoSync.setChecked(enabled);
        updatingUi = false;

        boolean showRevokedBanner = !hasPermission
                && (enabled || DevicePrefs.SYNC_STATUS_NO_PERMISSION.equals(
                prefs.getLastContactSyncStatus()));
        binding.bannerContactsSyncPermission.setVisibility(
                showRevokedBanner ? View.VISIBLE : View.GONE);
        if (showRevokedBanner) {
            binding.bannerContactsSyncPermission.setMessage(
                    R.string.settings_contacts_sync_banner_revoked);
        }
        binding.textContactsSyncStatus.setText(buildStatusText(context, prefs, hasPermission));
    }

    private void handleSwitchToggled(boolean enableRequested) {
        Context context = host.requireContext();
        DevicePrefs prefs = MetanoiaApp.container(context).devicePrefs();
        if (!enableRequested) {
            prefs.setContactSyncEnabled(false);
            ContactSyncScheduler.cancelContentUriTrigger(context);
            refresh();
            return;
        }
        if (!DeviceContactsReader.hasPermission(context)) {
            updatingUi = true;
            if (binding != null) {
                binding.switchContactsAutoSync.setChecked(false);
            }
            updatingUi = false;
            showExplanationAndRequestPermission();
            return;
        }
        enableAndSyncNow(context);
    }

    private void onPermissionResult(@Nullable Boolean granted) {
        if (!host.isAdded()) {
            return;
        }
        Context context = host.requireContext();
        if (Boolean.TRUE.equals(granted)) {
            enableAndSyncNow(context);
            return;
        }
        if (!host.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            showOpenSettingsDialog();
        }
        refresh();
    }

    private void enableAndSyncNow(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        AppContainer container = MetanoiaApp.container(appContext);
        container.devicePrefs().setContactSyncEnabled(true);
        ContactSyncScheduler.scheduleContentUriTrigger(appContext, true);
        if (binding != null) {
            updatingUi = true;
            binding.switchContactsAutoSync.setChecked(true);
            updatingUi = false;
            binding.textContactsSyncStatus.setText(R.string.settings_contacts_sync_in_progress);
        }
        container.executors().io().execute(() -> {
            ContactSyncCoordinator.runAutomaticSync(appContext);
            if (host.isAdded()) {
                host.requireActivity().runOnUiThread(this::refresh);
            }
        });
    }

    private void requestOrOpenPermissionSettings() {
        if (!host.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                && DevicePrefs.SYNC_STATUS_NO_PERMISSION.equals(
                MetanoiaApp.container(host.requireContext())
                        .devicePrefs()
                        .getLastContactSyncStatus())) {
            openAppSettings();
            return;
        }
        showExplanationAndRequestPermission();
    }

    private void showExplanationAndRequestPermission() {
        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.settings_contacts_sync_explain_title)
                .setMessage(R.string.settings_contacts_sync_explain_body)
                .setNegativeButton(R.string.contacts_import_not_now_btn, (d, w) -> refresh())
                .setPositiveButton(R.string.contacts_import_continue_btn,
                        (d, w) -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS))
                .show();
    }

    private void showOpenSettingsDialog() {
        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.settings_contacts_sync_explain_title)
                .setMessage(R.string.contacts_import_permission_denied)
                .setNegativeButton(R.string.contacts_import_not_now_btn, null)
                .setPositiveButton(R.string.contacts_import_open_settings_btn,
                        (d, w) -> openAppSettings())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", host.requireContext().getPackageName(), null));
        host.startActivity(intent);
    }

    @NonNull
    private static String buildStatusText(
            @NonNull Context context,
            @NonNull DevicePrefs prefs,
            boolean hasPermission
    ) {
        String status = prefs.getLastContactSyncStatus();
        long lastMillis = prefs.getLastContactSyncMillis();
        String formattedTime = lastMillis > 0L ? Formats.formatDateTime(lastMillis) : "";
        if (!hasPermission && (prefs.isContactSyncEnabled()
                || DevicePrefs.SYNC_STATUS_NO_PERMISSION.equals(status))) {
            return formattedTime.isEmpty()
                    ? context.getString(R.string.settings_contacts_sync_paused_permission)
                    : context.getString(
                            R.string.settings_contacts_sync_paused_permission_dated,
                            formattedTime);
        }
        if (lastMillis <= 0L || DevicePrefs.SYNC_STATUS_NONE.equals(status)) {
            return context.getString(R.string.settings_contacts_sync_never);
        }
        if (DevicePrefs.SYNC_STATUS_ERROR.equals(status)) {
            return context.getString(R.string.settings_contacts_sync_error, formattedTime);
        }
        int added = prefs.getLastContactSyncAdded();
        if (added <= 0) {
            return context.getString(R.string.settings_contacts_sync_none_added, formattedTime);
        }
        return context.getResources().getQuantityString(
                R.plurals.settings_contacts_sync_added_format, added, formattedTime, added);
    }
}
