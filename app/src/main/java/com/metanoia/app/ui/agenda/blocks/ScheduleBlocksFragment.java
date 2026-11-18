package com.metanoia.app.ui.agenda.blocks;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.FragmentScheduleBlocksBinding;
import com.metanoia.app.ui.common.ConfirmDialog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pantalla de administración de bloqueos de agenda generales y por profesional.
 */
public class ScheduleBlocksFragment extends Fragment {

    private static final String TAG_NEW_BLOCK = "new_schedule_block_dialog";

    private FragmentScheduleBlocksBinding binding;
    private ScheduleBlocksViewModel viewModel;
    private ScheduleBlocksAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentScheduleBlocksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ScheduleBlocksViewModel.class);

        adapter = new ScheduleBlocksAdapter(block -> ConfirmDialog.show(
                requireContext(),
                R.string.schedule_block_deactivate_title,
                R.string.schedule_block_deactivate_message,
                R.string.schedule_block_deactivate_btn,
                true,
                () -> viewModel.deactivateBlock(block.id)
        ));
        binding.scheduleBlocksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.scheduleBlocksRecycler.setAdapter(adapter);

        binding.fabNewScheduleBlock.setOnClickListener(v ->
                new ScheduleBlockFormDialogFragment()
                        .show(getChildFragmentManager(), TAG_NEW_BLOCK));

        viewModel.observeActiveProfessionals().observe(getViewLifecycleOwner(), professionals -> {
            Map<String, String> names = new LinkedHashMap<>();
            if (professionals != null) {
                for (ProfessionalWithDetails item : professionals) {
                    names.put(item.professional.id, item.professional.name);
                }
            }
            adapter.setProfessionalNames(names);
        });

        viewModel.observeActiveBlocks().observe(getViewLifecycleOwner(), blocks -> {
            boolean empty = blocks == null || blocks.isEmpty();
            binding.scheduleBlocksEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.scheduleBlocksRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            adapter.submitList(empty ? null : blocks);
        });

        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.observeAffectedCount().observe(getViewLifecycleOwner(), event -> {
            Integer count = event.getContentIfNotHandled();
            if (count != null && count > 0) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.schedule_block_affected_title)
                        .setMessage(getResources().getQuantityString(
                                R.plurals.schedule_block_affected_message, count, count))
                        .setPositiveButton(R.string.common_ok, null)
                        .show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
