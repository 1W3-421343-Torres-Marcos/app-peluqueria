package com.metanoia.app.ui.clients.importcontacts;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogContactsImportBinding;
import com.metanoia.app.ui.clients.importcontacts.ContactsImportViewModel.Step;
import com.metanoia.app.ui.clients.importcontacts.ContactsImportViewModel.UiState;
import com.metanoia.domain.model.ContactImportPreview;
import com.metanoia.domain.model.ContactSyncOutcome;

/**
 * Flujo modal para explicación previa de permiso, vista previa de contadores e importación masiva.
 */
public class ContactsImportDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "contacts_import_dialog";

    private DialogContactsImportBinding binding;
    private ContactsImportViewModel viewModel;
    private ActivityResultLauncher<String> permissionLauncher;

    public static ContactsImportDialogFragment newInstance() {
        return new ContactsImportDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> viewModel.onPermissionResult(Boolean.TRUE.equals(granted)));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogContactsImportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ContactsImportViewModel.class);
        wireActions();
        viewModel.observeState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.onResumeCheckPermission();
        }
    }

    private void wireActions() {
        binding.btnContactsImportContinue.setOnClickListener(
                v -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS));
        binding.btnContactsImportOpenSettings.setOnClickListener(v -> openAppSettings());
        binding.btnContactsImportNotNow.setOnClickListener(v -> dismiss());
        binding.btnContactsImportCancelPreview.setOnClickListener(v -> dismiss());
        binding.btnContactsImportConfirmAll.setOnClickListener(v -> viewModel.confirmImportAll());
        binding.btnContactsImportDone.setOnClickListener(v -> dismiss());
    }

    private void renderState(@NonNull UiState state) {
        binding.contactsImportExplainGroup.setVisibility(
                state.step == Step.EXPLAIN ? View.VISIBLE : View.GONE);
        boolean inProgress = state.step == Step.LOADING || state.step == Step.IMPORTING;
        binding.contactsImportProgressGroup.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        binding.contactsImportPreviewGroup.setVisibility(
                state.step == Step.PREVIEW ? View.VISIBLE : View.GONE);
        boolean finished = state.step == Step.DONE || state.step == Step.ERROR;
        binding.contactsImportDoneGroup.setVisibility(finished ? View.VISIBLE : View.GONE);

        setCancelable(!inProgress);

        if (state.step == Step.EXPLAIN) {
            renderExplainState(state);
        } else if (inProgress) {
            binding.contactsImportProgressLabel.setText(state.step == Step.IMPORTING
                    ? R.string.contacts_import_saving_status
                    : R.string.contacts_import_reading_status);
        } else if (state.step == Step.PREVIEW && state.preview != null) {
            renderPreviewState(state.preview);
        } else if (state.step == Step.DONE) {
            renderDoneState(state.outcome);
        } else if (state.step == Step.ERROR) {
            binding.contactsImportResultMessage.setText(
                    state.errorResId == 0 ? R.string.contacts_import_error : state.errorResId);
        }
    }

    private void renderExplainState(UiState state) {
        binding.contactsImportPermissionDeniedText.setVisibility(
                state.permissionDenied ? View.VISIBLE : View.GONE);
        boolean showSettings = state.permissionDenied
                && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS);
        binding.btnContactsImportOpenSettings.setVisibility(
                showSettings ? View.VISIBLE : View.GONE);
    }

    private void renderPreviewState(@NonNull ContactImportPreview preview) {
        binding.previewToImportCount.setText(String.valueOf(preview.toImportCount()));
        binding.previewAlreadyLoadedCount.setText(String.valueOf(preview.alreadyLoadedCount()));
        binding.previewSkippedCount.setText(String.valueOf(preview.skippedCount()));
        binding.btnContactsImportConfirmAll.setText(preview.toImportCount() > 0
                ? R.string.contacts_import_all_btn
                : R.string.contacts_import_finish_empty_btn);
    }

    private void renderDoneState(@Nullable ContactSyncOutcome outcome) {
        int imported = outcome == null ? 0 : outcome.importedCount();
        if (imported == 0) {
            binding.contactsImportResultMessage.setText(R.string.contacts_import_done_none);
        } else {
            binding.contactsImportResultMessage.setText(
                    getResources().getQuantityString(
                            R.plurals.contacts_import_done_count, imported, imported));
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
