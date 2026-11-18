package com.metanoia.app.ui.agenda.detail;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogCancelReasonBinding;
import com.metanoia.app.databinding.FragmentAppointmentDetailBinding;
import com.metanoia.app.ui.agenda.create.AppointmentCreateFragment;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailViewModel.DetailUiState;
import com.metanoia.app.ui.common.ConfirmDialog;
import com.metanoia.app.ui.common.MoneyTextView;
import com.metanoia.app.ui.common.StatusChip;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Money;

import java.util.ArrayList;

/**
 * Pantalla de detalle y acciones sobre un turno según su estado actual.
 */
public class AppointmentDetailFragment extends Fragment {

    public static final String ARG_APPOINTMENT_ID = "appointment_id";
    private static final String TAG_RESCHEDULE = "reschedule_appointment_dialog";

    private FragmentAppointmentDetailBinding binding;
    private AppointmentDetailViewModel viewModel;
    @Nullable
    private Appointment currentAppointment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAppointmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String appointmentId = getArguments() == null
                ? null
                : getArguments().getString(ARG_APPOINTMENT_ID);
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.appointment_detail_not_found,
                    Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        viewModel = new ViewModelProvider(this).get(AppointmentDetailViewModel.class);
        viewModel.observeRawAppointment(appointmentId.trim())
                .observe(getViewLifecycleOwner(), viewModel::resolveDetail);
        viewModel.observeDetailState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                currentAppointment = state.appointment;
                bindState(state);
            }
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        setupActionListeners();
    }

    private void setupActionListeners() {
        binding.btnActionConfirm.setOnClickListener(v -> {
            if (currentAppointment != null) {
                viewModel.changeStatus(
                        currentAppointment.id(), AppointmentStatus.CONFIRMED, null);
            }
        });
        binding.btnActionComplete.setOnClickListener(v -> {
            if (currentAppointment != null) {
                viewModel.changeStatus(
                        currentAppointment.id(), AppointmentStatus.COMPLETED, null);
            }
        });
        binding.btnActionEdit.setOnClickListener(v -> {
            if (currentAppointment != null) {
                Bundle args = new Bundle();
                args.putString(
                        AppointmentCreateFragment.ARG_EDIT_APPOINTMENT_ID,
                        currentAppointment.id());
                NavHostFragment.findNavController(this).navigate(R.id.nav_appointment_create, args);
            }
        });
        binding.btnActionReschedule.setOnClickListener(v -> openRescheduleDialog());
        binding.btnActionNoShow.setOnClickListener(v -> {
            if (currentAppointment == null) {
                return;
            }
            String id = currentAppointment.id();
            ConfirmDialog.show(
                    requireContext(),
                    R.string.appointment_no_show_confirm_title,
                    R.string.appointment_no_show_confirm_message,
                    R.string.appointment_action_no_show,
                    true,
                    () -> viewModel.changeStatus(id, AppointmentStatus.NO_SHOW, null)
            );
        });
        binding.btnActionCancel.setOnClickListener(v -> openCancelReasonDialog());
        binding.btnActionRevertCompleted.setOnClickListener(v -> {
            if (currentAppointment == null) {
                return;
            }
            String id = currentAppointment.id();
            ConfirmDialog.show(
                    requireContext(),
                    R.string.appointment_revert_confirm_title,
                    R.string.appointment_revert_confirm_message,
                    R.string.appointment_action_revert_completed,
                    true,
                    () -> viewModel.changeStatus(id, AppointmentStatus.CONFIRMED, null)
            );
        });
    }

    private void bindState(@NonNull DetailUiState state) {
        Appointment apt = state.appointment;
        binding.detailClientName.setText(state.clientName);
        binding.detailStatusChip.setVariant(StatusChip.Variant.valueOf(apt.status().name()));
        String dateText = Formats.formatDayWithShortWeekday(apt.day());
        String timeText = getString(
                R.string.agenda_time_inline,
                Formats.formatTime(apt.startMin()),
                Formats.formatTime(apt.endMin()));
        binding.detailDateTime.setText(
                getString(R.string.agenda_block_header, dateText, timeText));
        binding.detailProfessionalName.setText(
                getString(R.string.appointment_detail_professional_format, state.professionalName));

        renderItems(apt);
        AppointmentBalance balance = AppointmentBalance.of(
                apt,
                apt.status() == AppointmentStatus.PAID ? apt.totalAmount() : Money.zero());
        binding.detailTotalValue.setMoney(balance.total());
        binding.detailPaidValue.setMoney(balance.paid());
        binding.detailBalanceValue.setMoney(balance.balance());

        if (apt.status() == AppointmentStatus.CANCELLED && apt.cancelReason().isPresent()) {
            binding.detailNotesCard.setVisibility(View.VISIBLE);
            binding.detailNotesLabel.setText(R.string.appointment_detail_cancel_reason_section);
            binding.detailNotesValue.setText(apt.cancelReason().get());
        } else if (!apt.notes().isEmpty()) {
            binding.detailNotesCard.setVisibility(View.VISIBLE);
            binding.detailNotesLabel.setText(R.string.appointment_detail_notes_section);
            binding.detailNotesValue.setText(apt.notes());
        } else {
            binding.detailNotesCard.setVisibility(View.GONE);
        }

        updateActionButtonsVisibility(apt.status());
    }

    private void renderItems(@NonNull Appointment apt) {
        binding.detailItemsContainer.removeAllViews();
        int rowBottomMargin = getResources().getDimensionPixelSize(R.dimen.space_xs);
        for (AppointmentItem item : apt.items()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = rowBottomMargin;
            row.setLayoutParams(rowParams);

            LinearLayout line = new LinearLayout(requireContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView nameView = new TextView(requireContext());
            TextViewCompat.setTextAppearance(nameView, R.style.TextAppearance_Metanoia_BodySmall);
            nameView.setTextColor(MaterialColors.getColor(
                    nameView, com.google.android.material.R.attr.colorOnSurface, 0));
            nameView.setText(getString(
                    R.string.appointment_detail_item_format,
                    item.serviceNameSnapshot(),
                    item.durationMinSnapshot()));
            nameView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            MoneyTextView priceView = new MoneyTextView(requireContext());
            priceView.setMoney(item.priceSnapshot());
            line.addView(nameView);
            line.addView(priceView);
            row.addView(line);

            if (item.commissionBasisPointsSnapshot().isPresent()) {
                TextView commView = new TextView(requireContext());
                TextViewCompat.setTextAppearance(commView, R.style.TextAppearance_Metanoia_Caption);
                commView.setTextColor(MaterialColors.getColor(
                        commView, com.google.android.material.R.attr.colorOnSurfaceVariant, 0));
                commView.setText(getString(
                        R.string.appointment_detail_commission_frozen,
                        Formats.formatPercent(item.commissionBasisPointsSnapshot().get())));
                row.addView(commView);
            }
            binding.detailItemsContainer.addView(row);
        }
    }

    private void updateActionButtonsVisibility(@NonNull AppointmentStatus status) {
        boolean isPending = status == AppointmentStatus.PENDING;
        boolean isConfirmed = status == AppointmentStatus.CONFIRMED;
        boolean isCompleted = status == AppointmentStatus.COMPLETED;
        boolean isEditable = isPending || isConfirmed;

        binding.btnActionConfirm.setVisibility(isPending ? View.VISIBLE : View.GONE);
        binding.btnActionComplete.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionEdit.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionReschedule.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionNoShow.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionCancel.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionRevertCompleted.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
    }

    private void openRescheduleDialog() {
        if (currentAppointment == null) {
            return;
        }
        ArrayList<String> serviceIds = new ArrayList<>();
        for (AppointmentItem item : currentAppointment.items()) {
            serviceIds.add(item.serviceId());
        }
        RescheduleDialogFragment.newInstance(
                currentAppointment.id(),
                currentAppointment.professionalId(),
                currentAppointment.day(),
                currentAppointment.startMin(),
                currentAppointment.totalDurationMin(),
                serviceIds
        ).show(getChildFragmentManager(), TAG_RESCHEDULE);
    }

    private void openCancelReasonDialog() {
        if (currentAppointment == null) {
            return;
        }
        String appointmentId = currentAppointment.id();
        DialogCancelReasonBinding dialogBinding = DialogCancelReasonBinding.inflate(
                getLayoutInflater());
        TextInputEditText reasonInput = dialogBinding.inputCancelReason;

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.appointment_cancel_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.appointment_cancel_confirm_btn, (d, which) -> {
                    CharSequence cs = reasonInput.getText();
                    String reason = cs == null ? "" : cs.toString().trim();
                    viewModel.changeStatus(appointmentId, AppointmentStatus.CANCELLED, reason);
                })
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
