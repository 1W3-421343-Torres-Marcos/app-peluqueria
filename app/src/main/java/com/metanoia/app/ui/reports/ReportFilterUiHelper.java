package com.metanoia.app.ui.reports;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;
import com.metanoia.app.R;
import com.metanoia.app.databinding.FragmentReportsBinding;
import com.metanoia.app.ui.reports.ReportsUiModels.ClientRankingMode;
import com.metanoia.app.ui.reports.ReportsUiModels.IncomeGranularity;
import com.metanoia.app.ui.reports.ReportsUiModels.OccupancyGrouping;
import com.metanoia.app.ui.reports.ReportsUiModels.PeriodMode;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.ui.reports.ReportsUiModels.ServiceGrouping;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ReportType;

import java.time.LocalDate;

/**
 * Helper de sincronización de controles de filtro en {@link ReportsFragment}.
 */
final class ReportFilterUiHelper {

    private ReportFilterUiHelper() {
    }

    @NonNull
    static ReportType resolveReportType(int checkedId) {
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
    static PeriodMode resolvePeriodMode(int checkedId) {
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

    static void renderFilterControls(
            @NonNull Context context,
            @NonNull FragmentReportsBinding binding,
            @NonNull ReportsFilterState filter
    ) {
        syncReportTypeChip(binding, filter.reportType);
        syncPeriodControls(context, binding, filter);
        syncProfessionalChips(binding, filter.professionalId);
        renderSubFilterControls(binding, filter);
    }

    static void syncProfessionalChips(
            @NonNull FragmentReportsBinding binding,
            @Nullable String selectedProId
    ) {
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

    private static void syncReportTypeChip(
            @NonNull FragmentReportsBinding binding,
            @NonNull ReportType type
    ) {
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

    private static void syncPeriodControls(
            @NonNull Context context,
            @NonNull FragmentReportsBinding binding,
            @NonNull ReportsFilterState filter
    ) {
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
            binding.btnReportsFromDate.setText(context.getString(
                    R.string.commissions_range_from_format, Formats.formatDate(filter.fromDay)));
            binding.btnReportsToDate.setText(context.getString(
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
                binding.reportsPeriodLabel.setText(context.getString(
                        R.string.commissions_period_inline_format,
                        Formats.formatWeekdayAndDayMonth(filter.fromDay),
                        Formats.formatWeekdayAndDayMonth(filter.toDay)));
            }
        }
    }

    private static void renderSubFilterControls(
            @NonNull FragmentReportsBinding binding,
            @NonNull ReportsFilterState filter
    ) {
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
}
