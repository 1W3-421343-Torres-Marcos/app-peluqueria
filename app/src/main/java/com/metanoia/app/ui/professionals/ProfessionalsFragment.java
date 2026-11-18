package com.metanoia.app.ui.professionals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.metanoia.app.R;
import com.metanoia.app.databinding.FragmentProfessionalsBinding;

/**
 * Pantalla del listado de profesionales filtrable por activos/todos.
 */
public class ProfessionalsFragment extends Fragment {

    private FragmentProfessionalsBinding binding;
    private ProfessionalsViewModel viewModel;
    private ProfessionalsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfessionalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfessionalsViewModel.class);

        adapter = new ProfessionalsAdapter(details -> openForm(details.professional.id));
        binding.professionalsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.professionalsRecycler.setAdapter(adapter);

        binding.professionalsFilterGroup.check(R.id.btnProFilterActive);
        binding.professionalsFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setIncludeInactive(checkedId == R.id.btnProFilterAll);
            }
        });

        binding.fabNewProfessional.setOnClickListener(v -> openForm(null));

        viewModel.observeProfessionals().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.professionalsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.professionalsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (!empty) {
                adapter.submitList(list);
            }
        });
    }

    private void openForm(@Nullable String professionalId) {
        Bundle args = new Bundle();
        if (professionalId != null) {
            args.putString(ProfessionalFormFragment.ARG_PROFESSIONAL_ID, professionalId);
        }
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_professionals_to_form, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
