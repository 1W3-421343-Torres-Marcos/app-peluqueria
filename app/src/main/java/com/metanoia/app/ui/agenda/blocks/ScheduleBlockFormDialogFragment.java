package com.metanoia.app.ui.agenda.blocks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.DialogScheduleBlockFormBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ScheduleBlockType;

import java.time.LocalDate;
import java.util.List;

/**
 * Bottom sheet para crear bloqueos de agenda generales o por profesional.
 */
public class ScheduleBlockFormDialogFragment extends BottomSheetDialogFragment {

    private DialogScheduleBlockFormBinding binding;
    private ScheduleBlocksViewModel viewModel;
    @Nullable
    private String selectedProfessionalId;
    private long startDayEpoch;
    private long endDayEpoch;
    private int startMin = 13 * 60;
    private int endMin = 14 * 60;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogScheduleBlockFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment())
                .get(ScheduleBlocksViewModel.class);

        long today = viewModel.todayEpochDay();
        startDayEpoch = today;
        endDayEpoch = today;
        updateDateButtons();
        updateTimeButtons();

        binding.switchBlockFullDay.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.blockTimeRangeContainer.setVisibility(isChecked ? View.GONE : View.VISIBLE));
        binding.btnBlockStartDay.setOnClickListener(v -> pickDate(true));
        binding.btnBlockEndDay.setOnClickListener(v -> pickDate(false));
        binding.btnBlockStartTime.setOnClickListener(v -> pickTime(true));
        binding.btnBlockEndTime.setOnClickListener(v -> pickTime(false));
        binding.btnSaveScheduleBlock.setOnClickListener(v -> submit());

        viewModel.observeActiveProfessionals().observe(getViewLifecycleOwner(), this::renderScopes);
        viewModel.observeCloseForm().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                dismiss();
            }
        });
    }

    private void renderScopes(@Nullable List<ProfessionalWithDetails> professionals) {
        binding.blockScopeChipGroup.removeAllViews();

        Chip allChip = new Chip(requireContext());
        allChip.setId(View.generateViewId());
        allChip.setCheckable(true);
        allChip.setText(R.string.agenda_block_scope_all);
        allChip.setChecked(selectedProfessionalId == null);
        allChip.setOnClickListener(v -> selectedProfessionalId = null);
        binding.blockScopeChipGroup.addView(allChip);

        if (professionals == null) {
            return;
        }
        for (ProfessionalWithDetails pro : professionals) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(pro.professional.name);
            chip.setChecked(pro.professional.id.equals(selectedProfessionalId));
            chip.setOnClickListener(v -> selectedProfessionalId = pro.professional.id);
            binding.blockScopeChipGroup.addView(chip);
        }
    }

    private void updateDateButtons() {
        binding.btnBlockStartDay.setText(getString(
                R.string.schedule_block_from_date, Formats.formatDate(startDayEpoch)));
        binding.btnBlockEndDay.setText(getString(
                R.string.schedule_block_to_date, Formats.formatDate(endDayEpoch)));
    }

    private void updateTimeButtons() {
        binding.btnBlockStartTime.setText(Formats.formatTime(startMin));
        binding.btnBlockEndTime.setText(Formats.formatTime(endMin));
    }

    private void pickDate(boolean isStart) {
        LocalDate initial = LocalDate.ofEpochDay(isStart ? startDayEpoch : endDayEpoch);
        new DatePickerDialog(
                requireContext(),
                (picker, year, monthZero, day) -> {
                    long picked = LocalDate.of(year, monthZero + 1, day).toEpochDay();
                    if (isStart) {
                        startDayEpoch = picked;
                        if (endDayEpoch < startDayEpoch) {
                            endDayEpoch = startDayEpoch;
                        }
                    } else {
                        endDayEpoch = picked;
                    }
                    updateDateButtons();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        ).show();
    }

    private void pickTime(boolean isStart) {
        int current = isStart ? startMin : endMin;
        new TimePickerDialog(
                requireContext(),
                (picker, hourOfDay, minute) -> {
                    int value = hourOfDay * 60 + minute;
                    if (isStart) {
                        startMin = value;
                    } else {
                        endMin = value;
                    }
                    updateTimeButtons();
                },
                current / 60,
                current % 60,
                true
        ).show();
    }

    private void submit() {
        boolean fullDay = binding.switchBlockFullDay.isChecked();
        CharSequence reasonCs = binding.inputBlockReason.getText();
        String reason = reasonCs == null ? "" : reasonCs.toString().trim();
        viewModel.createBlock(
                selectedProfessionalId,
                startDayEpoch,
                endDayEpoch,
                fullDay ? null : startMin,
                fullDay ? null : endMin,
                selectedType(),
                reason
        );
    }

    private ScheduleBlockType selectedType() {
        int checkedId = binding.blockTypeChipGroup.getCheckedChipId();
        if (checkedId == R.id.chipBlockVacation) {
            return ScheduleBlockType.VACATION;
        }
        if (checkedId == R.id.chipBlockLunch) {
            return ScheduleBlockType.LUNCH;
        }
        if (checkedId == R.id.chipBlockHoliday) {
            return ScheduleBlockType.HOLIDAY;
        }
        return ScheduleBlockType.OTHER;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
