package com.metanoia.app.ui.cash.receivables;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.metanoia.app.databinding.FragmentReceivablesBinding;
import com.metanoia.app.ui.cash.charge.ChargeAppointmentDialogFragment;

/**
 * Listado de turnos completados con saldo pendiente. Tocar una tarjeta abre el cobro.
 */
public class ReceivablesFragment extends Fragment {

    private FragmentReceivablesBinding binding;
    private ReceivablesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentReceivablesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ReceivablesViewModel viewModel = new ViewModelProvider(this).get(ReceivablesViewModel.class);
        adapter = new ReceivablesAdapter(card ->
                ChargeAppointmentDialogFragment.newInstance(card.id, false)
                        .show(getChildFragmentManager(), "charge_appointment"));
        binding.receivablesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.receivablesRecycler.setAdapter(adapter);

        viewModel.getReceivables().observe(getViewLifecycleOwner(), cards -> {
            boolean empty = cards == null || cards.isEmpty();
            binding.receivablesRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.receivablesEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submitList(cards);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
