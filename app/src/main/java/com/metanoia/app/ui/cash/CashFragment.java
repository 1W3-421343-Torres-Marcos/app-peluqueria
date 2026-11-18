package com.metanoia.app.ui.cash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.metanoia.app.R;
import com.metanoia.app.databinding.FragmentCashBinding;

/**
 * Pantalla de Caja (movimientos del día, cobros pendientes y cierre diario).
 */
public class CashFragment extends Fragment {

    private FragmentCashBinding binding;

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
        CashViewModel viewModel = new ViewModelProvider(this).get(CashViewModel.class);
        viewModel.getReceivablesCount().observe(getViewLifecycleOwner(), count -> {
            int safeCount = count == null ? 0 : count;
            binding.btnCashReceivables.setText(
                    getString(R.string.cash_receivables_button_format, safeCount));
        });
        binding.btnCashReceivables.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_cash_to_receivables));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
