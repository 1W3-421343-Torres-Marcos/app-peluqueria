package com.metanoia.app.ui.cash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.databinding.DialogReverseReasonBinding;
import com.metanoia.app.databinding.FragmentCashBinding;
import com.metanoia.app.util.Formats;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Pantalla de Caja: resumen diario por medio de pago, movimientos, ventas sueltas/gastos
 * y acceso a cobros pendientes.
 */
public class CashFragment extends Fragment {

    private FragmentCashBinding binding;
    private CashViewModel viewModel;
    private CashMovementsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CashViewModel.class);

        adapter = new CashMovementsAdapter(this::confirmReverseMovement);
        binding.cashMovementsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.cashMovementsRecycler.setAdapter(adapter);

        binding.btnCashPrevDate.setOnClickListener(v -> viewModel.previousDay());
        binding.btnCashNextDate.setOnClickListener(v -> viewModel.nextDay());
        binding.btnCashToday.setOnClickListener(v -> viewModel.selectToday());
        binding.cashDateLabel.setOnClickListener(v -> showDatePicker());

        binding.btnCashAddIncome.setOnClickListener(v ->
                CashMovementFormDialogFragment.newInstance(false)
                        .show(getChildFragmentManager(), "cash_income_form"));
        binding.btnCashAddExpense.setOnClickListener(v ->
                CashMovementFormDialogFragment.newInstance(true)
                        .show(getChildFragmentManager(), "cash_expense_form"));
        binding.btnCashReceivables.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_cash_to_receivables));

        viewModel.getSelectedDay().observe(getViewLifecycleOwner(), epochDay -> {
            if (epochDay != null) {
                binding.cashDateLabel.setText(Formats.formatDayWithShortWeekday(epochDay));
                boolean isToday = epochDay == viewModel.todayEpochDay();
                binding.btnCashToday.setVisibility(isToday ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getReceivablesCount().observe(getViewLifecycleOwner(), count -> {
            int safeCount = count == null ? 0 : count;
            binding.btnCashReceivables.setText(
                    getString(R.string.cash_receivables_button_format, safeCount));
        });

        viewModel.getDayState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            binding.cashSummaryCashAmount.setMoneyCents(state.cashCents);
            binding.cashSummaryTransferAmount.setMoneyCents(state.transferCents);
            binding.cashSummaryCardAmount.setMoneyCents(state.cardCents);
            binding.cashSummaryMercadoPagoAmount.setMoneyCents(state.mercadoPagoCents);
            binding.cashSummaryNetAmount.setMoneyCents(state.netTotalCents);

            boolean empty = state.rows.isEmpty();
            binding.cashMovementsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.cashEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submitList(state.rows);
        });

        viewModel.getMessageEvent().observe(getViewLifecycleOwner(), event -> {
            Integer msgRes = event == null ? null : event.getContentIfNotHandled();
            if (msgRes != null) {
                Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        Long current = viewModel.getSelectedDay().getValue();
        long initialMillis = (current == null ? viewModel.todayEpochDay() : current)
                * 86_400_000L;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(initialMillis)
                .build();
        picker.addOnPositiveButtonClickListener(selectionMillis -> {
            if (selectionMillis != null) {
                long epochDay = Instant.ofEpochMilli(selectionMillis)
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDate()
                        .toEpochDay();
                viewModel.setSelectedDay(epochDay);
            }
        });
        picker.show(getChildFragmentManager(), "cash_date_picker");
    }

    private void confirmReverseMovement(@NonNull CashMovementEntity movement) {
        DialogReverseReasonBinding dialogBinding = DialogReverseReasonBinding.inflate(
                LayoutInflater.from(requireContext()));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cash_reverse_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.cash_reverse_confirm_btn, (dialog, which) -> {
                    String reason = dialogBinding.inputReverseReason.getText() == null
                            ? ""
                            : dialogBinding.inputReverseReason.getText().toString();
                    viewModel.reverseMovement(movement.id, reason);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
