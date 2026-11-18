package com.metanoia.app.ui.agenda.detail;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.DialogRescheduleAppointmentBinding;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader.SlotLoadResult;
import com.metanoia.app.util.Formats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bottom sheet para reprogramar fecha, profesional habilitado y horario libre de un turno.
 */
public class RescheduleDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_APPOINTMENT_ID = "appointment_id";
    private static final String ARG_PRO_ID = "pro_id";
    private static final String ARG_DAY = "day";
    private static final String ARG_START_MIN = "start_min";
    private static final String ARG_DURATION_MIN = "duration_min";
    private static final String ARG_SERVICE_IDS = "service_ids";

    private DialogRescheduleAppointmentBinding binding;
    private AppointmentDetailViewModel viewModel;
    private String appointmentId;
    private String selectedProId;
    private long selectedDayEpoch;
    @Nullable
    private Integer selectedStartMin;
    private int durationMin;
    private List<String> requiredServiceIds = Collections.emptyList();

    public static RescheduleDialogFragment newInstance(
            @NonNull String appointmentId,
            @NonNull String proId,
            long dayEpoch,
            int startMin,
            int durationMin,
            @NonNull ArrayList<String> serviceIds
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        args.putString(ARG_PRO_ID, proId);
        args.putLong(ARG_DAY, dayEpoch);
        args.putInt(ARG_START_MIN, startMin);
        args.putInt(ARG_DURATION_MIN, durationMin);
        args.putStringArrayList(ARG_SERVICE_IDS, serviceIds);
        RescheduleDialogFragment fragment = new RescheduleDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogRescheduleAppointmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        appointmentId = args.getString(ARG_APPOINTMENT_ID, "");
        selectedProId = args.getString(ARG_PRO_ID, "");
        selectedDayEpoch = args.getLong(ARG_DAY);
        selectedStartMin = args.getInt(ARG_START_MIN);
        durationMin = args.getInt(ARG_DURATION_MIN, 30);
        ArrayList<String> svcList = args.getStringArrayList(ARG_SERVICE_IDS);
        requiredServiceIds = svcList == null ? Collections.emptyList() : svcList;

        viewModel = new ViewModelProvider(requireParentFragment())
                .get(AppointmentDetailViewModel.class);

        binding.rescheduleDateText.setText(Formats.formatDayWithShortWeekday(selectedDayEpoch));
        binding.btnRescheduleChangeDate.setOnClickListener(v -> openDatePicker());
        binding.rescheduleSlotGrid.setOnSlotSelectedListener(startMin -> {
            selectedStartMin = startMin;
            SlotLoadResult current = viewModel.observeRescheduleSlots().getValue();
            if (current != null) {
                binding.rescheduleSlotGrid.setSlots(current.options, selectedStartMin);
            }
        });
        binding.btnConfirmReschedule.setOnClickListener(v -> viewModel.reschedule(
                appointmentId, selectedProId, selectedDayEpoch, selectedStartMin));

        viewModel.observeActiveProfessionals().observe(getViewLifecycleOwner(), this::renderPros);
        viewModel.observeRescheduleSlots().observe(getViewLifecycleOwner(), this::renderSlots);
        viewModel.observeCloseReschedule().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                dismiss();
            }
        });

        reloadSlots();
    }

    private void renderPros(@Nullable List<ProfessionalWithDetails> professionals) {
        binding.rescheduleProGroup.removeAllViews();
        if (professionals == null) {
            return;
        }
        for (ProfessionalWithDetails pro : professionals) {
            if (!supportsRequired(pro)) {
                continue;
            }
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(pro.professional.name);
            chip.setChecked(pro.professional.id.equals(selectedProId));
            chip.setOnClickListener(v -> {
                selectedProId = pro.professional.id;
                selectedStartMin = null;
                reloadSlots();
            });
            binding.rescheduleProGroup.addView(chip);
        }
    }

    private boolean supportsRequired(@NonNull ProfessionalWithDetails pro) {
        Set<String> ids = new LinkedHashSet<>();
        if (pro.serviceRefs != null) {
            for (ProfessionalServiceCrossRef ref : pro.serviceRefs) {
                ids.add(ref.serviceId);
            }
        }
        return ids.containsAll(requiredServiceIds);
    }

    private void renderSlots(@Nullable SlotLoadResult result) {
        if (result == null) {
            binding.rescheduleSlotGrid.setSlots(Collections.emptyList(), null);
            binding.txtRescheduleNoSlots.setVisibility(View.VISIBLE);
            selectedStartMin = null;
            return;
        }
        boolean empty = result.freeStartMinutes.isEmpty();
        binding.txtRescheduleNoSlots.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            selectedStartMin = null;
        } else if (selectedStartMin == null || !result.freeStartMinutes.contains(selectedStartMin)) {
            selectedStartMin = result.freeStartMinutes.get(0);
        }
        binding.rescheduleSlotGrid.setSlots(result.options, selectedStartMin);
    }

    private void openDatePicker() {
        LocalDate initial = LocalDate.ofEpochDay(selectedDayEpoch);
        new DatePickerDialog(
                requireContext(),
                (picker, year, monthZero, day) -> {
                    selectedDayEpoch = LocalDate.of(year, monthZero + 1, day).toEpochDay();
                    selectedStartMin = null;
                    binding.rescheduleDateText.setText(
                            Formats.formatDayWithShortWeekday(selectedDayEpoch));
                    reloadSlots();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        ).show();
    }

    private void reloadSlots() {
        if (selectedProId != null && !selectedProId.isEmpty()) {
            viewModel.loadRescheduleSlots(
                    selectedProId, selectedDayEpoch, durationMin, appointmentId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
