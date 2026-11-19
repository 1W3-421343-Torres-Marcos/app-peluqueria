package com.metanoia.app.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.FragmentReportsBinding;
import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.PeriodMode;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsUiState;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ReportType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Pantalla de Reportes en Más: selector de los 6 reportes, filtros de período y profesional,
 * totales KPI, resumen de menor demanda y tabla detallada.
 */
public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel viewModel;
    private ReportRowsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        adapter = new ReportRowsAdapter();
        binding.reportsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reportsRecycler.setAdapter(adapter);

        setupControls();
        observeData();
    }

    private void setupControls() {
        binding.reportTypeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                viewModel.selectReportType(resolveReportType(checkedIds.get(0)));
            }
        });
        binding.reportsPeriodModeGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (isChecked) {
                        viewModel.setPeriodMode(resolvePeriodMode(checkedId));
                    }
                });
        binding.btnReportsPrevPeriod.setOnClickListener(v -> viewModel.shiftPeriod(-1));
        binding.btnReportsNextPeriod.setOnClickListener(v -> viewModel.shiftPeriod(1));
        binding.btnReportsFromDate.setOnClickListener(v -> pickRangeDate(true));
        binding.btnReportsToDate.setOnClickListener(v -> pickRangeDate(false));
        binding.reportsSubModeGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (isChecked) {
                        viewModel.selectSubMode(checkedId == R.id.btnReportsSubModeB);
                    }
                });
        binding.switchIncludeClientPhone.setOnCheckedChangeListener(
                (btn, checked) -> viewModel.setIncludeClientPhone(checked));
    }

    private void observeData() {
        viewModel.observeProfessionals().observe(getViewLifecycleOwner(), professionals -> {
            List<ProfessionalWithDetails> safe = professionals == null
                    ? Collections.emptyList()
                    : professionals;
            renderProfessionalChips(safe);
        });
        viewModel.observeFilter().observe(getViewLifecycleOwner(), filter -> {
            if (filter != null) {
                renderFilterControls(filter);
            }
        });
        viewModel.observeUiState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                renderUiState(state);
            }
        });
        viewModel.observeMessage().observe(getViewLifecycleOwner(), event -> {
            Integer msgRes = event == null ? null : event.getContentIfNotHandled();
            if (msgRes != null) {
                Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @NonNull
    private ReportType resolveReportType(int checkedId) {
        if (checkedId == R.id.chipReportTopServices) {
            return ReportType.TOP_SERVICES;
        }
        if (checkedId == R.id.chipReportFrequentClients) {
            return ReportType.FREQUENT_CLIENTS;
        }
        if (checkedId == R.id.chipReportOccupancy) {
            return ReportType.OCCUPANCY;
        }
        if (checkedId == R.id.chipReportPerformance) {
            return ReportType.PROFESSIONAL_PERFORMANCE;
        }
        if (checkedId == R.id.chipReportReceivables) {
            return ReportType.RECEIVABLES;
        }
        return ReportType.INCOME;
    }

    @NonNull
    private PeriodMode resolvePeriodMode(int checkedId) {
        if (checkedId == R.id.btnReportsModeToday) {
            return PeriodMode.TODAY;
        }
        if (checkedId == R.id.btnReportsModeWeek) {
            return PeriodMode.WEEK;
        }
        if (checkedId == R.id.btnReportsModeRange) {
            return PeriodMode.RANGE;
        }
        return PeriodMode.MONTH;
    }

    private void renderProfessionalChips(@NonNull List<ProfessionalWithDetails> professionals) {
        binding.reportsProfessionalGroup.removeAllViews();
        ReportsFilterState filter = viewModel.currentFilter();
        String selectedId = filter == null ? null : filter.professionalId;

        Chip allChip = createProfessionalChip(getString(R.string.reports_professional_all), null);
        allChip.setChecked(selectedId == null);
        binding.reportsProfessionalGroup.addView(allChip);

        for (ProfessionalWithDetails item : professionals) {
            Chip chip = createProfessionalChip(item.professional.name, item.professional.id);
            chip.setChecked(item.professional.id.equals(selectedId));
            binding.reportsProfessionalGroup.addView(chip);
        }
    }

    @NonNull
    private Chip createProfessionalChip(@NonNull String label, @Nullable String tagProId) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setCheckable(true);
        chip.setText(label);
        chip.setTag(tagProId);
        chip.setOnClickListener(v -> viewModel.selectProfessional(tagProId));
        return chip;
    }

    private void syncProfessionalChips(@Nullable String selectedProId) {
        int count = binding.reportsProfessionalGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.reportsProfessionalGroup.getChildAt(i);
            if (child instanceof Chip) {
                Object tag = child.getTag();
                boolean match = selectedProId == null
                        ? tag == null
                        : selectedProId.equals(tag);
                ((Chip) child).setChecked(match);
            }
        }
    }

    private void renderFilterControls(@NonNull ReportsFilterState filter) {
        syncReportTypeChip(filter.reportType);
        syncPeriodControls(filter);
        syncProfessionalChips(filter.professionalId);
        renderSubFilterControls(filter);
    }

    private void syncReportTypeChip(@NonNull ReportType type) {
        int chipId;
        switch (type) {
            case TOP_SERVICES:
                chipId = R.id.chipReportTopServices;
                break;
            case FREQUENT_CLIENTS:
                chipId = R.id.chipReportFrequentClients;
                break;
            case OCCUPANCY:
                chipId = R.id.chipReportOccupancy;
                break;
            case PROFESSIONAL_PERFORMANCE:
                chipId = R.id.chipReportPerformance;
                break;
            case RECEIVABLES:
                chipId = R.id.chipReportReceivables;
                break;
            case INCOME:
            default:
                chipId = R.id.chipReportIncome;
                break;
        }
        if (binding.reportTypeChipGroup.getCheckedChipId() != chipId) {
            binding.reportTypeChipGroup.check(chipId);
        }
    }

    private void syncPeriodControls(@NonNull ReportsFilterState filter) {
        int modeBtn;
        if (filter.periodMode == PeriodMode.TODAY) {
            modeBtn = R.id.btnReportsModeToday;
        } else if (filter.periodMode == PeriodMode.WEEK) {
            modeBtn = R.id.btnReportsModeWeek;
        } else if (filter.periodMode == PeriodMode.RANGE) {
            modeBtn = R.id.btnReportsModeRange;
        } else {
            modeBtn = R.id.btnReportsModeMonth;
        }
        if (binding.reportsPeriodModeGroup.getCheckedButtonId() != modeBtn) {
            binding.reportsPeriodModeGroup.check(modeBtn);
        }

        if (filter.periodMode == PeriodMode.RANGE) {
            binding.reportsNavBar.setVisibility(View.GONE);
            binding.reportsCustomRangeBar.setVisibility(View.VISIBLE);
            binding.btnReportsFromDate.setText(getString(
                    R.string.commissions_range_from_format, Formats.formatDate(filter.fromDay)));
            binding.btnReportsToDate.setText(getString(
                    R.string.commissions_range_to_format, Formats.formatDate(filter.toDay)));
        } else {
            binding.reportsNavBar.setVisibility(View.VISIBLE);
            binding.reportsCustomRangeBar.setVisibility(View.GONE);
            if (filter.periodMode == PeriodMode.TODAY) {
                binding.reportsPeriodLabel.setText(
                        Formats.formatDayWithShortWeekday(filter.fromDay));
            } else if (filter.periodMode == PeriodMode.MONTH) {
                LocalDate d = LocalDate.ofEpochDay(filter.fromDay);
                binding.reportsPeriodLabel.setText(
                        ReportPresentationMapper.formatMonthYear(d.getYear(), d.getMonthValue()));
            } else {
                binding.reportsPeriodLabel.setText(getString(
                        R.string.commissions_period_inline_format,
                        Formats.formatWeekdayAndDayMonth(filter.fromDay),
                        Formats.formatWeekdayAndDayMonth(filter.toDay)));
            }
        }
    }

    private void renderSubFilterControls(@NonNull ReportsFilterState filter) {
        int labelA = 0;
        int labelB = 0;
        boolean selectB = false;
        switch (filter.reportType) {
            case INCOME:
                labelA = R.string.reports_sub_income_day;
                labelB = R.string.reports_sub_income_month;
                selectB = filter.incomeGranularity == IncomeGranularity.MONTH;
                break;
            case TOP_SERVICES:
                labelA = R.string.reports_sub_services_item;
                labelB = R.string.reports_sub_services_category;
                selectB = filter.serviceGrouping == ServiceGrouping.CATEGORY;
                break;
            case FREQUENT_CLIENTS:
                labelA = R.string.reports_sub_clients_visits;
                labelB = R.string.reports_sub_clients_spend;
                selectB = filter.clientRankingMode == ClientRankingMode.SPEND;
                break;
            case OCCUPANCY:
                labelA = R.string.reports_sub_occupancy_hour;
                labelB = R.string.reports_sub_occupancy_day;
                selectB = filter.occupancyGrouping == OccupancyGrouping.DAY;
                break;
            default:
                break;
        }
        if (labelA == 0) {
            binding.reportsSubModeGroup.setVisibility(View.GONE);
        } else {
            binding.reportsSubModeGroup.setVisibility(View.VISIBLE);
            binding.btnReportsSubModeA.setText(labelA);
            binding.btnReportsSubModeB.setText(labelB);
            int targetId = selectB ? R.id.btnReportsSubModeB : R.id.btnReportsSubModeA;
            if (binding.reportsSubModeGroup.getCheckedButtonId() != targetId) {
                binding.reportsSubModeGroup.check(targetId);
            }
        }

        boolean showPhoneSwitch = filter.reportType == ReportType.FREQUENT_CLIENTS;
        binding.switchIncludeClientPhone.setVisibility(showPhoneSwitch ? View.VISIBLE : View.GONE);
        if (binding.switchIncludeClientPhone.isChecked() != filter.includeClientPhone) {
            binding.switchIncludeClientPhone.setChecked(filter.includeClientPhone);
        }
    }

    private void renderUiState(@NonNull ReportsUiState state) {
        binding.reportsKpiPrimaryLabel.setText(state.kpiPrimaryLabel);
        binding.reportsKpiPrimaryValue.setText(state.kpiPrimaryValue);
        binding.reportsKpiSecondaryLabel.setText(state.kpiSecondaryLabel);
        binding.reportsKpiSecondaryValue.setText(state.kpiSecondaryValue);

        if (state.secondarySectionTitle != null && !state.secondaryLines.isEmpty()) {
            binding.reportsSecondaryCard.setVisibility(View.VISIBLE);
            binding.reportsSecondaryTitle.setText(state.secondarySectionTitle);
            binding.reportsSecondaryBody.setText(String.join("\n", state.secondaryLines));
        } else {
            binding.reportsSecondaryCard.setVisibility(View.GONE);
        }

        if (!state.chartEntries.isEmpty()) {
            binding.reportsChartCard.setVisibility(View.VISIBLE);
            binding.reportsChartTitle.setText(state.chartTitle);
            binding.reportsBarChart.setData(state.chartEntries, state.chartMaxValueOverride);
        } else {
            binding.reportsChartCard.setVisibility(View.GONE);
        }

        boolean empty = state.rows.isEmpty();
        binding.reportsEmptyState.setMessage(state.emptyMessageRes);
        binding.reportsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.reportsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        adapter.submitList(empty ? null : state.rows);
    }

    private void pickRangeDate(boolean pickingFrom) {
        ReportsFilterState filter = viewModel.currentFilter();
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
            ReportsFilterState latest = viewModel.currentFilter();
            if (latest == null) {
                return;
            }
            if (pickingFrom) {
                viewModel.setCustomRange(chosenDay, Math.max(chosenDay, latest.toDay));
            } else {
                viewModel.setCustomRange(Math.min(chosenDay, latest.fromDay), chosenDay);
            }
        });
        picker.show(getChildFragmentManager(),
                pickingFrom ? "reports_from_picker" : "reports_to_picker");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
