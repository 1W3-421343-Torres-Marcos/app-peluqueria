package com.metanoia.app.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsUiState;
import com.metanoia.app.ui.reports.ReportsViewModel.ShareReportFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Pantalla de Reportes en Más: selector de los 6 reportes, filtros de período y profesional,
 * totales KPI, gráfico monocromático, tabla y exportación explícita.
 */
public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel viewModel;
    private ReportRowsAdapter adapter;

    private final ActivityResultLauncher<String> exportCsvLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/csv"),
                    uri -> {
                        if (uri != null) {
                            viewModel.exportCsvToUri(uri);
                        }
                    });

    private final ActivityResultLauncher<String> exportPdfLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/pdf"),
                    uri -> {
                        if (uri != null) {
                            viewModel.exportPdfToUri(uri);
                        }
                    });

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
                viewModel.selectReportType(
                        ReportFilterUiHelper.resolveReportType(checkedIds.get(0)));
            }
        });
        binding.reportsPeriodModeGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (isChecked) {
                        viewModel.setPeriodMode(
                                ReportFilterUiHelper.resolvePeriodMode(checkedId));
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
        binding.btnExportCsv.setOnClickListener(v -> onExportCsvClicked());
        binding.btnExportPdf.setOnClickListener(v -> onExportPdfClicked());
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
                ReportFilterUiHelper.renderFilterControls(requireContext(), binding, filter);
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
        viewModel.observeShareFile().observe(getViewLifecycleOwner(), event -> {
            ShareReportFile share = event == null ? null : event.getContentIfNotHandled();
            if (share != null) {
                ReportExportUiHelper.shareGeneratedFile(
                        requireContext(), share.file, share.mimeType, share.formatLabel);
            }
        });
    }

    private void onExportCsvClicked() {
        if (!viewModel.hasLoadedReport()) {
            Toast.makeText(requireContext(), R.string.reports_export_not_ready, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        ReportExportUiHelper.showSaveOrShareDialog(
                requireContext(),
                "CSV",
                () -> exportCsvLauncher.launch(viewModel.suggestCsvFileName()),
                () -> viewModel.prepareShareCsv()
        );
    }

    private void onExportPdfClicked() {
        if (!viewModel.hasLoadedReport()) {
            Toast.makeText(requireContext(), R.string.reports_export_not_ready, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        ReportExportUiHelper.showSaveOrShareDialog(
                requireContext(),
                "PDF",
                () -> exportPdfLauncher.launch(viewModel.suggestPdfFileName()),
                () -> viewModel.prepareSharePdf()
        );
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
