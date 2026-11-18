package com.metanoia.app.ui.services;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.databinding.DialogServiceFormBinding;
import com.metanoia.domain.model.SalonService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet para crear o editar un servicio del catálogo.
 */
public class ServiceFormDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_PRICE_CENTS = "price_cents";
    private static final String ARG_DURATION_MIN = "duration_min";
    private static final String ARG_COLOR_SERVICE = "color_service";
    private static final String ARG_ACTIVE = "active";
    private static final String ARG_SLOT_MINUTES = "slot_minutes";

    private DialogServiceFormBinding binding;
    private int selectedDurationMin;
    private final List<Integer> durationOptions = new ArrayList<>();

    public static ServiceFormDialogFragment newInstance(
            @Nullable SalonServiceEntity existing,
            int slotMinutes
    ) {
        Bundle args = new Bundle();
        args.putInt(ARG_SLOT_MINUTES, slotMinutes > 0 ? slotMinutes : 15);
        if (existing != null) {
            args.putString(ARG_ID, existing.id);
            args.putString(ARG_NAME, existing.name);
            args.putString(ARG_CATEGORY, existing.category);
            args.putLong(ARG_PRICE_CENTS, existing.priceCents);
            args.putInt(ARG_DURATION_MIN, existing.durationMin);
            args.putBoolean(ARG_COLOR_SERVICE, existing.colorService);
            args.putBoolean(ARG_ACTIVE, existing.active);
        }
        ServiceFormDialogFragment fragment = new ServiceFormDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogServiceFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        String existingId = args.getString(ARG_ID);
        boolean isEdit = existingId != null;
        int slotMinutes = args.getInt(ARG_SLOT_MINUTES, 15);

        binding.dialogServiceTitle.setText(isEdit
                ? R.string.service_form_edit_title
                : R.string.service_form_new_title);
        binding.switchServiceActive.setVisibility(isEdit ? View.VISIBLE : View.GONE);

        setupCategoryDropdown(isEdit ? args.getString(ARG_CATEGORY, "") : "");
        int initialDuration = isEdit
                ? args.getInt(ARG_DURATION_MIN, slotMinutes * 2)
                : slotMinutes * 2;
        setupDurationDropdown(slotMinutes, initialDuration);

        if (isEdit) {
            binding.inputServiceName.setText(args.getString(ARG_NAME, ""));
            long priceCents = args.getLong(ARG_PRICE_CENTS, 0L);
            binding.inputServicePrice.setText(
                    BigDecimal.valueOf(priceCents).movePointLeft(2).stripTrailingZeros().toPlainString());
            binding.switchServiceColor.setChecked(args.getBoolean(ARG_COLOR_SERVICE, false));
            binding.switchServiceActive.setChecked(args.getBoolean(ARG_ACTIVE, true));
        } else {
            binding.switchServiceActive.setChecked(true);
        }

        ServicesViewModel viewModel = new ViewModelProvider(requireParentFragment())
                .get(ServicesViewModel.class);
        viewModel.observeCloseForm().observe(getViewLifecycleOwner(), event -> {
            Boolean shouldClose = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(shouldClose)) {
                dismiss();
            }
        });
        binding.btnSaveService.setOnClickListener(v -> submit(viewModel, existingId, isEdit));
    }

    private void setupCategoryDropdown(String initialCategory) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                SalonService.SUGGESTED_CATEGORIES
        );
        binding.inputServiceCategory.setAdapter(adapter);
        if (!initialCategory.isEmpty()) {
            binding.inputServiceCategory.setText(initialCategory, false);
        }
    }

    private void setupDurationDropdown(int slotMinutes, int initialDuration) {
        durationOptions.clear();
        List<String> labels = new ArrayList<>();
        for (int mult = 1; mult <= 16; mult++) {
            int mins = slotMinutes * mult;
            durationOptions.add(mins);
            labels.add(getString(R.string.service_duration_minutes, mins));
        }
        if (!durationOptions.contains(initialDuration) && initialDuration > 0) {
            durationOptions.add(0, initialDuration);
            labels.add(0, getString(R.string.service_duration_minutes, initialDuration));
        }
        selectedDurationMin = initialDuration;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        binding.inputServiceDuration.setAdapter(adapter);
        binding.inputServiceDuration.setText(
                getString(R.string.service_duration_minutes, selectedDurationMin), false);
        binding.inputServiceDuration.setOnItemClickListener((parent, view, position, id) ->
                selectedDurationMin = durationOptions.get(position));
    }

    private void submit(ServicesViewModel viewModel, @Nullable String existingId, boolean isEdit) {
        String name = textOf(binding.inputServiceName);
        String category = textOf(binding.inputServiceCategory);
        Long priceCents = parsePriceToCents(textOf(binding.inputServicePrice));
        if (priceCents == null) {
            Toast.makeText(requireContext(), R.string.service_error_validation, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        boolean active = !isEdit || binding.switchServiceActive.isChecked();
        viewModel.saveService(
                existingId,
                name,
                category,
                priceCents,
                selectedDurationMin,
                binding.switchServiceColor.isChecked(),
                active,
                0
        );
    }

    @Nullable
    private static Long parsePriceToCents(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        String normalized = raw.replace(",", ".");
        try {
            BigDecimal amount = new BigDecimal(normalized);
            return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    private static String textOf(android.widget.TextView view) {
        CharSequence cs = view.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
