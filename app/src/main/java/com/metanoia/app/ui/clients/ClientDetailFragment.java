package com.metanoia.app.ui.clients;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.databinding.FragmentClientDetailBinding;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailFragment;
import com.metanoia.app.ui.common.ConfirmDialog;
import com.metanoia.domain.rules.PhoneNormalizer;

import java.util.Collections;

/**
 * Ficha de cliente: datos básicos, contador de ausencias, saldo pendiente y pestañas de historial.
 */
public class ClientDetailFragment extends Fragment {

    public static final String ARG_CLIENT_ID = "client_id";
    private static final String TAG_EDIT_CLIENT = "edit_client_dialog";

    private FragmentClientDetailBinding binding;
    private ClientsViewModel viewModel;
    @Nullable
    private ClientEntity currentClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentClientDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String clientId = getArguments() == null ? null : getArguments().getString(ARG_CLIENT_ID);
        if (clientId == null || clientId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.client_detail_not_found, Toast.LENGTH_SHORT)
                    .show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        String trimmedClientId = clientId.trim();
        ClientVisitsAdapter visitsAdapter = new ClientVisitsAdapter(card -> {
            Bundle args = new Bundle();
            args.putString(AppointmentDetailFragment.ARG_APPOINTMENT_ID, card.id);
            NavHostFragment.findNavController(this).navigate(R.id.nav_appointment_detail, args);
        });
        binding.clientVisitsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.clientVisitsRecycler.setAdapter(visitsAdapter);

        viewModel = new ViewModelProvider(this).get(ClientsViewModel.class);
        viewModel.observeClientById(trimmedClientId).observe(getViewLifecycleOwner(), entity -> {
            currentClient = entity;
            if (entity != null) {
                bindClient(entity);
            }
        });
        viewModel.observeClientVisits(trimmedClientId).observe(getViewLifecycleOwner(), visits -> {
            boolean hasVisits = visits != null && !visits.isEmpty();
            visitsAdapter.submitList(hasVisits ? visits : Collections.emptyList());
            binding.clientVisitsRecycler.setVisibility(hasVisits ? View.VISIBLE : View.GONE);
            binding.clientTabVisitsEmpty.setVisibility(hasVisits ? View.GONE : View.VISIBLE);
            if (hasVisits) {
                binding.clientDetailVisitsCount.setVisibility(View.VISIBLE);
                binding.clientDetailVisitsCount.setText(
                        getString(R.string.client_detail_visits_count_format, visits.size()));
            } else {
                binding.clientDetailVisitsCount.setVisibility(View.GONE);
            }
        });
        viewModel.observeClientPendingBalanceCents(trimmedClientId).observe(
                getViewLifecycleOwner(),
                pendingCents -> binding.clientDetailBalanceValue.setMoneyCents(
                        pendingCents == null ? 0L : pendingCents));
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        getChildFragmentManager().setFragmentResultListener(
                ClientFormDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String type = result.getString(ClientFormDialogFragment.KEY_RESULT_TYPE);
                    String existingId = result.getString(ClientFormDialogFragment.KEY_CLIENT_ID);
                    if (ClientFormDialogFragment.RESULT_OPEN_EXISTING.equals(type)
                            && existingId != null) {
                        openOtherClient(existingId);
                    }
                }
        );

        binding.btnEditClient.setOnClickListener(v -> {
            if (currentClient != null) {
                ClientFormDialogFragment.newEditInstance(currentClient)
                        .show(getChildFragmentManager(), TAG_EDIT_CLIENT);
            }
        });

        binding.btnToggleClientActive.setOnClickListener(v -> toggleActiveState());
        setupTabs();
    }

    private void bindClient(@NonNull ClientEntity entity) {
        binding.clientDetailName.setText(entity.name);
        binding.clientDetailInactiveBadge.setVisibility(entity.active ? View.GONE : View.VISIBLE);
        binding.clientDetailPhone.setText(PhoneNormalizer.formatForDisplay(entity.phone));
        binding.clientDetailNoShowsValue.setText(String.valueOf(entity.noShowCount));
        binding.clientDetailBirthdayValue.setText(formatBirthday(entity));
        binding.clientDetailNotesValue.setText(
                entity.notes.isEmpty()
                        ? getString(R.string.client_detail_notes_empty)
                        : entity.notes);
        binding.btnToggleClientActive.setText(entity.active
                ? R.string.client_detail_deactivate
                : R.string.client_detail_reactivate);
    }

    private String formatBirthday(@NonNull ClientEntity entity) {
        if (entity.birthDay == null || entity.birthMonth == null) {
            return getString(R.string.client_detail_birthday_empty);
        }
        if (entity.birthYear != null) {
            return getString(R.string.client_detail_birthday_full,
                    entity.birthDay, entity.birthMonth, entity.birthYear);
        }
        return getString(R.string.client_detail_birthday_day_month,
                entity.birthDay, entity.birthMonth);
    }

    private void toggleActiveState() {
        if (currentClient == null) {
            return;
        }
        if (currentClient.active) {
            String targetId = currentClient.id;
            ConfirmDialog.show(
                    requireContext(),
                    R.string.client_deactivate_confirm_title,
                    R.string.client_deactivate_confirm_message,
                    R.string.client_detail_deactivate,
                    true,
                    () -> viewModel.setClientActive(targetId, false)
            );
        } else {
            viewModel.setClientActive(currentClient.id, true);
        }
    }

    private void setupTabs() {
        showTabPanel(0);
        binding.clientDetailTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTabPanel(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                showTabPanel(tab.getPosition());
            }
        });
    }

    private void showTabPanel(int index) {
        binding.clientTabVisitsPanel.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        binding.clientTabSpendingEmpty.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        binding.clientTabColorEmpty.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void openOtherClient(@NonNull String clientId) {
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientId);
        NavHostFragment.findNavController(this).navigate(R.id.nav_client_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
