package com.metanoia.app.ui.waitlist;

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
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.databinding.FragmentWaitlistBinding;
import com.metanoia.app.ui.common.ConfirmDialog;
import com.metanoia.app.ui.waitlist.WaitlistAdapter.WaitlistRowItem;
import com.metanoia.domain.model.WaitlistEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla principal de lista de espera con filtro activo/historial, alta, edición y cancelación.
 */
public class WaitlistFragment extends Fragment {

    private static final String TAG_WAITLIST_FORM = "waitlist_form_dialog";

    private FragmentWaitlistBinding binding;
    private WaitlistViewModel viewModel;
    private WaitlistAdapter adapter;

    private List<WaitlistEntry> latestEntries = Collections.emptyList();
    private final Map<String, String> clientNames = new HashMap<>();
    private final Map<String, String> serviceNames = new HashMap<>();
    private final Map<String, String> professionalNames = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentWaitlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WaitlistViewModel.class);

        adapter = new WaitlistAdapter(new WaitlistAdapter.OnWaitlistActionListener() {
            @Override
            public void onEdit(@NonNull WaitlistEntry entry) {
                WaitlistFormDialogFragment.newEditInstance(entry.id())
                        .show(getChildFragmentManager(), TAG_WAITLIST_FORM);
            }

            @Override
            public void onCancel(@NonNull WaitlistEntry entry) {
                ConfirmDialog.show(
                        requireContext(),
                        R.string.waitlist_cancel_confirm_title,
                        R.string.waitlist_cancel_confirm_message,
                        R.string.waitlist_cancel_btn,
                        true,
                        () -> viewModel.cancelEntry(entry.id())
                );
            }
        });

        binding.waitlistRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.waitlistRecycler.setAdapter(adapter);

        setupControls();
        observeData();
    }

    private void setupControls() {
        binding.waitlistFilterGroup.check(R.id.btnWaitlistFilterOpen);
        binding.waitlistFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            viewModel.setOnlyOpen(checkedId == R.id.btnWaitlistFilterOpen);
        });
        binding.fabNewWaitlist.setOnClickListener(v ->
                WaitlistFormDialogFragment.newCreateInstance()
                        .show(getChildFragmentManager(), TAG_WAITLIST_FORM));
    }

    private void observeData() {
        viewModel.observeEntries().observe(getViewLifecycleOwner(), list -> {
            latestEntries = list == null ? Collections.emptyList() : list;
            renderEntries();
        });
        viewModel.observeClientsForEntries().observe(getViewLifecycleOwner(), clients -> {
            clientNames.clear();
            if (clients != null) {
                for (ClientRowProjection client : clients) {
                    clientNames.put(client.id, client.name);
                }
            }
            renderEntries();
        });
        viewModel.observeServices().observe(getViewLifecycleOwner(), services -> {
            serviceNames.clear();
            if (services != null) {
                for (SalonServiceEntity svc : services) {
                    serviceNames.put(svc.id, svc.name);
                }
            }
            renderEntries();
        });
        viewModel.observeProfessionals().observe(getViewLifecycleOwner(), professionals -> {
            professionalNames.clear();
            if (professionals != null) {
                for (ProfessionalWithDetails pro : professionals) {
                    professionalNames.put(pro.professional.id, pro.professional.name);
                }
            }
            renderEntries();
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderEntries() {
        if (binding == null || adapter == null) {
            return;
        }
        boolean empty = latestEntries.isEmpty();
        binding.waitlistEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.waitlistRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            adapter.submitList(Collections.emptyList());
            return;
        }
        List<WaitlistRowItem> rows = new ArrayList<>(latestEntries.size());
        for (WaitlistEntry entry : latestEntries) {
            String clientName = clientNames.getOrDefault(entry.clientId(), "");
            String proName = entry.professionalId()
                    .map(professionalNames::get)
                    .orElse(null);
            rows.add(new WaitlistRowItem(
                    entry,
                    clientName,
                    buildServicesSummary(entry),
                    proName
            ));
        }
        adapter.submitList(rows);
    }

    private String buildServicesSummary(@NonNull WaitlistEntry entry) {
        List<String> names = new ArrayList<>(entry.serviceIds().size());
        for (String serviceId : entry.serviceIds()) {
            String resolved = serviceNames.get(serviceId);
            if (resolved != null && !resolved.isEmpty()) {
                names.add(resolved);
            }
        }
        return String.join(" + ", names);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
