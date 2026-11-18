package com.metanoia.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.metanoia.app.R;
import com.metanoia.app.databinding.FragmentSettingsBinding;
import com.metanoia.domain.model.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de configuración del negocio, horarios generales, bloques y operadores.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private Settings loadedSettings;
    private final List<String> operators = new ArrayList<>();
    private String selectedOperator = Settings.DEFAULT_OPERATOR;
    private boolean formPopulated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        viewModel.observeSettings().observe(getViewLifecycleOwner(), settings -> {
            loadedSettings = settings;
            if (!formPopulated) {
                formPopulated = true;
                populateForm(settings);
            }
        });

        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAddOperator.setOnClickListener(v -> addOperatorFromInput());
        binding.btnSaveSettings.setOnClickListener(v -> submitForm());
    }

    private void populateForm(Settings settings) {
        binding.inputBusinessName.setText(settings.businessName());
        binding.inputSubtitle.setText(settings.subtitle());
        binding.inputAddress.setText(settings.address());
        binding.inputPhone.setText(settings.phone());
        binding.inputCuit.setText(settings.cuit());

        binding.slotMinutesGroup.check(slotButtonIdFor(settings.slotMinutes()));
        operators.clear();
        operators.addAll(settings.operators());
        selectedOperator = settings.currentOperator();
        renderOperatorChips();

        binding.scheduleEditor.setSchedule(settings.generalSchedule());
    }

    private void addOperatorFromInput() {
        CharSequence raw = binding.inputNewOperator.getText();
        String name = raw == null ? "" : raw.toString().replace(",", "").trim();
        if (name.isEmpty()) {
            return;
        }
        if (!operators.contains(name)) {
            operators.add(name);
        }
        selectedOperator = name;
        binding.inputNewOperator.setText("");
        renderOperatorChips();
    }

    private void renderOperatorChips() {
        binding.operatorsChipGroup.removeAllViews();
        for (String op : operators) {
            Chip chip = new Chip(requireContext());
            chip.setText(op);
            chip.setCheckable(true);
            chip.setChecked(op.equals(selectedOperator));
            chip.setCloseIconVisible(operators.size() > 1);
            chip.setOnClickListener(v -> selectedOperator = op);
            chip.setOnCloseIconClickListener(v -> {
                if (operators.size() <= 1) {
                    return;
                }
                operators.remove(op);
                if (selectedOperator.equals(op)) {
                    selectedOperator = operators.get(0);
                }
                renderOperatorChips();
            });
            binding.operatorsChipGroup.addView(chip);
        }
    }

    private void submitForm() {
        if (binding.scheduleEditor.hasInvalidRange()) {
            Toast.makeText(requireContext(), R.string.settings_error_validation, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Settings base = loadedSettings != null ? loadedSettings : Settings.defaultSettings();
        Settings candidate = base.toBuilder()
                .businessName(textOf(binding.inputBusinessName))
                .subtitle(textOf(binding.inputSubtitle))
                .address(textOf(binding.inputAddress))
                .phone(textOf(binding.inputPhone))
                .cuit(textOf(binding.inputCuit))
                .slotMinutes(selectedSlotMinutes())
                .operators(operators)
                .currentOperator(selectedOperator)
                .generalSchedule(binding.scheduleEditor.getSchedule())
                .build();
        viewModel.save(candidate);
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText editText) {
        CharSequence cs = editText.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private int slotButtonIdFor(int slotMinutes) {
        switch (slotMinutes) {
            case 10:
                return R.id.btnSlot10;
            case 20:
                return R.id.btnSlot20;
            case 30:
                return R.id.btnSlot30;
            case 15:
            default:
                return R.id.btnSlot15;
        }
    }

    private int selectedSlotMinutes() {
        int checkedId = binding.slotMinutesGroup.getCheckedButtonId();
        if (checkedId == R.id.btnSlot10) {
            return 10;
        }
        if (checkedId == R.id.btnSlot20) {
            return 20;
        }
        if (checkedId == R.id.btnSlot30) {
            return 30;
        }
        return 15;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
