package com.metanoia.app.ui.agenda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.backup.BackupFolderAccess;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderState;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderStatus;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.FragmentAgendaBinding;
import com.metanoia.app.ui.agenda.AgendaViewModel.AgendaFilter;
import com.metanoia.app.ui.agenda.AgendaViewModel.ViewMode;
import com.metanoia.app.ui.agenda.create.AppointmentCreateFragment;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailFragment;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.rules.BackupRetentionPolicy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla principal de la agenda de turnos con vista diaria/semanal y filtro por profesional.
 */
public class AgendaFragment extends Fragment {

    private FragmentAgendaBinding binding;
    private AgendaViewModel viewModel;
    private DayTimelineAdapter dayAdapter;
    private WeekDaysAdapter weekAdapter;
    private final Map<String, String> professionalNames = new LinkedHashMap<>();
    private List<ScheduleBlockEntity> latestDayBlocks = Collections.emptyList();
    private List<AppointmentCardProjection> latestDayAppointments = Collections.emptyList();
    private List<ScheduleBlockEntity> latestWeekBlocks = Collections.emptyList();
    private List<AppointmentCardProjection> latestWeekAppointments = Collections.emptyList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAgendaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AgendaViewModel.class);

        dayAdapter = new DayTimelineAdapter(card -> openAppointmentDetail(card.id));
        weekAdapter = new WeekDaysAdapter(dayEpoch -> viewModel.selectDayAndOpenDayMode(dayEpoch));

        binding.agendaRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.agendaRecycler.setAdapter(dayAdapter);

        setupControls();
        observeData();
    }

    private void setupControls() {
        binding.agendaViewModeGroup.check(R.id.btnAgendaModeDay);
        binding.agendaViewModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnAgendaModeWeek) {
                viewModel.setViewMode(ViewMode.WEEK);
            } else {
                viewModel.setViewMode(ViewMode.DAY);
            }
        });

        binding.btnAgendaPrevDate.setOnClickListener(v -> viewModel.shiftDate(-1));
        binding.btnAgendaNextDate.setOnClickListener(v -> viewModel.shiftDate(1));
        binding.btnAgendaToday.setOnClickListener(v -> viewModel.jumpToToday());
        binding.btnAgendaReminders.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_agenda_to_reminders));
        binding.btnAgendaBlocks.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_agenda_to_blocks));
        binding.fabNewAppointment.setOnClickListener(v -> openCreateAppointment());
    }

    private void openAppointmentDetail(@NonNull String appointmentId) {
        Bundle args = new Bundle();
        args.putString(AppointmentDetailFragment.ARG_APPOINTMENT_ID, appointmentId);
        NavHostFragment.findNavController(this).navigate(R.id.action_agenda_to_detail, args);
    }

    private void openCreateAppointment() {
        Bundle args = new Bundle();
        args.putLong(AppointmentCreateFragment.ARG_INITIAL_DAY, viewModel.currentSelectedDay());
        String proId = viewModel.currentProfessionalId();
        if (proId != null) {
            args.putString(AppointmentCreateFragment.ARG_INITIAL_PROFESSIONAL_ID, proId);
        }
        NavHostFragment.findNavController(this).navigate(R.id.action_agenda_to_create, args);
    }

    private void observeData() {
        viewModel.observeActiveProfessionals().observe(getViewLifecycleOwner(), professionals -> {
            professionalNames.clear();
            if (professionals != null) {
                for (ProfessionalWithDetails item : professionals) {
                    professionalNames.put(item.professional.id, item.professional.name);
                }
            }
            renderProfessionalChips(professionals);
            renderCurrentMode();
        });

        viewModel.observeFilter().observe(getViewLifecycleOwner(), filter -> {
            if (filter == null) {
                return;
            }
            renderFilterHeader(filter);
            syncProfessionalChipChecks(filter.professionalId);
            renderCurrentMode();
        });

        viewModel.observeDayBlocks().observe(getViewLifecycleOwner(), blocks -> {
            latestDayBlocks = blocks == null ? Collections.emptyList() : blocks;
            renderCurrentMode();
        });

        viewModel.observeDayAppointments().observe(getViewLifecycleOwner(), cards -> {
            latestDayAppointments = cards == null ? Collections.emptyList() : cards;
            renderCurrentMode();
        });

        viewModel.observeWeekBlocks().observe(getViewLifecycleOwner(), blocks -> {
            latestWeekBlocks = blocks == null ? Collections.emptyList() : blocks;
            renderCurrentMode();
        });

        viewModel.observeWeekAppointments().observe(getViewLifecycleOwner(), cards -> {
            latestWeekAppointments = cards == null ? Collections.emptyList() : cards;
            renderCurrentMode();
        });
    }

    private void renderFilterHeader(@NonNull AgendaFilter filter) {
        int desiredButtonId = filter.viewMode == ViewMode.WEEK
                ? R.id.btnAgendaModeWeek
                : R.id.btnAgendaModeDay;
        if (binding.agendaViewModeGroup.getCheckedButtonId() != desiredButtonId) {
            binding.agendaViewModeGroup.check(desiredButtonId);
        }

        if (filter.viewMode == ViewMode.DAY) {
            binding.agendaDateLabel.setText(
                    Formats.formatDayWithShortWeekday(filter.selectedDayEpoch));
            binding.btnAgendaToday.setVisibility(
                    filter.selectedDayEpoch == viewModel.todayEpochDay()
                            ? View.GONE
                            : View.VISIBLE);
        } else {
            long monday = AgendaViewModel.mondayOf(filter.selectedDayEpoch);
            binding.agendaDateLabel.setText(getString(
                    R.string.agenda_time_inline,
                    Formats.formatWeekdayAndDayMonth(monday),
                    Formats.formatWeekdayAndDayMonth(monday + 6L)));
            boolean inCurrentWeek = viewModel.todayEpochDay() >= monday
                    && viewModel.todayEpochDay() <= monday + 6L;
            binding.btnAgendaToday.setVisibility(inCurrentWeek ? View.GONE : View.VISIBLE);
        }
    }

    private void renderProfessionalChips(@Nullable List<ProfessionalWithDetails> professionals) {
        binding.agendaProfessionalFilterGroup.removeAllViews();
        String selectedProId = viewModel.currentProfessionalId();

        Chip allChip = new Chip(requireContext());
        allChip.setId(View.generateViewId());
        allChip.setCheckable(true);
        allChip.setText(R.string.agenda_filter_all_professionals);
        allChip.setTag(null);
        allChip.setChecked(selectedProId == null);
        allChip.setOnClickListener(v -> viewModel.setSelectedProfessional(null));
        binding.agendaProfessionalFilterGroup.addView(allChip);

        if (professionals == null) {
            return;
        }
        for (ProfessionalWithDetails item : professionals) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(item.professional.name);
            chip.setTag(item.professional.id);
            chip.setChecked(item.professional.id.equals(selectedProId));
            chip.setOnClickListener(v -> viewModel.setSelectedProfessional(item.professional.id));
            binding.agendaProfessionalFilterGroup.addView(chip);
        }
    }

    private void syncProfessionalChipChecks(@Nullable String selectedProId) {
        int count = binding.agendaProfessionalFilterGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.agendaProfessionalFilterGroup.getChildAt(i);
            if (child instanceof Chip) {
                Object tag = child.getTag();
                boolean match = (selectedProId == null && tag == null)
                        || (selectedProId != null && selectedProId.equals(tag));
                ((Chip) child).setChecked(match);
            }
        }
    }

    private void renderCurrentMode() {
        AgendaFilter filter = viewModel.observeFilter().getValue();
        if (filter == null) {
            return;
        }
        if (filter.viewMode == ViewMode.WEEK) {
            if (binding.agendaRecycler.getAdapter() != weekAdapter) {
                binding.agendaRecycler.setAdapter(weekAdapter);
            }
            binding.agendaEmptyState.setVisibility(View.GONE);
            binding.agendaRecycler.setVisibility(View.VISIBLE);
            long monday = AgendaViewModel.mondayOf(filter.selectedDayEpoch);
            weekAdapter.submitList(WeekDaysAdapter.buildWeek(
                    monday,
                    filter.selectedDayEpoch,
                    viewModel.todayEpochDay(),
                    latestWeekAppointments,
                    latestWeekBlocks,
                    filter.professionalId
            ));
            return;
        }

        if (binding.agendaRecycler.getAdapter() != dayAdapter) {
            binding.agendaRecycler.setAdapter(dayAdapter);
        }
        List<DayTimelineAdapter.TimelineRow> rows = DayTimelineAdapter.buildRows(
                latestDayBlocks,
                latestDayAppointments,
                filter.professionalId,
                professionalNames
        );
        boolean empty = rows.isEmpty();
        binding.agendaEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.agendaRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            boolean filtered = filter.professionalId != null;
            binding.agendaEmptyState.setMessage(filtered
                    ? R.string.agenda_empty_filtered
                    : R.string.empty_agenda_title);
            dayAdapter.submitList(null);
        } else {
            dayAdapter.submitList(rows);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBackupBanner();
    }

    private void refreshBackupBanner() {
        if (!isAdded() || binding == null) {
            return;
        }
        AppContainer container = MetanoiaApp.container(requireContext());
        android.content.Context appContext = requireContext().getApplicationContext();
        container.executors().io().execute(() -> {
            DevicePrefs prefs = container.devicePrefs();
            FolderStatus folderStatus = BackupFolderAccess.inspect(appContext, prefs);
            long lastSuccess = prefs.getLastBackupSuccessMillis();
            int warningDays = prefs.getOverdueWarningDays();
            boolean validFolder = folderStatus.state == FolderState.READY;
            boolean overdue = BackupRetentionPolicy.isBackupOverdue(
                    validFolder,
                    lastSuccess,
                    container.clock().nowMillis(),
                    warningDays);
            if (!isAdded() || binding == null) {
                return;
            }
            requireActivity().runOnUiThread(() -> applyBackupBannerState(
                    folderStatus.state, lastSuccess, warningDays, overdue));
        });
    }

    private void applyBackupBannerState(
            @NonNull FolderState folderState,
            long lastSuccessMillis,
            int warningDays,
            boolean overdue
    ) {
        if (!isAdded() || binding == null) {
            return;
        }
        if (folderState == FolderState.NOT_CONFIGURED) {
            binding.agendaBackupBanner.setVisibility(View.VISIBLE);
            binding.agendaBackupBanner.setMessage(R.string.backup_overdue_no_folder);
            binding.agendaBackupBanner.setAction(
                    R.string.backup_banner_action_configure,
                    v -> NavHostFragment.findNavController(this)
                            .navigate(R.id.action_agenda_to_backup));
            return;
        }
        if (folderState == FolderState.PERMISSION_LOST) {
            binding.agendaBackupBanner.setVisibility(View.VISIBLE);
            binding.agendaBackupBanner.setMessage(R.string.backup_overdue_lost_folder);
            binding.agendaBackupBanner.setAction(
                    R.string.backup_banner_action_configure,
                    v -> NavHostFragment.findNavController(this)
                            .navigate(R.id.action_agenda_to_backup));
            return;
        }
        if (overdue) {
            binding.agendaBackupBanner.setVisibility(View.VISIBLE);
            if (lastSuccessMillis <= 0L) {
                binding.agendaBackupBanner.setMessage(R.string.backup_overdue_never_run);
            } else {
                binding.agendaBackupBanner.setMessage(
                        getString(R.string.backup_overdue_days_format, warningDays));
            }
            binding.agendaBackupBanner.setAction(
                    R.string.backup_banner_action_now,
                    v -> NavHostFragment.findNavController(this)
                            .navigate(R.id.action_agenda_to_backup));
            return;
        }
        binding.agendaBackupBanner.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
