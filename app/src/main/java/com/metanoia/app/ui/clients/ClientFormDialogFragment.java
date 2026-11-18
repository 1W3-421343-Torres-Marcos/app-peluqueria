package com.metanoia.app.ui.clients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.databinding.DialogClientFormBinding;
import com.metanoia.domain.model.Client;

/**
 * Bottom sheet reutilizable para alta completa, edición o alta rápida (nombre + teléfono).
 */
public class ClientFormDialogFragment extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "client_form_result";
    public static final String KEY_RESULT_TYPE = "result_type";
    public static final String RESULT_SAVED = "saved";
    public static final String RESULT_OPEN_EXISTING = "open_existing";
    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_CLIENT_NAME = "client_name";
    public static final String KEY_CLIENT_PHONE = "client_phone";

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_PHONE = "phone";
    private static final String ARG_BIRTH_DAY = "birth_day";
    private static final String ARG_BIRTH_MONTH = "birth_month";
    private static final String ARG_BIRTH_YEAR = "birth_year";
    private static final String ARG_NOTES = "notes";
    private static final String ARG_ACTIVE = "active";
    private static final String ARG_QUICK_CREATE = "quick_create";

    private DialogClientFormBinding binding;
    private ClientsViewModel viewModel;

    public static ClientFormDialogFragment newCreateInstance() {
        return buildInstance(null, false);
    }

    public static ClientFormDialogFragment newQuickCreateInstance() {
        return buildInstance(null, true);
    }

    public static ClientFormDialogFragment newEditInstance(@NonNull ClientEntity existing) {
        return buildInstance(existing, false);
    }

    private static ClientFormDialogFragment buildInstance(
            @Nullable ClientEntity existing,
            boolean quickCreate
    ) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_QUICK_CREATE, quickCreate);
        if (existing != null) {
            args.putString(ARG_ID, existing.id);
            args.putString(ARG_NAME, existing.name);
            args.putString(ARG_PHONE, existing.phone);
            if (existing.birthDay != null) {
                args.putInt(ARG_BIRTH_DAY, existing.birthDay);
            }
            if (existing.birthMonth != null) {
                args.putInt(ARG_BIRTH_MONTH, existing.birthMonth);
            }
            if (existing.birthYear != null) {
                args.putInt(ARG_BIRTH_YEAR, existing.birthYear);
            }
            args.putString(ARG_NOTES, existing.notes);
            args.putBoolean(ARG_ACTIVE, existing.active);
        }
        ClientFormDialogFragment fragment = new ClientFormDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogClientFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        String existingId = args.getString(ARG_ID);
        boolean isEdit = existingId != null && !existingId.isEmpty();
        boolean isQuickCreate = args.getBoolean(ARG_QUICK_CREATE, false);

        configureMode(args, isEdit, isQuickCreate);

        viewModel = new ViewModelProvider(this).get(ClientsViewModel.class);
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeSavedClient().observe(getViewLifecycleOwner(), event -> {
            Client saved = event.getContentIfNotHandled();
            if (saved != null) {
                emitSavedResult(saved);
                dismiss();
            }
        });
        viewModel.observeDuplicateClientId().observe(getViewLifecycleOwner(), event -> {
            String duplicateId = event.getContentIfNotHandled();
            if (duplicateId != null) {
                showDuplicateDialog(duplicateId);
            }
        });

        binding.btnSaveClient.setOnClickListener(v -> submit(existingId, isEdit, isQuickCreate));
    }

    private void configureMode(Bundle args, boolean isEdit, boolean isQuickCreate) {
        if (isQuickCreate) {
            binding.dialogClientTitle.setText(R.string.client_form_quick_title);
        } else {
            binding.dialogClientTitle.setText(isEdit
                    ? R.string.client_form_edit_title
                    : R.string.client_form_new_title);
        }

        binding.clientOptionalDetailsSection.setVisibility(
                isQuickCreate ? View.GONE : View.VISIBLE);
        binding.switchClientActive.setVisibility(isEdit ? View.VISIBLE : View.GONE);

        if (isEdit) {
            binding.inputClientName.setText(args.getString(ARG_NAME, ""));
            binding.inputClientPhone.setText(args.getString(ARG_PHONE, ""));
            if (args.containsKey(ARG_BIRTH_DAY)) {
                binding.inputClientBirthDay.setText(String.valueOf(args.getInt(ARG_BIRTH_DAY)));
            }
            if (args.containsKey(ARG_BIRTH_MONTH)) {
                binding.inputClientBirthMonth.setText(
                        String.valueOf(args.getInt(ARG_BIRTH_MONTH)));
            }
            if (args.containsKey(ARG_BIRTH_YEAR)) {
                binding.inputClientBirthYear.setText(String.valueOf(args.getInt(ARG_BIRTH_YEAR)));
            }
            binding.inputClientNotes.setText(args.getString(ARG_NOTES, ""));
            binding.switchClientActive.setChecked(args.getBoolean(ARG_ACTIVE, true));
        } else {
            binding.switchClientActive.setChecked(true);
        }
    }

    private void submit(@Nullable String existingId, boolean isEdit, boolean isQuickCreate) {
        String name = textOf(binding.inputClientName);
        String phone = textOf(binding.inputClientPhone);
        Integer birthDay = null;
        Integer birthMonth = null;
        Integer birthYear = null;
        String notes = "";
        boolean active = true;

        if (!isQuickCreate) {
            birthDay = parseOptionalInt(textOf(binding.inputClientBirthDay));
            birthMonth = parseOptionalInt(textOf(binding.inputClientBirthMonth));
            birthYear = parseOptionalInt(textOf(binding.inputClientBirthYear));
            if (hasUnparseableBirthday(
                    textOf(binding.inputClientBirthDay), birthDay,
                    textOf(binding.inputClientBirthMonth), birthMonth,
                    textOf(binding.inputClientBirthYear), birthYear)) {
                Toast.makeText(requireContext(), R.string.client_error_validation,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            notes = textOf(binding.inputClientNotes);
            active = !isEdit || binding.switchClientActive.isChecked();
        }

        viewModel.saveClient(existingId, name, phone, birthDay, birthMonth, birthYear, notes, active);
    }

    private static boolean hasUnparseableBirthday(
            String rawDay, @Nullable Integer day,
            String rawMonth, @Nullable Integer month,
            String rawYear, @Nullable Integer year
    ) {
        return (!rawDay.isEmpty() && day == null)
                || (!rawMonth.isEmpty() && month == null)
                || (!rawYear.isEmpty() && year == null);
    }

    @Nullable
    private static Integer parseOptionalInt(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void showDuplicateDialog(@NonNull String existingClientId) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.client_duplicate_title)
                .setMessage(R.string.client_duplicate_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.client_duplicate_open, (dialog, which) -> {
                    Bundle result = new Bundle();
                    result.putString(KEY_RESULT_TYPE, RESULT_OPEN_EXISTING);
                    result.putString(KEY_CLIENT_ID, existingClientId);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    dismiss();
                })
                .show();
    }

    private void emitSavedResult(@NonNull Client client) {
        Bundle result = new Bundle();
        result.putString(KEY_RESULT_TYPE, RESULT_SAVED);
        result.putString(KEY_CLIENT_ID, client.id());
        result.putString(KEY_CLIENT_NAME, client.name());
        result.putString(KEY_CLIENT_PHONE, client.phone());
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private static String textOf(TextInputEditText view) {
        CharSequence cs = view.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
