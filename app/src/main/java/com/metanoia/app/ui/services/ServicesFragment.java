package com.metanoia.app.ui.services;

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

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.databinding.FragmentServicesBinding;

/**
 * Pantalla del catálogo de servicios agrupada por categoría y filtrable por estado.
 */
public class ServicesFragment extends Fragment {

    private static final String TAG_FORM = "service_form";

    private FragmentServicesBinding binding;
    private ServicesViewModel viewModel;
    private ServicesAdapter adapter;
    private int currentSlotMinutes = 15;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentServicesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ServicesViewModel.class);

        adapter = new ServicesAdapter(this::openForm);
        binding.servicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.servicesRecycler.setAdapter(adapter);

        binding.servicesFilterGroup.check(R.id.btnFilterActive);
        binding.servicesFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setIncludeInactive(checkedId == R.id.btnFilterAll);
            }
        });

        binding.fabNewService.setOnClickListener(v -> openForm(null));

        viewModel.observeSlotMinutes().observe(getViewLifecycleOwner(), slot -> {
            if (slot != null && slot > 0) {
                currentSlotMinutes = slot;
            }
        });

        viewModel.observeServices().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.servicesEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.servicesRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (!empty) {
                adapter.submitGrouped(list);
            }
        });

        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openForm(@Nullable SalonServiceEntity existing) {
        ServiceFormDialogFragment.newInstance(existing, currentSlotMinutes)
                .show(getChildFragmentManager(), TAG_FORM);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
