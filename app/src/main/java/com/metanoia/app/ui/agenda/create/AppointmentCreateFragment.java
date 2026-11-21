package com.metanoia.app.ui.agenda.create;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.ColorFormulaWithComponents;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.data.mapper.ColorFormulaMapper;
import com.metanoia.app.databinding.FragmentAppointmentCreateBinding;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader.SlotLoadResult;
import com.metanoia.app.ui.clients.ClientDetailFragment;
import com.metanoia.app.ui.clients.ClientFormDialogFragment;
import com.metanoia.app.ui.waitlist.WaitlistFormDialogFragment;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.rules.PhoneNormalizer;
import com.metanoia.domain.usecase.GenerateRecurringAppointmentsUseCase.RecurringResult;
import com.metanoia.domain.usecase.GenerateRecurringAppointmentsUseCase.SkippedOccurrence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flujo guiado en una pantalla para crear (≤ 4 toques) o editar servicios/notas de un turno.
 */
public class AppointmentCreateFragment extends Fragment {

    public static final String ARG_INITIAL_DAY = "initial_day";
    public static final String ARG_INITIAL_PROFESSIONAL_ID = "initial_professional_id";
    public static final String ARG_EDIT_APPOINTMENT_ID = "edit_appointment_id";

    private static final String TAG_QUICK_CLIENT = "quick_client_dialog";
    private static final String TAG_ADD_WAITLIST = "agenda_add_waitlist_dialog";

    private FragmentAppointmentCreateBinding binding;
    private AppointmentCreateViewModel viewModel;

    @Nullable
    private String editingAppointmentId;
    @Nullable
    private String selectedClientId;
    @Nullable
    private String selectedClientName;
    @Nullable
    private String preferredProfessionalId;
    @Nullable
    private String selectedProfessionalId;
    private long selectedDayEpoch;
    @Nullable
    private Integer selectedStartMin;
    private int currentSlotMinutes = 15;
    private boolean editLoaded;

    private final List<String> selectedServiceIds = new ArrayList<>();
    private final Map<String, SalonServiceEntity> servicesById = new LinkedHashMap<>();
    private List<ProfessionalWithDetails> allProfessionals = Collections.emptyList();
    @Nullable
    private ColorFormulaWithComponents latestClientFormula;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAppointmentCreateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AppointmentCreateViewModel.class);

        Bundle args = getArguments();
        editingAppointmentId = args == null ? null : args.getString(ARG_EDIT_APPOINTMENT_ID);
        boolean isEdit = editingAppointmentId != null && !editingAppointmentId.isEmpty();

        selectedDayEpoch = (args != null && args.containsKey(ARG_INITIAL_DAY))
                ? args.getLong(ARG_INITIAL_DAY)
                : viewModel.todayEpochDay();
        preferredProfessionalId = args == null
                ? null
                : args.getString(ARG_INITIAL_PROFESSIONAL_ID);
        selectedProfessionalId = preferredProfessionalId;

        configureUiForMode(isEdit);
        observeData(isEdit);

        if (isEdit && editingAppointmentId != null) {
            viewModel.loadForEdit(editingAppointmentId);
        }
    }

    private void configureUiForMode(boolean isEdit) {
        binding.aptCreateTitleHeader.setText(isEdit
                ? R.string.appointment_edit_title
                : R.string.appointment_create_title);
        binding.sectionClientPicker.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        binding.sectionProAndSchedule.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        binding.switchAptConfirmed.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        binding.switchAptRepeat.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        if (isEdit) {
            binding.aptRepeatOptionsContainer.setVisibility(View.GONE);
        } else {
            if (binding.inputAptRepeatWeeks.getText() == null
                    || binding.inputAptRepeatWeeks.getText().toString().trim().isEmpty()) {
                binding.inputAptRepeatWeeks.setText("2");
            }
            if (binding.inputAptRepeatCount.getText() == null
                    || binding.inputAptRepeatCount.getText().toString().trim().isEmpty()) {
                binding.inputAptRepeatCount.setText("4");
            }
            binding.aptRepeatOptionsContainer.setVisibility(
                    binding.switchAptRepeat.isChecked() ? View.VISIBLE : View.GONE);
            binding.switchAptRepeat.setOnCheckedChangeListener((buttonView, isChecked) ->
                    binding.aptRepeatOptionsContainer.setVisibility(
                            isChecked ? View.VISIBLE : View.GONE));
        }
        binding.btnConfirmAppointment.setText(isEdit
                ? R.string.appointment_save_edit
                : R.string.appointment_save_create);

        binding.txtAptSelectedDate.setText(Formats.formatDayWithShortWeekday(selectedDayEpoch));
        binding.btnAptChangeDate.setOnClickListener(v -> openDatePicker());
        binding.aptClientSearchField.setOnQueryChangeListener(q -> viewModel.setClientQuery(q));
        binding.btnQuickCreateClient.setOnClickListener(v ->
                ClientFormDialogFragment.newQuickCreateInstance()
                        .show(getChildFragmentManager(), TAG_QUICK_CLIENT));

        getChildFragmentManager().setFragmentResultListener(
                ClientFormDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> handleQuickClientResult(result)
        );

        binding.aptTimeSlotGrid.setOnSlotSelectedListener(startMin -> {
            selectedStartMin = startMin;
            SlotLoadResult current = viewModel.observeSlotOptions().getValue();
            if (current != null) {
                binding.aptTimeSlotGrid.setSlots(current.options, selectedStartMin);
            }
        });

        binding.btnConfirmAppointment.setOnClickListener(v -> submitForm(isEdit));
        binding.btnAddToWaitlistFromAgenda.setOnClickListener(v ->
                WaitlistFormDialogFragment.newCreatePrefilledInstance(
                        selectedClientId,
                        selectedClientName,
                        new ArrayList<>(selectedServiceIds),
                        selectedProfessionalId,
                        selectedDayEpoch)
                        .show(getChildFragmentManager(), TAG_ADD_WAITLIST));
    }

    private void observeData(boolean isEdit) {
        viewModel.observeClientResults().observe(getViewLifecycleOwner(), this::renderClientResults);
        if (!isEdit) {
            viewModel.observeLatestClientFormula().observe(getViewLifecycleOwner(), formula -> {
                latestClientFormula = formula;
                if (formula == null) {
                    binding.chipRepeatLastFormula.setChecked(false);
                }
                updateRepeatFormulaCardVisibility();
            });
        }
        viewModel.observeSlotMinutes().observe(getViewLifecycleOwner(), slotMin -> {
            if (slotMin != null && slotMin > 0) {
                currentSlotMinutes = slotMin;
                updateServicesSummary();
                if (!isEdit) {
                    triggerSlotReload();
                }
            }
        });
        viewModel.observeActiveServices().observe(getViewLifecycleOwner(), services -> {
            servicesById.clear();
            if (services != null) {
                for (SalonServiceEntity svc : services) {
                    servicesById.put(svc.id, svc);
                }
            }
            renderServiceChips(services);
            updateServicesSummary();
            if (!isEdit) {
                renderEligibleProfessionals();
            }
        });
        viewModel.observeActiveProfessionals().observe(getViewLifecycleOwner(), professionals -> {
            allProfessionals = professionals == null ? Collections.emptyList() : professionals;
            if (!isEdit) {
                renderEligibleProfessionals();
            }
        });
        viewModel.observeSlotOptions().observe(getViewLifecycleOwner(), this::renderSlotGrid);
        viewModel.observeEditingAppointment().observe(getViewLifecycleOwner(), appointment -> {
            if (appointment != null && !editLoaded) {
                editLoaded = true;
                populateEditAppointment(appointment);
            }
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeSavedSuccess().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                NavHostFragment.findNavController(this).navigateUp();
            }
        });
        viewModel.observeRecurringResult().observe(getViewLifecycleOwner(), event -> {
            RecurringResult result = event.getContentIfNotHandled();
            if (result != null) {
                showRecurringConflictsDialog(result);
            }
        });
    }

    private void renderClientResults(@Nullable List<ClientRowProjection> clients) {
        binding.aptClientResultsGroup.removeAllViews();
        boolean empty = clients == null || clients.isEmpty();
        binding.txtClientSearchEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
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
            chip.setOnClickListener(v -> selectClient(client.id, client.name));
            binding.aptClientResultsGroup.addView(chip);
        }
    }

    private void selectClient(@NonNull String id, @NonNull String name) {
        selectedClientId = id;
        selectedClientName = name;
        binding.txtSelectedClient.setText(getString(R.string.appointment_client_selected, name));
        binding.txtSelectedClient.setVisibility(View.VISIBLE);
        binding.chipRepeatLastFormula.setChecked(false);
        viewModel.setSelectedClientId(id);
        updateRepeatFormulaCardVisibility();
    }

    private void renderServiceChips(@Nullable List<SalonServiceEntity> services) {
        binding.aptServicesChipGroup.removeAllViews();
        boolean empty = services == null || services.isEmpty();
        binding.txtNoActiveServices.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        for (SalonServiceEntity service : services) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(getString(
                    R.string.appointment_service_chip_format,
                    service.name,
                    service.durationMin,
                    Formats.formatMoney(service.priceCents)));
            chip.setChecked(selectedServiceIds.contains(service.id));
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    if (!selectedServiceIds.contains(service.id)) {
                        selectedServiceIds.add(service.id);
                    }
                } else {
                    selectedServiceIds.remove(service.id);
                }
                updateServicesSummary();
                if (editingAppointmentId == null) {
                    renderEligibleProfessionals();
                }
            });
            binding.aptServicesChipGroup.addView(chip);
        }
    }

    private int updateServicesSummary() {
        if (selectedServiceIds.isEmpty()) {
            binding.aptServicesSummaryRow.setVisibility(View.GONE);
            updateRepeatFormulaCardVisibility();
            return 0;
        }
        int rawDuration = 0;
        long totalCents = 0L;
        for (String id : selectedServiceIds) {
            SalonServiceEntity entity = servicesById.get(id);
            if (entity != null) {
                rawDuration += entity.durationMin;
                totalCents += entity.priceCents;
            }
        }
        int rounded = rawDuration == 0
                ? 0
                : ((rawDuration + currentSlotMinutes - 1) / currentSlotMinutes) * currentSlotMinutes;
        binding.aptServicesSummaryRow.setVisibility(View.VISIBLE);
        binding.txtAptDurationSummary.setText(
                getString(R.string.appointment_services_summary, rounded));
        binding.txtAptEstimatedPrice.setMoneyCents(totalCents);
        updateRepeatFormulaCardVisibility();
        return rounded;
    }

    private void updateRepeatFormulaCardVisibility() {
        if (binding == null) {
            return;
        }
        boolean isEdit = editingAppointmentId != null && !editingAppointmentId.isEmpty();
        boolean hasColorService = false;
        for (String id : selectedServiceIds) {
            SalonServiceEntity entity = servicesById.get(id);
            if (entity != null && entity.colorService) {
                hasColorService = true;
                break;
            }
        }
        boolean hasPreviousFormula = latestClientFormula != null
                && latestClientFormula.formula != null
                && latestClientFormula.components != null
                && !latestClientFormula.components.isEmpty();
        boolean visible = !isEdit
                && selectedClientId != null
                && hasColorService
                && hasPreviousFormula;
        if (visible) {
            ColorFormula formula = ColorFormulaMapper.toDomain(latestClientFormula);
            binding.txtLatestFormulaPreview.setText(getString(
                    R.string.appointment_repeat_formula_preview_format,
                    Formats.formatDate(formula.day()),
                    formula.mainShadesSummary()));
            binding.aptColorFormulaRepeatCard.setVisibility(View.VISIBLE);
        } else {
            binding.aptColorFormulaRepeatCard.setVisibility(View.GONE);
            binding.chipRepeatLastFormula.setChecked(false);
        }
    }

    private void renderEligibleProfessionals() {
        binding.aptProfessionalChipGroup.removeAllViews();
        List<ProfessionalWithDetails> eligible = new ArrayList<>();
        for (ProfessionalWithDetails pro : allProfessionals) {
            if (supportsAllSelectedServices(pro)) {
                eligible.add(pro);
            }
        }
        boolean empty = eligible.isEmpty();
        binding.txtNoEligiblePros.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            selectedProfessionalId = null;
            viewModel.reloadSlots(null, selectedDayEpoch, 0);
            return;
        }

        boolean currentStillEligible = false;
        for (ProfessionalWithDetails pro : eligible) {
            if (pro.professional.id.equals(selectedProfessionalId)) {
                currentStillEligible = true;
                break;
            }
        }
        if (!currentStillEligible) {
            selectedProfessionalId = eligible.get(0).professional.id;
        }

        for (ProfessionalWithDetails pro : eligible) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setText(pro.professional.name);
            chip.setChecked(pro.professional.id.equals(selectedProfessionalId));
            chip.setOnClickListener(v -> {
                selectedProfessionalId = pro.professional.id;
                selectedStartMin = null;
                triggerSlotReload();
            });
            binding.aptProfessionalChipGroup.addView(chip);
        }
        triggerSlotReload();
    }

    private boolean supportsAllSelectedServices(@NonNull ProfessionalWithDetails pro) {
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

    private void triggerSlotReload() {
        int duration = computeSelectedDurationMinutes();
        if (selectedProfessionalId == null || duration <= 0) {
            viewModel.reloadSlots(null, selectedDayEpoch, 0);
            return;
        }
        viewModel.reloadSlots(selectedProfessionalId, selectedDayEpoch, duration);
    }

    private int computeSelectedDurationMinutes() {
        int raw = 0;
        for (String id : selectedServiceIds) {
            SalonServiceEntity svc = servicesById.get(id);
            if (svc != null) {
                raw += svc.durationMin;
            }
        }
        if (raw <= 0) {
            return 0;
        }
        return ((raw + currentSlotMinutes - 1) / currentSlotMinutes) * currentSlotMinutes;
    }

    private void renderSlotGrid(@Nullable SlotLoadResult result) {
        if (result == null) {
            binding.aptTimeSlotGrid.setSlots(Collections.emptyList(), null);
            binding.txtNoFreeSlots.setVisibility(View.GONE);
            binding.btnAddToWaitlistFromAgenda.setVisibility(View.GONE);
            selectedStartMin = null;
            return;
        }
        boolean noFree = result.freeStartMinutes.isEmpty();
        binding.txtNoFreeSlots.setVisibility(noFree ? View.VISIBLE : View.GONE);
        binding.btnAddToWaitlistFromAgenda.setVisibility(noFree ? View.VISIBLE : View.GONE);
        if (noFree) {
            selectedStartMin = null;
        } else if (selectedStartMin == null || !result.freeStartMinutes.contains(selectedStartMin)) {
            selectedStartMin = result.freeStartMinutes.get(0);
        }
        binding.aptTimeSlotGrid.setSlots(result.options, selectedStartMin);
    }

    private void openDatePicker() {
        LocalDate initial = LocalDate.ofEpochDay(selectedDayEpoch);
        new DatePickerDialog(
                requireContext(),
                (picker, year, monthZeroBased, dayOfMonth) -> {
                    selectedDayEpoch = LocalDate.of(year, monthZeroBased + 1, dayOfMonth)
                            .toEpochDay();
                    selectedStartMin = null;
                    binding.txtAptSelectedDate.setText(
                            Formats.formatDayWithShortWeekday(selectedDayEpoch));
                    triggerSlotReload();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        ).show();
    }

    private void populateEditAppointment(@NonNull Appointment appointment) {
        selectedServiceIds.clear();
        for (AppointmentItem item : appointment.items()) {
            selectedServiceIds.add(item.serviceId());
        }
        binding.inputAptNotes.setText(appointment.notes());
        renderServiceChips(new ArrayList<>(servicesById.values()));
        updateServicesSummary();
    }

    private void handleQuickClientResult(@NonNull Bundle result) {
        String type = result.getString(ClientFormDialogFragment.KEY_RESULT_TYPE);
        String clientId = result.getString(ClientFormDialogFragment.KEY_CLIENT_ID);
        String clientName = result.getString(ClientFormDialogFragment.KEY_CLIENT_NAME);
        if (ClientFormDialogFragment.RESULT_SAVED.equals(type)
                && clientId != null && clientName != null) {
            selectClient(clientId, clientName);
        } else if (ClientFormDialogFragment.RESULT_OPEN_EXISTING.equals(type)
                && clientId != null) {
            Bundle navArgs = new Bundle();
            navArgs.putString(ClientDetailFragment.ARG_CLIENT_ID, clientId);
            NavHostFragment.findNavController(this).navigate(R.id.nav_client_detail, navArgs);
        }
    }

    private void submitForm(boolean isEdit) {
        CharSequence notesCs = binding.inputAptNotes.getText();
        String notes = notesCs == null ? "" : notesCs.toString().trim();
        boolean repeatLastColorFormula = !isEdit
                && binding.aptColorFormulaRepeatCard.getVisibility() == View.VISIBLE
                && binding.chipRepeatLastFormula.isChecked();
        if (!isEdit && isSelectedSlotOverlap()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.appointment_overlap_title)
                    .setMessage(R.string.appointment_overlap_confirm)
                    .setNegativeButton(R.string.common_cancel, null)
                    .setPositiveButton(R.string.appointment_overlap_accept,
                            (dialog, which) -> submitValidatedForm(
                                    isEdit, notes, repeatLastColorFormula))
                    .show();
            return;
        }
        submitValidatedForm(isEdit, notes, repeatLastColorFormula);
    }

    private boolean isSelectedSlotOverlap() {
        SlotLoadResult result = viewModel.observeSlotOptions().getValue();
        return result != null
                && selectedStartMin != null
                && result.overlapStartMinutes.contains(selectedStartMin);
    }

    private void submitValidatedForm(
            boolean isEdit,
            @NonNull String notes,
            boolean repeatLastColorFormula
    ) {
        if (isEdit && editingAppointmentId != null) {
            viewModel.updateAppointment(editingAppointmentId, new ArrayList<>(selectedServiceIds),
                    notes);
        } else if (binding.switchAptRepeat.isChecked()) {
            int intervalWeeks = parsePositiveInt(binding.inputAptRepeatWeeks.getText());
            int occurrences = parsePositiveInt(binding.inputAptRepeatCount.getText());
            viewModel.createRecurringAppointment(
                    selectedClientId,
                    selectedProfessionalId,
                    selectedDayEpoch,
                    selectedStartMin,
                    new ArrayList<>(selectedServiceIds),
                    notes,
                    binding.switchAptConfirmed.isChecked(),
                    intervalWeeks,
                    occurrences,
                    repeatLastColorFormula
            );
        } else {
            viewModel.createAppointment(
                    selectedClientId,
                    selectedProfessionalId,
                    selectedDayEpoch,
                    selectedStartMin,
                    new ArrayList<>(selectedServiceIds),
                    notes,
                    binding.switchAptConfirmed.isChecked(),
                    repeatLastColorFormula
            );
        }
    }

    private int parsePositiveInt(@Nullable CharSequence raw) {
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void showRecurringConflictsDialog(@NonNull RecurringResult result) {
        StringBuilder skippedDates = new StringBuilder();
        for (int i = 0; i < result.conflicts().size(); i++) {
            SkippedOccurrence skipped = result.conflicts().get(i);
            if (i > 0) {
                skippedDates.append('\n');
            }
            skippedDates.append("• ").append(Formats.formatDayWithShortWeekday(skipped.dayEpoch()));
        }
        String message = getString(
                R.string.appointment_recurring_created_with_conflicts,
                result.createdAppointments().size(),
                result.conflicts().size(),
                skippedDates.toString()
        );
        if (!result.overlaps().isEmpty()) {
            message += "\n\n" + getString(
                    R.string.appointment_recurring_overlap_warning,
                    result.overlaps().size());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.appointment_recurring_created_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.common_ok,
                        (dialog, which) -> NavHostFragment.findNavController(this).navigateUp())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
