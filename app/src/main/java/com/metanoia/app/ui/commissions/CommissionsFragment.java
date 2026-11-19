package com.metanoia.app.ui.commissions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.FragmentCommissionsBinding;
import com.metanoia.app.ui.commissions.CommissionsViewModel.CommissionsFilter;
import com.metanoia.app.ui.commissions.CommissionsViewModel.CommissionsUiState;
import com.metanoia.app.ui.commissions.CommissionsViewModel.PeriodMode;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.rules.CommissionCalculator.PeriodCalculation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Pantalla de Comisiones: selector de profesional y período (semana, mes, rango),
 * totales calculados sobre turnos cobrados y detalle por turno.
 */
public class CommissionsFragment extends Fragment {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM uuuu", Formats.ES_AR);

    private FragmentCommissionsBinding binding;
    private CommissionsViewModel viewModel;
    private CommissionDetailsAdapter detailsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCommissionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CommissionsViewModel.class);

        detailsAdapter = new CommissionDetailsAdapter();
        binding.commissionsDetailRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.commissionsDetailRecycler.setAdapter(detailsAdapter);

        setupControls();
        observeData();
    }

    private void setupControls() {
        binding.commissionsPeriodModeGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }
                    if (checkedId == R.id.btnCommissionsModeMonth) {
                        viewModel.setPeriodMode(PeriodMode.MONTH);
                    } else if (checkedId == R.id.btnCommissionsModeRange) {
                        viewModel.setPeriodMode(PeriodMode.RANGE);
                    } else {
                        viewModel.setPeriodMode(PeriodMode.WEEK);
                    }
                });
        binding.btnCommissionsPrevPeriod.setOnClickListener(v -> viewModel.shiftPeriod(-1));
        binding.btnCommissionsNextPeriod.setOnClickListener(v -> viewModel.shiftPeriod(1));
        binding.btnCommissionsFromDate.setOnClickListener(v -> pickRangeDate(true));
        binding.btnCommissionsToDate.setOnClickListener(v -> pickRangeDate(false));
    }

    private void observeData() {
        viewModel.observeProfessionals().observe(getViewLifecycleOwner(), professionals -> {
            List<ProfessionalWithDetails> safeList = professionals == null
                    ? Collections.emptyList()
                    : professionals;
            renderProfessionalChips(safeList);
            if (safeList.isEmpty()) {
                binding.commissionsEmptyState.setMessage(R.string.commissions_no_professionals);
                binding.commissionsEmptyState.setVisibility(View.VISIBLE);
                binding.commissionsDetailRecycler.setVisibility(View.GONE);
            } else {
                viewModel.ensureDefaultProfessional(safeList);
            }
        });

        viewModel.observeFilter().observe(getViewLifecycleOwner(), filter -> {
            if (filter == null) {
                return;
            }
            syncProfessionalChipChecks(filter.professionalId);
            renderFilterControls(filter);
        });

        viewModel.observeUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                renderUiState(state);
            }
        });
    }

    private void renderProfessionalChips(@NonNull List<ProfessionalWithDetails> professionals) {
        binding.commissionsProfessionalGroup.removeAllViews();
        CommissionsFilter filter = viewModel.currentFilter();
        String selectedId = filter == null ? null : filter.professionalId;

        for (ProfessionalWithDetails item : professionals) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(item.professional.name);
            chip.setTag(item.professional.id);
            chip.setChecked(item.professional.id.equals(selectedId));
            chip.setOnClickListener(v -> viewModel.selectProfessional(item.professional.id));
            binding.commissionsProfessionalGroup.addView(chip);
        }
    }

    private void syncProfessionalChipChecks(@Nullable String selectedProId) {
        int count = binding.commissionsProfessionalGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.commissionsProfessionalGroup.getChildAt(i);
            if (child instanceof Chip) {
                Object tag = child.getTag();
                ((Chip) child).setChecked(selectedProId != null && selectedProId.equals(tag));
            }
        }
    }

    private void renderFilterControls(@NonNull CommissionsFilter filter) {
        int modeButtonId;
        if (filter.mode == PeriodMode.MONTH) {
            modeButtonId = R.id.btnCommissionsModeMonth;
        } else if (filter.mode == PeriodMode.RANGE) {
            modeButtonId = R.id.btnCommissionsModeRange;
        } else {
            modeButtonId = R.id.btnCommissionsModeWeek;
        }
        if (binding.commissionsPeriodModeGroup.getCheckedButtonId() != modeButtonId) {
            binding.commissionsPeriodModeGroup.check(modeButtonId);
        }

        if (filter.mode == PeriodMode.RANGE) {
            binding.commissionsNavBar.setVisibility(View.GONE);
            binding.commissionsCustomRangeBar.setVisibility(View.VISIBLE);
            binding.btnCommissionsFromDate.setText(getString(
                    R.string.commissions_range_from_format,
                    Formats.formatDate(filter.fromDay)));
            binding.btnCommissionsToDate.setText(getString(
                    R.string.commissions_range_to_format,
                    Formats.formatDate(filter.toDay)));
        } else {
            binding.commissionsNavBar.setVisibility(View.VISIBLE);
            binding.commissionsCustomRangeBar.setVisibility(View.GONE);
            if (filter.mode == PeriodMode.MONTH) {
                String raw = LocalDate.ofEpochDay(filter.fromDay).format(MONTH_FORMATTER);
                String capitalized = raw.isEmpty()
                        ? ""
                        : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
                binding.commissionsPeriodLabel.setText(capitalized);
            } else {
                binding.commissionsPeriodLabel.setText(getString(
                        R.string.commissions_period_inline_format,
                        Formats.formatWeekdayAndDayMonth(filter.fromDay),
                        Formats.formatWeekdayAndDayMonth(filter.toDay)));
            }
        }
    }

    private void renderUiState(@NonNull CommissionsUiState state) {
        PeriodCalculation calc = state.calculation;
        long totalCommission = calc == null ? 0L : calc.totalCommissionCents();
        long totalBase = calc == null ? 0L : calc.totalBaseCents();
        binding.commissionsTotalCommission.setMoneyCents(totalCommission);
        binding.commissionsTotalBase.setMoneyCents(totalBase);

        if (calc != null && !calc.pendingCollectionAppointments().isEmpty()) {
            binding.commissionsPendingSummaryRow.setVisibility(View.VISIBLE);
            binding.commissionsPendingSummaryText.setText(getString(
                    R.string.commissions_summary_pending_value_format,
                    Formats.formatMoney(calc.pendingCollectionBaseCents()),
                    Formats.formatMoney(calc.pendingCollectionCommissionCents())));
        } else {
            binding.commissionsPendingSummaryRow.setVisibility(View.GONE);
        }

        boolean empty = state.rows.isEmpty();
        binding.commissionsEmptyState.setMessage(state.filter.professionalId == null
                ? R.string.commissions_no_professionals
                : R.string.commissions_empty);
        binding.commissionsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.commissionsDetailRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        detailsAdapter.submitList(empty ? null : state.rows);
    }

    private void pickRangeDate(boolean pickingFrom) {
        CommissionsFilter filter = viewModel.currentFilter();
        long currentEpoch = filter == null
                ? LocalDate.now().toEpochDay()
                : (pickingFrom ? filter.fromDay : filter.toDay);
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(currentEpoch * 86_400_000L)
                .build();
        picker.addOnPositiveButtonClickListener(selectionMillis -> {
            if (selectionMillis == null) {
                return;
            }
            long chosenDay = Instant.ofEpochMilli(selectionMillis)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate()
                    .toEpochDay();
            CommissionsFilter latest = viewModel.currentFilter();
            if (latest == null) {
                return;
            }
            if (pickingFrom) {
                long nextTo = chosenDay > latest.toDay ? chosenDay : latest.toDay;
                viewModel.setCustomRange(chosenDay, nextTo);
            } else {
                long nextFrom = chosenDay < latest.fromDay ? chosenDay : latest.fromDay;
                viewModel.setCustomRange(nextFrom, chosenDay);
            }
        });
        picker.show(getChildFragmentManager(), pickingFrom ? "comm_from_picker" : "comm_to_picker");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
