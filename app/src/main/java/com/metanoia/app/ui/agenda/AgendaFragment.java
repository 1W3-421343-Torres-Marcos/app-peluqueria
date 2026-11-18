package com.metanoia.app.ui.agenda;

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
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.databinding.FragmentAgendaBinding;
import com.metanoia.app.ui.agenda.AgendaViewModel.AgendaFilter;
import com.metanoia.app.ui.agenda.AgendaViewModel.ViewMode;
import com.metanoia.app.util.Formats;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla principal de la agenda de turnos con navegación diaria y filtro por profesional.
 */
public class AgendaFragment extends Fragment {

    private FragmentAgendaBinding binding;
    private AgendaViewModel viewModel;
    private DayTimelineAdapter dayAdapter;
    private final Map<String, String> professionalNames = new LinkedHashMap<>();
    private List<ScheduleBlockEntity> latestDayBlocks = Collections.emptyList();
    private List<AppointmentCardProjection> latestDayAppointments = Collections.emptyList();

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

        dayAdapter = new DayTimelineAdapter(card -> Toast.makeText(
                requireContext(), R.string.agenda_detail_pending_step, Toast.LENGTH_SHORT).show());
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
        binding.btnAgendaBlocks.setOnClickListener(v -> Toast.makeText(
                requireContext(), R.string.agenda_blocks_pending_step, Toast.LENGTH_SHORT).show());
        binding.fabNewAppointment.setOnClickListener(v -> Toast.makeText(
                requireContext(), R.string.agenda_create_pending_step, Toast.LENGTH_SHORT).show());
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
            renderTimeline();
        });

        viewModel.observeFilter().observe(getViewLifecycleOwner(), filter -> {
            if (filter == null) {
                return;
            }
            renderFilterHeader(filter);
            syncProfessionalChipChecks(filter.professionalId);
            renderTimeline();
        });

        viewModel.observeDayBlocks().observe(getViewLifecycleOwner(), blocks -> {
            latestDayBlocks = blocks == null ? Collections.emptyList() : blocks;
            renderTimeline();
        });

        viewModel.observeDayAppointments().observe(getViewLifecycleOwner(), cards -> {
            latestDayAppointments = cards == null ? Collections.emptyList() : cards;
            renderTimeline();
        });
    }

    private void renderFilterHeader(@NonNull AgendaFilter filter) {
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
            binding.btnAgendaToday.setVisibility(View.VISIBLE);
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

    private void renderTimeline() {
        List<DayTimelineAdapter.TimelineRow> rows = DayTimelineAdapter.buildRows(
                latestDayBlocks,
                latestDayAppointments,
                viewModel.currentProfessionalId(),
                professionalNames
        );
        boolean empty = rows.isEmpty();
        binding.agendaEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.agendaRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            boolean filtered = viewModel.currentProfessionalId() != null;
            binding.agendaEmptyState.setMessage(filtered
                    ? R.string.agenda_empty_filtered
                    : R.string.empty_agenda_title);
            dayAdapter.submitList(null);
        } else {
            dayAdapter.submitList(rows);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
