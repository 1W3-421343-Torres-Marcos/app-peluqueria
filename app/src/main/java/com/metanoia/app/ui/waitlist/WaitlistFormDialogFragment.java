package com.metanoia.app.ui.waitlist;

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
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.databinding.DialogWaitlistFormBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.rules.PhoneNormalizer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Bottom sheet reutilizable para alta y edición rápida de solicitudes en lista de espera.
 */
public class WaitlistFormDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ENTRY_ID = "entry_id";
    private static final String ARG_PREFILL_CLIENT_ID = "prefill_client_id";
    private static final String ARG_PREFILL_CLIENT_NAME = "prefill_client_name";
    private static final String ARG_PREFILL_SERVICE_IDS = "prefill_service_ids";
    private static final String ARG_PREFILL_PRO_ID = "prefill_pro_id";
    private static final String ARG_PREFILL_SPECIFIC_DAY = "prefill_specific_day";
    private static final int DEFAULT_FROM_MIN = 9 * 60;
    private static final int DEFAULT_TO_MIN = 20 * 60;

    private DialogWaitlistFormBinding binding;
    private WaitlistViewModel viewModel;

    @Nullable
    private String editingEntryId;
    @Nullable
    private String selectedClientId;
    @Nullable
    private String selectedClientName;
    @Nullable
    private String selectedProfessionalId;
    private boolean clientLocked;
    private boolean specificDayMode = true;
    private long selectedSpecificDay;
    private long validUntilDay;
    private boolean validUntilCustomized;
    private boolean entryPopulated;
    private int fromMin = DEFAULT_FROM_MIN;
    private int toMin = DEFAULT_TO_MIN;

    private final List<String> selectedServiceIds = new ArrayList<>();
    private final Set<Integer> selectedWeekdays = new TreeSet<>();
    private List<SalonServiceEntity> allServices = Collections.emptyList();
    private List<ProfessionalWithDetails> allProfessionals = Collections.emptyList();

    public static WaitlistFormDialogFragment newCreateInstance() {
        return new WaitlistFormDialogFragment();
    }

    public static WaitlistFormDialogFragment newCreatePrefilledInstance(
            @Nullable String clientId,
            @Nullable String clientName,
            @Nullable ArrayList<String> serviceIds,
            @Nullable String professionalId,
            @Nullable Long specificDay
    ) {
        Bundle args = new Bundle();
        if (clientId != null && !clientId.trim().isEmpty()) {
            args.putString(ARG_PREFILL_CLIENT_ID, clientId.trim());
        }
        if (clientName != null) {
            args.putString(ARG_PREFILL_CLIENT_NAME, clientName);
        }
        if (serviceIds != null && !serviceIds.isEmpty()) {
            args.putStringArrayList(ARG_PREFILL_SERVICE_IDS, serviceIds);
        }
        if (professionalId != null && !professionalId.trim().isEmpty()) {
            args.putString(ARG_PREFILL_PRO_ID, professionalId.trim());
        }
        if (specificDay != null) {
            args.putLong(ARG_PREFILL_SPECIFIC_DAY, specificDay);
        }
        WaitlistFormDialogFragment fragment = new WaitlistFormDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static WaitlistFormDialogFragment newEditInstance(@NonNull String entryId) {
        Bundle args = new Bundle();
        args.putString(ARG_ENTRY_ID, entryId);
        WaitlistFormDialogFragment fragment = new WaitlistFormDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogWaitlistFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WaitlistViewModel.class);

        long today = viewModel.todayEpochDay();
        selectedSpecificDay = today + 1;
        validUntilDay = today + Settings.DEFAULT_WAITLIST_EXPIRY_DAYS;

        readInitialArguments(today);
        setupListeners();
        observeViewModel();

        if (editingEntryId != null && !editingEntryId.isEmpty()) {
            viewModel.loadEntry(editingEntryId);
        } else {
            refreshClientDisplay();
            refreshDateTimeControls();
        }
    }

    private void readInitialArguments(long today) {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        editingEntryId = args.getString(ARG_ENTRY_ID);
        String prefillClientId = args.getString(ARG_PREFILL_CLIENT_ID);
        if (prefillClientId != null && !prefillClientId.isEmpty()) {
            selectedClientId = prefillClientId;
            selectedClientName = args.getString(ARG_PREFILL_CLIENT_NAME, "");
            clientLocked = true;
        }
        ArrayList<String> prefillServices = args.getStringArrayList(ARG_PREFILL_SERVICE_IDS);
        if (prefillServices != null) {
            selectedServiceIds.addAll(prefillServices);
        }
        selectedProfessionalId = args.getString(ARG_PREFILL_PRO_ID);
        if (args.containsKey(ARG_PREFILL_SPECIFIC_DAY)) {
            selectedSpecificDay = Math.max(today, args.getLong(ARG_PREFILL_SPECIFIC_DAY));
            specificDayMode = true;
        }
    }

    private void setupListeners() {
        boolean isEdit = editingEntryId != null && !editingEntryId.isEmpty();
        binding.dialogWaitlistTitle.setText(isEdit
                ? R.string.waitlist_form_edit_title
                : R.string.waitlist_form_new_title);

        binding.waitlistClientSearchField.setOnQueryChangeListener(viewModel::setClientQuery);
        binding.waitlistDayModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            specificDayMode = checkedId == R.id.btnWaitlistModeSpecificDay;
            refreshDayModeVisibility();
        });
        binding.btnWaitlistPickSpecificDay.setOnClickListener(
                v -> openDatePicker(selectedSpecificDay, picked -> {
                    selectedSpecificDay = picked;
                    refreshDateTimeControls();
                }));
        binding.btnWaitlistPickValidUntil.setOnClickListener(
                v -> openDatePicker(validUntilDay, picked -> {
                    validUntilDay = picked;
                    validUntilCustomized = true;
                    refreshDateTimeControls();
                }));
        binding.btnWaitlistFromTime.setOnClickListener(v -> openTimePicker(fromMin, picked -> {
            fromMin = picked;
            refreshDateTimeControls();
        }));
        binding.btnWaitlistToTime.setOnClickListener(v -> openTimePicker(toMin, picked -> {
            toMin = picked;
            refreshDateTimeControls();
        }));
        bindWeekdayChip(binding.chipWeekdayMon, 1);
        bindWeekdayChip(binding.chipWeekdayTue, 2);
        bindWeekdayChip(binding.chipWeekdayWed, 3);
        bindWeekdayChip(binding.chipWeekdayThu, 4);
        bindWeekdayChip(binding.chipWeekdayFri, 5);
        bindWeekdayChip(binding.chipWeekdaySat, 6);
        bindWeekdayChip(binding.chipWeekdaySun, 7);

        binding.btnSaveWaitlist.setOnClickListener(v -> submitForm());
        binding.btnCancelWaitlistForm.setOnClickListener(v -> dismiss());
    }

    private void bindWeekdayChip(@NonNull Chip chip, int isoDay) {
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedWeekdays.add(isoDay);
            } else {
                selectedWeekdays.remove(isoDay);
            }
        });
    }

    private void observeViewModel() {
        viewModel.observeClientSearchResults().observe(
                getViewLifecycleOwner(), this::renderClientChips);
        viewModel.observeServices().observe(getViewLifecycleOwner(), list -> {
            allServices = list == null ? Collections.emptyList() : list;
            renderServiceChips();
            renderProfessionalChips();
        });
        viewModel.observeProfessionals().observe(getViewLifecycleOwner(), list -> {
            allProfessionals = list == null ? Collections.emptyList() : list;
            renderProfessionalChips();
        });
        viewModel.observeWaitlistExpiryDays().observe(getViewLifecycleOwner(), days -> {
            if (!validUntilCustomized && editingEntryId == null && days != null && days > 0) {
                validUntilDay = viewModel.todayEpochDay() + days;
                refreshDateTimeControls();
            }
        });
        viewModel.observeLoadedEntry().observe(getViewLifecycleOwner(), state -> {
            if (state != null && editingEntryId != null
                    && editingEntryId.equals(state.entry.id()) && !entryPopulated) {
                entryPopulated = true;
                populateExistingEntry(state.entry, state.clientName);
            }
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeSavedEntry().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                dismiss();
            }
        });
    }

    private void populateExistingEntry(@NonNull WaitlistEntry entry, @NonNull String clientName) {
        clientLocked = true;
        selectedClientId = entry.clientId();
        selectedClientName = clientName;
        selectedServiceIds.clear();
        selectedServiceIds.addAll(entry.serviceIds());
        selectedProfessionalId = entry.professionalId().orElse(null);

        if (entry.specificDay().isPresent()) {
            specificDayMode = true;
            selectedSpecificDay = entry.specificDay().get();
        } else {
            specificDayMode = false;
            selectedWeekdays.clear();
            selectedWeekdays.addAll(entry.preferredDaysOfWeek());
        }
        fromMin = entry.fromMin();
        toMin = entry.toMin();
        validUntilDay = entry.validUntilDay();
        validUntilCustomized = true;
        binding.inputWaitlistNotes.setText(entry.notes());

        refreshClientDisplay();
        renderServiceChips();
        renderProfessionalChips();
        syncWeekdayChips();
        refreshDateTimeControls();
    }

    private void refreshClientDisplay() {
        binding.waitlistClientSearchSection.setVisibility(
                clientLocked ? View.GONE : View.VISIBLE);
        if (selectedClientId != null && selectedClientName != null
                && !selectedClientName.isEmpty()) {
            binding.txtWaitlistSelectedClient.setVisibility(View.VISIBLE);
            binding.txtWaitlistSelectedClient.setText(getString(
                    R.string.waitlist_client_selected_format, selectedClientName));
        } else {
            binding.txtWaitlistSelectedClient.setVisibility(View.GONE);
        }
    }

    private void renderClientChips(@Nullable List<ClientRowProjection> clients) {
        binding.waitlistClientChips.removeAllViews();
        if (clients == null || clientLocked) {
            return;
        }
        for (ClientRowProjection client : clients) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(getString(
                    R.string.appointment_client_option_format,
                    client.name,
                    PhoneNormalizer.formatForDisplay(client.phone)));
            chip.setChecked(client.id.equals(selectedClientId));
            chip.setOnClickListener(v -> {
                selectedClientId = client.id;
                selectedClientName = client.name;
                refreshClientDisplay();
            });
            binding.waitlistClientChips.addView(chip);
        }
    }

    private void renderServiceChips() {
        binding.waitlistServiceChips.removeAllViews();
        for (SalonServiceEntity svc : allServices) {
            boolean alreadySelected = selectedServiceIds.contains(svc.id);
            if (!svc.active && !alreadySelected) {
                continue;
            }
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(svc.name);
            chip.setChecked(alreadySelected);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedServiceIds.contains(svc.id)) {
                        selectedServiceIds.add(svc.id);
                    }
                } else {
                    selectedServiceIds.remove(svc.id);
                }
                renderProfessionalChips();
            });
            binding.waitlistServiceChips.addView(chip);
        }
    }

    private void renderProfessionalChips() {
        binding.waitlistProfessionalChips.removeAllViews();
        List<ProfessionalWithDetails> compatible = new ArrayList<>();
        boolean selectedStillCompatible = selectedProfessionalId == null;
        for (ProfessionalWithDetails pro : allProfessionals) {
            boolean matchesServices = supportsSelectedServices(pro);
            boolean isCurrentlySelected = pro.professional.id.equals(selectedProfessionalId);
            if ((pro.professional.active && matchesServices) || isCurrentlySelected) {
                compatible.add(pro);
                if (isCurrentlySelected) {
                    selectedStillCompatible = true;
                }
            }
        }
        if (!selectedStillCompatible) {
            selectedProfessionalId = null;
        }

        Chip anyChip = new Chip(requireContext());
        anyChip.setId(View.generateViewId());
        anyChip.setCheckable(true);
        anyChip.setText(R.string.waitlist_any_professional);
        anyChip.setChecked(selectedProfessionalId == null);
        anyChip.setOnClickListener(v -> selectedProfessionalId = null);
        binding.waitlistProfessionalChips.addView(anyChip);

        for (ProfessionalWithDetails pro : compatible) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(pro.professional.name);
            chip.setChecked(pro.professional.id.equals(selectedProfessionalId));
            chip.setOnClickListener(v -> selectedProfessionalId = pro.professional.id);
            binding.waitlistProfessionalChips.addView(chip);
        }
    }

    private boolean supportsSelectedServices(@NonNull ProfessionalWithDetails pro) {
        if (selectedServiceIds.isEmpty()) {
            return true;
        }
        Set<String> supported = new LinkedHashSet<>();
        if (pro.serviceRefs != null) {
            for (ProfessionalServiceCrossRef ref : pro.serviceRefs) {
                supported.add(ref.serviceId);
            }
        }
        return supported.containsAll(selectedServiceIds);
    }

    private void syncWeekdayChips() {
        binding.chipWeekdayMon.setChecked(selectedWeekdays.contains(1));
        binding.chipWeekdayTue.setChecked(selectedWeekdays.contains(2));
        binding.chipWeekdayWed.setChecked(selectedWeekdays.contains(3));
        binding.chipWeekdayThu.setChecked(selectedWeekdays.contains(4));
        binding.chipWeekdayFri.setChecked(selectedWeekdays.contains(5));
        binding.chipWeekdaySat.setChecked(selectedWeekdays.contains(6));
        binding.chipWeekdaySun.setChecked(selectedWeekdays.contains(7));
    }

    private void refreshDateTimeControls() {
        binding.waitlistDayModeGroup.check(specificDayMode
                ? R.id.btnWaitlistModeSpecificDay
                : R.id.btnWaitlistModeWeekdays);
        refreshDayModeVisibility();
        binding.txtWaitlistSpecificDay.setText(
                Formats.formatDayWithShortWeekday(selectedSpecificDay));
        binding.txtWaitlistValidUntil.setText(
                Formats.formatDayWithShortWeekday(validUntilDay));
        binding.btnWaitlistFromTime.setText(getString(
                R.string.waitlist_from_time_format, Formats.formatTime(fromMin)));
        binding.btnWaitlistToTime.setText(getString(
                R.string.waitlist_to_time_format, Formats.formatTime(toMin)));
    }

    private void refreshDayModeVisibility() {
        binding.waitlistSpecificDayContainer.setVisibility(
                specificDayMode ? View.VISIBLE : View.GONE);
        binding.waitlistWeekdaysChips.setVisibility(
                specificDayMode ? View.GONE : View.VISIBLE);
    }

    private void openDatePicker(long initialDay, @NonNull java.util.function.LongConsumer onPicked) {
        LocalDate initial = LocalDate.ofEpochDay(initialDay);
        new DatePickerDialog(
                requireContext(),
                (picker, year, monthZero, day) ->
                        onPicked.accept(LocalDate.of(year, monthZero + 1, day).toEpochDay()),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        ).show();
    }

    private void openTimePicker(int initialMin, @NonNull java.util.function.IntConsumer onPicked) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(initialMin / 60)
                .setMinute(initialMin % 60)
                .build();
        picker.addOnPositiveButtonClickListener(
                v -> onPicked.accept(picker.getHour() * 60 + picker.getMinute()));
        picker.show(getChildFragmentManager(), "waitlist_time_picker");
    }

    private void submitForm() {
        CharSequence notesCs = binding.inputWaitlistNotes.getText();
        String notes = notesCs == null ? "" : notesCs.toString().trim();
        viewModel.saveEntry(
                editingEntryId,
                selectedClientId,
                new ArrayList<>(selectedServiceIds),
                selectedProfessionalId,
                specificDayMode ? selectedSpecificDay : null,
                specificDayMode ? Collections.emptySet() : new TreeSet<>(selectedWeekdays),
                fromMin,
                toMin,
                validUntilDay,
                notes
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
