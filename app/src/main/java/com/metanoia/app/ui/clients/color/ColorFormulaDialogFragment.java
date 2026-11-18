package com.metanoia.app.ui.clients.color;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.DialogColorFormulaFormBinding;
import com.metanoia.app.databinding.ItemColorComponentInputBinding;
import com.metanoia.app.ui.clients.ClientsViewModel;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ColorComponentUnit;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;
import com.metanoia.domain.rules.ColorFormulaRules.ComponentDraft;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bottom sheet para alta, detalle/edición y duplicación de fórmulas de coloración.
 */
public class ColorFormulaDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_CLIENT_ID = "client_id";
    private static final String ARG_FORMULA_ID = "formula_id";
    private static final String ARG_LOCKED = "locked";

    private DialogColorFormulaFormBinding binding;
    private ClientsViewModel viewModel;
    private final List<ComponentRowHolder> componentRows = new ArrayList<>();
    private List<ProfessionalWithDetails> allProfessionals = Collections.emptyList();

    private String clientId;
    @Nullable
    private String editingFormulaId;
    private boolean locked;
    private long selectedDayEpoch;
    @Nullable
    private String selectedProfessionalId;
    private boolean formulaPopulated;

    public static ColorFormulaDialogFragment newCreateInstance(@NonNull String clientId) {
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientId);
        ColorFormulaDialogFragment fragment = new ColorFormulaDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ColorFormulaDialogFragment newEditInstance(
            @NonNull String clientId,
            @NonNull String formulaId,
            boolean locked
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientId);
        args.putString(ARG_FORMULA_ID, formulaId);
        args.putBoolean(ARG_LOCKED, locked);
        ColorFormulaDialogFragment fragment = new ColorFormulaDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogColorFormulaFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ClientsViewModel.class);

        Bundle args = requireArguments();
        clientId = args.getString(ARG_CLIENT_ID, "");
        editingFormulaId = args.getString(ARG_FORMULA_ID);
        locked = args.getBoolean(ARG_LOCKED, false);
        boolean isEdit = editingFormulaId != null && !editingFormulaId.isEmpty();
        selectedDayEpoch = viewModel.todayEpochDay();

        configureHeaderAndActions(isEdit);
        observeViewModel(isEdit);

        if (isEdit && editingFormulaId != null) {
            viewModel.loadColorFormula(editingFormulaId);
        } else {
            updateDateLabel();
            addComponentRow(null);
            applyLockedState();
        }
    }

    private void configureHeaderAndActions(boolean isEdit) {
        binding.dialogFormulaTitle.setText(isEdit
                ? (locked ? R.string.color_formula_form_detail_title
                        : R.string.color_formula_form_edit_title)
                : R.string.color_formula_form_new_title);
        binding.btnDuplicateFormula.setVisibility(isEdit ? View.VISIBLE : View.GONE);

        binding.btnFormulaChangeDate.setOnClickListener(v -> openDatePicker());
        binding.btnAddColorComponent.setOnClickListener(v -> {
            if (!locked) {
                addComponentRow(null);
                refreshRemoveButtons();
            }
        });
        binding.btnSaveFormula.setOnClickListener(v -> submitForm());
        binding.btnDuplicateFormula.setOnClickListener(v -> {
            if (editingFormulaId != null) {
                viewModel.duplicateColorFormula(editingFormulaId);
            }
        });
        binding.btnCancelFormula.setOnClickListener(v -> dismiss());
    }

    private void observeViewModel(boolean isEdit) {
        viewModel.observeProfessionals(true).observe(getViewLifecycleOwner(), professionals -> {
            allProfessionals = professionals == null ? Collections.emptyList() : professionals;
            renderProfessionalChips();
        });
        if (isEdit) {
            viewModel.observeLoadedColorFormula().observe(getViewLifecycleOwner(), state -> {
                if (state != null && editingFormulaId != null
                        && editingFormulaId.equals(state.formula.id()) && !formulaPopulated) {
                    formulaPopulated = true;
                    locked = state.locked;
                    populateFormula(state.formula);
                }
            });
        }
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeSavedColorFormula().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                dismiss();
            }
        });
    }

    private void populateFormula(@NonNull ColorFormula formula) {
        selectedDayEpoch = formula.day();
        selectedProfessionalId = formula.professionalId().orElse(null);
        updateDateLabel();
        renderProfessionalChips();

        binding.formulaComponentsContainer.removeAllViews();
        componentRows.clear();
        for (ColorFormulaComponent component : formula.components()) {
            addComponentRow(component);
        }
        binding.inputFormulaExposure.setText(formula.exposureMin() > 0
                ? String.valueOf(formula.exposureMin())
                : "");
        binding.inputFormulaDeveloper.setText(formula.developerVolume().orElse(""));
        binding.inputFormulaNotes.setText(formula.notes());
        binding.inputFormulaResult.setText(formula.result().orElse(""));
        applyLockedState();
    }

    private void applyLockedState() {
        boolean isEdit = editingFormulaId != null && !editingFormulaId.isEmpty();
        binding.dialogFormulaTitle.setText(isEdit
                ? (locked ? R.string.color_formula_form_detail_title
                        : R.string.color_formula_form_edit_title)
                : R.string.color_formula_form_new_title);
        binding.dialogFormulaLockedNotice.setVisibility(locked ? View.VISIBLE : View.GONE);
        binding.btnSaveFormula.setVisibility(locked ? View.GONE : View.VISIBLE);
        binding.btnAddColorComponent.setVisibility(locked ? View.GONE : View.VISIBLE);
        binding.btnFormulaChangeDate.setEnabled(!locked);
        binding.inputFormulaExposure.setEnabled(!locked);
        binding.inputFormulaDeveloper.setEnabled(!locked);
        binding.inputFormulaNotes.setEnabled(!locked);
        binding.inputFormulaResult.setEnabled(!locked);

        for (int i = 0; i < binding.formulaProfessionalChips.getChildCount(); i++) {
            binding.formulaProfessionalChips.getChildAt(i).setEnabled(!locked);
        }
        for (ComponentRowHolder row : componentRows) {
            row.setEditable(!locked);
        }
        refreshRemoveButtons();
    }

    private void renderProfessionalChips() {
        binding.formulaProfessionalChips.removeAllViews();

        Chip unassignedChip = new Chip(requireContext());
        unassignedChip.setId(View.generateViewId());
        unassignedChip.setCheckable(true);
        unassignedChip.setText(R.string.color_formula_professional_unassigned);
        unassignedChip.setChecked(selectedProfessionalId == null);
        unassignedChip.setEnabled(!locked);
        unassignedChip.setOnClickListener(v -> selectedProfessionalId = null);
        binding.formulaProfessionalChips.addView(unassignedChip);

        for (ProfessionalWithDetails item : allProfessionals) {
            boolean isCurrentlyAssigned = item.professional.id.equals(selectedProfessionalId);
            if (!item.professional.active && !isCurrentlyAssigned) {
                continue;
            }
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(item.professional.name);
            chip.setChecked(isCurrentlyAssigned);
            chip.setEnabled(!locked);
            chip.setOnClickListener(v -> selectedProfessionalId = item.professional.id);
            binding.formulaProfessionalChips.addView(chip);
        }
    }

    private void addComponentRow(@Nullable ColorFormulaComponent existing) {
        ItemColorComponentInputBinding rowBinding = ItemColorComponentInputBinding.inflate(
                getLayoutInflater(), binding.formulaComponentsContainer, false);
        ComponentRowHolder holder = new ComponentRowHolder(rowBinding);
        if (existing != null) {
            holder.bind(existing);
        }
        holder.binding.btnRemoveComponent.setOnClickListener(v -> {
            if (!locked && componentRows.size() > 1) {
                componentRows.remove(holder);
                binding.formulaComponentsContainer.removeView(holder.binding.getRoot());
                refreshRemoveButtons();
            }
        });
        componentRows.add(holder);
        binding.formulaComponentsContainer.addView(rowBinding.getRoot());
        holder.setEditable(!locked);
        refreshRemoveButtons();
    }

    private void refreshRemoveButtons() {
        boolean canRemove = !locked && componentRows.size() > 1;
        for (ComponentRowHolder row : componentRows) {
            row.binding.btnRemoveComponent.setVisibility(canRemove ? View.VISIBLE : View.INVISIBLE);
            row.binding.btnRemoveComponent.setEnabled(canRemove);
        }
    }

    private void updateDateLabel() {
        binding.txtFormulaDate.setText(Formats.formatDayWithShortWeekday(selectedDayEpoch));
    }

    private void openDatePicker() {
        if (locked) {
            return;
        }
        LocalDate initial = LocalDate.ofEpochDay(selectedDayEpoch);
        new DatePickerDialog(
                requireContext(),
                (picker, year, monthZeroBased, dayOfMonth) -> {
                    selectedDayEpoch = LocalDate.of(year, monthZeroBased + 1, dayOfMonth)
                            .toEpochDay();
                    updateDateLabel();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        ).show();
    }

    private void submitForm() {
        List<ComponentDraft> drafts = new ArrayList<>(componentRows.size());
        for (ComponentRowHolder row : componentRows) {
            drafts.add(row.toDraft());
        }
        int exposureMin = parseExposureMinutes(binding.inputFormulaExposure.getText());
        if (exposureMin < 0) {
            Toast.makeText(requireContext(), R.string.color_formula_error_validation,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String developer = safeText(binding.inputFormulaDeveloper.getText());
        String notes = safeText(binding.inputFormulaNotes.getText());
        String result = safeText(binding.inputFormulaResult.getText());

        viewModel.saveColorFormula(
                editingFormulaId,
                clientId,
                null,
                selectedProfessionalId,
                selectedDayEpoch,
                exposureMin,
                developer,
                notes,
                result,
                drafts
        );
    }

    private static int parseExposureMinutes(@Nullable CharSequence cs) {
        if (cs == null) {
            return 0;
        }
        String trimmed = cs.toString().trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @NonNull
    private static String safeText(@Nullable CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        componentRows.clear();
        binding = null;
    }

    private static final class ComponentRowHolder {
        final ItemColorComponentInputBinding binding;

        ComponentRowHolder(@NonNull ItemColorComponentInputBinding binding) {
            this.binding = binding;
        }

        void bind(@NonNull ColorFormulaComponent component) {
            binding.inputComponentBrand.setText(component.brand());
            binding.inputComponentLine.setText(component.productLine().orElse(""));
            binding.inputComponentShade.setText(component.shade());
            binding.inputComponentAmount.setText(component.formattedAmount());
            if (component.unit() == ColorComponentUnit.MILLILITERS) {
                binding.unitChipMl.setChecked(true);
            } else if (component.unit() == ColorComponentUnit.PARTS) {
                binding.unitChipParts.setChecked(true);
            } else {
                binding.unitChipGrams.setChecked(true);
            }
        }

        void setEditable(boolean editable) {
            binding.inputComponentBrand.setEnabled(editable);
            binding.inputComponentLine.setEnabled(editable);
            binding.inputComponentShade.setEnabled(editable);
            binding.inputComponentAmount.setEnabled(editable);
            binding.unitChipGrams.setEnabled(editable);
            binding.unitChipMl.setEnabled(editable);
            binding.unitChipParts.setEnabled(editable);
        }

        ComponentDraft toDraft() {
            String brand = safeText(binding.inputComponentBrand.getText());
            String line = safeText(binding.inputComponentLine.getText());
            String shade = safeText(binding.inputComponentShade.getText());
            int amountTenths = ColorFormulaComponent.parseAmountTenths(
                    safeText(binding.inputComponentAmount.getText())).orElse(0);
            ColorComponentUnit unit = resolveUnit();
            return new ComponentDraft(brand, line, shade, amountTenths, unit);
        }

        private ColorComponentUnit resolveUnit() {
            int checkedId = binding.componentUnitChipGroup.getCheckedChipId();
            if (checkedId == R.id.unitChipMl) {
                return ColorComponentUnit.MILLILITERS;
            }
            if (checkedId == R.id.unitChipParts) {
                return ColorComponentUnit.PARTS;
            }
            return ColorComponentUnit.GRAMS;
        }
    }
}
