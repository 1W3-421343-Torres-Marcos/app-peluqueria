package com.metanoia.app.ui.professionals;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.databinding.FragmentProfessionalFormBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Professional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Formulario de alta/edición de profesional: datos, comisión, servicios, horario y días libres.
 */
public class ProfessionalFormFragment extends Fragment {

    public static final String ARG_PROFESSIONAL_ID = "professional_id";

    private FragmentProfessionalFormBinding binding;
    private ProfessionalsViewModel viewModel;
    private String editingId;
    private final Set<String> selectedServiceIds = new LinkedHashSet<>();
    private final Set<Long> daysOff = new LinkedHashSet<>();
    private final Map<String, MaterialCheckBox> serviceCheckBoxes = new LinkedHashMap<>();
    private boolean professionalLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfessionalFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editingId = getArguments() == null ? null : getArguments().getString(ARG_PROFESSIONAL_ID);
        boolean isEdit = editingId != null && !editingId.isEmpty();
        binding.switchProActive.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        if (!isEdit) {
            binding.switchProActive.setChecked(true);
            binding.inputProCommission.setText("0");
        }

        viewModel = new ViewModelProvider(this).get(ProfessionalsViewModel.class);
        viewModel.loadForForm(editingId);

        viewModel.observeActiveServices().observe(getViewLifecycleOwner(), this::renderServiceChecklist);
        viewModel.observeDefaultSalonSchedule().observe(getViewLifecycleOwner(), schedule -> {
            if (!isEdit && schedule != null && !professionalLoaded) {
                binding.proScheduleEditor.setSchedule(schedule);
            }
        });
        viewModel.observeLoadedProfessional().observe(getViewLifecycleOwner(), pro -> {
            if (pro != null && !professionalLoaded) {
                professionalLoaded = true;
                populateProfessional(pro);
            }
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeSavedSuccess().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                NavHostFragment.findNavController(this).navigateUp();
            }
        });

        binding.btnAddDayOff.setOnClickListener(v -> openDayOffPicker());
        binding.btnSaveProfessional.setOnClickListener(v -> submitForm(isEdit));
    }

    private void populateProfessional(Professional pro) {
        binding.inputProName.setText(pro.name());
        binding.inputProPhone.setText(pro.phone());
        binding.inputProCommission.setText(
                BigDecimal.valueOf(pro.commissionBasisPoints())
                        .movePointLeft(2)
                        .stripTrailingZeros()
                        .toPlainString()
        );
        binding.switchProActive.setChecked(pro.isActive());
        selectedServiceIds.clear();
        selectedServiceIds.addAll(pro.serviceIds());
        syncChecklistChecks();
        binding.proScheduleEditor.setSchedule(pro.workSchedules());
        daysOff.clear();
        daysOff.addAll(pro.daysOff());
        renderDaysOff();
    }

    private void renderServiceChecklist(@Nullable List<SalonServiceEntity> services) {
        binding.servicesChecklistContainer.removeAllViews();
        serviceCheckBoxes.clear();
        boolean empty = services == null || services.isEmpty();
        binding.txtNoServicesHint.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        for (SalonServiceEntity service : services) {
            MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
            checkBox.setText(service.category + " — " + service.name);
            checkBox.setChecked(selectedServiceIds.contains(service.id));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedServiceIds.add(service.id);
                } else {
                    selectedServiceIds.remove(service.id);
                }
            });
            serviceCheckBoxes.put(service.id, checkBox);
            binding.servicesChecklistContainer.addView(checkBox);
        }
    }

    private void syncChecklistChecks() {
        for (Map.Entry<String, MaterialCheckBox> entry : serviceCheckBoxes.entrySet()) {
            entry.getValue().setChecked(selectedServiceIds.contains(entry.getKey()));
        }
    }

    private void openDayOffPicker() {
        LocalDate today = LocalDate.now();
        new DatePickerDialog(
                requireContext(),
                (picker, year, monthZeroBased, dayOfMonth) -> {
                    long epochDay = LocalDate.of(year, monthZeroBased + 1, dayOfMonth).toEpochDay();
                    daysOff.add(epochDay);
                    renderDaysOff();
                },
                today.getYear(),
                today.getMonthValue() - 1,
                today.getDayOfMonth()
        ).show();
    }

    private void renderDaysOff() {
        binding.daysOffChipGroup.removeAllViews();
        List<Long> sorted = new ArrayList<>(daysOff);
        Collections.sort(sorted);
        for (long epochDay : sorted) {
            Chip chip = new Chip(requireContext());
            chip.setText(Formats.formatDate(epochDay));
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                daysOff.remove(epochDay);
                renderDaysOff();
            });
            binding.daysOffChipGroup.addView(chip);
        }
    }

    private void submitForm(boolean isEdit) {
        if (binding.proScheduleEditor.hasInvalidRange()) {
            Toast.makeText(requireContext(), R.string.professional_error_validation, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Integer basisPoints = parseCommissionBasisPoints(textOf(binding.inputProCommission));
        if (basisPoints == null) {
            Toast.makeText(requireContext(), R.string.professional_error_validation, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        boolean active = !isEdit || binding.switchProActive.isChecked();
        viewModel.save(
                editingId,
                textOf(binding.inputProName),
                textOf(binding.inputProPhone),
                basisPoints,
                active,
                binding.proScheduleEditor.getSchedule(),
                selectedServiceIds,
                daysOff
        );
    }

    @Nullable
    private static Integer parseCommissionBasisPoints(String raw) {
        if (raw.isEmpty()) {
            return 0;
        }
        String normalized = raw.replace(",", ".");
        try {
            BigDecimal pct = new BigDecimal(normalized);
            return pct.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    private static String textOf(com.google.android.material.textfield.TextInputEditText editText) {
        CharSequence cs = editText.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
