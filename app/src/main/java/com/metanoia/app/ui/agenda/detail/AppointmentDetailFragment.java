package com.metanoia.app.ui.agenda.detail;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import androidx.core.content.FileProvider;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.data.db.entity.ReminderEntity;
import com.metanoia.app.data.mapper.CashMapper;
import com.metanoia.app.databinding.DialogCancelReasonBinding;
import com.metanoia.app.databinding.DialogReverseReasonBinding;
import com.metanoia.app.databinding.FragmentAppointmentDetailBinding;
import com.metanoia.app.ui.agenda.create.AppointmentCreateFragment;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailViewModel.DetailUiState;
import com.metanoia.app.ui.cash.CashUiLabels;
import com.metanoia.app.ui.cash.charge.ChargeAppointmentDialogFragment;
import com.metanoia.app.ui.common.ConfirmDialog;
import com.metanoia.app.ui.common.MoneyTextView;
import com.metanoia.app.ui.common.StatusChip;
import com.metanoia.app.util.Formats;
import com.metanoia.app.util.WhatsAppLauncher;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.rules.AppointmentPaymentCalculator;
import com.metanoia.domain.usecase.CancelRecurringSeriesUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de detalle y acciones sobre un turno según su estado actual.
 */
public class AppointmentDetailFragment extends Fragment {

    public static final String ARG_APPOINTMENT_ID = "appointment_id";
    private static final String TAG_RESCHEDULE = "reschedule_appointment_dialog";
    private static final String TAG_CHARGE = "charge_appointment_dialog";

    private FragmentAppointmentDetailBinding binding;
    private AppointmentDetailViewModel viewModel;
    @Nullable
    private Appointment currentAppointment;
    @Nullable
    private DetailUiState latestDetailState;
    @Nullable
    private ReminderEntity latestReminder;
    private List<CashMovement> latestMovements = Collections.emptyList();

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

        String trimmedId = appointmentId.trim();
        viewModel = new ViewModelProvider(this).get(AppointmentDetailViewModel.class);
        viewModel.observeRawAppointment(trimmedId)
                .observe(getViewLifecycleOwner(), viewModel::resolveDetail);
        viewModel.observeAppointmentMovements(trimmedId).observe(
                getViewLifecycleOwner(),
                entities -> {
                    List<CashMovement> mapped = new ArrayList<>();
                    if (entities != null) {
                        for (CashMovementEntity entity : entities) {
                            mapped.add(CashMapper.toDomain(entity));
                        }
                    }
                    latestMovements = mapped;
                    if (latestDetailState != null) {
                        bindState(latestDetailState);
                    }
                });
        viewModel.observeDetailState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                latestDetailState = state;
                currentAppointment = state.appointment;
                bindState(state);
            }
        });
        viewModel.observeAppointmentReminder(trimmedId).observe(getViewLifecycleOwner(), entity -> {
            latestReminder = entity;
            if (latestDetailState != null) {
                bindState(latestDetailState);
            }
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeShareReceipt().observe(getViewLifecycleOwner(), event -> {
            java.io.File pdfFile = event.getContentIfNotHandled();
            if (pdfFile != null) {
                sharePdfReceipt(pdfFile);
            }
        });

        setupActionListeners();
    }

    private void setupActionListeners() {
        binding.btnActionCharge.setOnClickListener(v -> {
            if (currentAppointment != null) {
                ChargeAppointmentDialogFragment.newInstance(currentAppointment.id(), false)
                        .show(getChildFragmentManager(), TAG_CHARGE);
            }
        });
        binding.btnActionCompleteAndCharge.setOnClickListener(v -> {
            if (currentAppointment != null) {
                ChargeAppointmentDialogFragment.newInstance(currentAppointment.id(), true)
                        .show(getChildFragmentManager(), TAG_CHARGE);
            }
        });
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
        binding.btnActionRemind.setOnClickListener(v -> remindByWhatsApp());
        binding.btnUndoAppointmentReminder.setOnClickListener(v -> {
            if (latestReminder != null) {
                viewModel.undoReminder(latestReminder.id);
            }
        });
        binding.btnActionShareReceipt.setOnClickListener(v -> {
            if (latestDetailState != null) {
                viewModel.generateReceiptPdf(latestDetailState, latestMovements);
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
        binding.detailRecurringBadge.setVisibility(
                apt.recurrenceRuleId().isPresent() ? View.VISIBLE : View.GONE);

        renderItems(apt);
        AppointmentBalance balance = AppointmentPaymentCalculator.balanceOf(apt, latestMovements);
        binding.detailTotalValue.setMoney(balance.total());
        binding.detailPaidValue.setMoney(balance.paid());
        binding.detailBalanceValue.setMoney(balance.balance());
        renderPayments(latestMovements);

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

        updateActionButtonsVisibility(apt.status(), balance);
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
            if (item.colorFormulaId().isPresent()) {
                TextView formulaView = new TextView(requireContext());
                TextViewCompat.setTextAppearance(
                        formulaView, R.style.TextAppearance_Metanoia_Caption);
                formulaView.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                formulaView.setText(R.string.appointment_detail_color_formula_linked);
                row.addView(formulaView);
            }
            binding.detailItemsContainer.addView(row);
        }
    }

    private void renderPayments(@NonNull List<CashMovement> movements) {
        if (movements.isEmpty()) {
            binding.detailPaymentsCard.setVisibility(View.GONE);
            binding.detailPaymentsContainer.removeAllViews();
            return;
        }
        binding.detailPaymentsCard.setVisibility(View.VISIBLE);
        binding.detailPaymentsContainer.removeAllViews();
        Set<String> reversedIds = new HashSet<>();
        for (CashMovement m : movements) {
            m.reversesMovementId().ifPresent(reversedIds::add);
        }
        int rowBottomMargin = getResources().getDimensionPixelSize(R.dimen.space_xs);
        for (CashMovement movement : movements) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = rowBottomMargin;
            row.setLayoutParams(params);

            TextView labelView = new TextView(requireContext());
            TextViewCompat.setTextAppearance(labelView, R.style.TextAppearance_Metanoia_BodySmall);
            labelView.setTextColor(MaterialColors.getColor(
                    labelView, com.google.android.material.R.attr.colorOnSurface, 0));
            labelView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            String methodName = CashUiLabels.methodLabel(requireContext(), movement.method());
            if (movement.type() == CashMovementType.REVERSAL) {
                labelView.setText(getString(
                        R.string.appointment_detail_reversal_row_prefix, methodName));
            } else {
                labelView.setText(getString(
                        R.string.appointment_detail_payment_line_format,
                        methodName,
                        Formats.formatDateTime(movement.timestamp())));
            }

            MoneyTextView amountView = new MoneyTextView(requireContext());
            long signedCents = movement.type() == CashMovementType.REVERSAL
                    ? -movement.amount().cents()
                    : movement.amount().cents();
            amountView.setMoneyCents(signedCents);
            row.addView(labelView);
            row.addView(amountView);

            if (movement.type() == CashMovementType.APPOINTMENT_PAYMENT
                    && !reversedIds.contains(movement.id())) {
                MaterialButton reverseBtn = new MaterialButton(
                        requireContext(),
                        null,
                        com.google.android.material.R.attr.borderlessButtonStyle);
                reverseBtn.setText(R.string.cash_reverse_action);
                reverseBtn.setOnClickListener(v -> openReverseDialog(movement.id()));
                row.addView(reverseBtn);
            }
            binding.detailPaymentsContainer.addView(row);
        }
    }

    private void openReverseDialog(@NonNull String movementId) {
        DialogReverseReasonBinding dialogBinding = DialogReverseReasonBinding.inflate(
                getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cash_reverse_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.cash_reverse_confirm_btn, (d, which) -> {
                    CharSequence cs = dialogBinding.inputReverseReason.getText();
                    String reason = cs == null ? "" : cs.toString().trim();
                    viewModel.reverseMovement(movementId, reason);
                })
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
    }

    private void updateActionButtonsVisibility(
            @NonNull AppointmentStatus status,
            @NonNull AppointmentBalance balance
    ) {
        boolean isPending = status == AppointmentStatus.PENDING;
        boolean isConfirmed = status == AppointmentStatus.CONFIRMED;
        boolean isCompleted = status == AppointmentStatus.COMPLETED;
        boolean isPaid = status == AppointmentStatus.PAID;
        boolean isEditable = isPending || isConfirmed;

        binding.btnActionCharge.setVisibility(
                isCompleted && balance.balance().isPositive() ? View.VISIBLE : View.GONE);
        binding.btnActionCompleteAndCharge.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionConfirm.setVisibility(isPending ? View.VISIBLE : View.GONE);
        binding.btnActionComplete.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionEdit.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionReschedule.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionNoShow.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionCancel.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        binding.btnActionShareReceipt.setVisibility(
                isCompleted || isPaid ? View.VISIBLE : View.GONE);
        binding.btnActionRevertCompleted.setVisibility(
                isCompleted && !balance.hasPayments() ? View.VISIBLE : View.GONE);

        if (isEditable) {
            boolean hasPhone = latestDetailState != null
                    && WhatsAppLauncher.hasValidPhone(latestDetailState.clientPhone);
            binding.btnActionRemind.setVisibility(View.VISIBLE);
            binding.btnActionRemind.setEnabled(hasPhone);
            binding.detailNoPhoneHint.setVisibility(hasPhone ? View.GONE : View.VISIBLE);
            if (latestReminder != null) {
                binding.detailReminderRow.setVisibility(View.VISIBLE);
                binding.detailReminderStatusText.setText(getString(
                        R.string.appointment_detail_reminded_format,
                        Formats.formatDateTime(latestReminder.sentAt)));
                binding.btnActionRemind.setText(R.string.appointment_action_remind_again);
            } else {
                binding.detailReminderRow.setVisibility(View.GONE);
                binding.btnActionRemind.setText(R.string.appointment_action_remind);
            }
        } else {
            binding.btnActionRemind.setVisibility(View.GONE);
            binding.detailNoPhoneHint.setVisibility(View.GONE);
            binding.detailReminderRow.setVisibility(View.GONE);
        }
    }

    private void remindByWhatsApp() {
        if (latestDetailState == null) {
            return;
        }
        DetailUiState snapshot = latestDetailState;
        Appointment apt = snapshot.appointment;
        viewModel.buildAppointmentReminderMessage(snapshot, message -> {
            if (binding == null) {
                return;
            }
            if (WhatsAppLauncher.openChat(requireContext(), snapshot.clientPhone, message)) {
                viewModel.markAppointmentReminded(apt.id(), apt.clientId(), reminder -> {
                    if (binding == null) {
                        return;
                    }
                    Snackbar.make(binding.getRoot(), R.string.reminder_marked_sent,
                                    Snackbar.LENGTH_LONG)
                            .setAction(R.string.reminder_undo_action,
                                    v -> viewModel.undoReminder(reminder.id()))
                            .show();
                });
            }
        });
    }

    private void sharePdfReceipt(@NonNull java.io.File pdfFile) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pdfFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.receipt_title));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(
                    intent, getString(R.string.receipt_share_chooser_title)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), R.string.receipt_share_no_app, Toast.LENGTH_SHORT)
                    .show();
        }
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
        boolean isRecurring = currentAppointment.recurrenceRuleId().isPresent();
        DialogCancelReasonBinding dialogBinding = DialogCancelReasonBinding.inflate(
                getLayoutInflater());
        dialogBinding.radioGroupCancelScope.setVisibility(isRecurring ? View.VISIBLE : View.GONE);
        TextInputEditText reasonInput = dialogBinding.inputCancelReason;

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.appointment_cancel_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.appointment_cancel_confirm_btn, (d, which) -> {
                    CharSequence cs = reasonInput.getText();
                    String reason = cs == null ? "" : cs.toString().trim();
                    if (isRecurring) {
                        CancelRecurringSeriesUseCase.Scope scope =
                                dialogBinding.radioCancelFollowing.isChecked()
                                        ? CancelRecurringSeriesUseCase.Scope.THIS_AND_FOLLOWING
                                        : CancelRecurringSeriesUseCase.Scope.ONLY_THIS;
                        viewModel.cancelRecurring(appointmentId, scope, reason);
                    } else {
                        viewModel.changeStatus(appointmentId, AppointmentStatus.CANCELLED, reason);
                    }
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
