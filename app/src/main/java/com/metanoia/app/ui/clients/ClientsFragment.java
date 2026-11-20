package com.metanoia.app.ui.clients;

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
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.databinding.FragmentClientsBinding;
import com.metanoia.app.ui.clients.ClientsViewModel.ClientFilterMode;
import com.metanoia.app.ui.clients.importcontacts.ContactsImportDialogFragment;

/**
 * Listado, filtros y búsqueda SQL liviana de clientes del salón.
 */
public class ClientsFragment extends Fragment {

    private static final String TAG_CLIENT_FORM = "client_form_dialog";

    private FragmentClientsBinding binding;
    private ClientsViewModel viewModel;
    private ClientsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentClientsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ClientsViewModel.class);

        adapter = new ClientsAdapter(this::handleClientClick);
        binding.clientsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.clientsRecycler.setAdapter(adapter);

        setupControls();
        observeData();
    }

    private void setupControls() {
        binding.clientsSearchField.setOnQueryChangeListener(query -> viewModel.setQuery(query));
        binding.switchClientsIncludeInactive.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setIncludeInactive(isChecked));

        binding.clientsFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            ClientFilterMode mode = resolveFilterMode(
                    checkedIds.isEmpty() ? R.id.chipClientsFilterAll : checkedIds.get(0));
            viewModel.setFilterMode(mode);
            adapter.setFilterMode(mode);
            binding.switchClientsIncludeInactive.setVisibility(
                    mode == ClientFilterMode.ALL ? View.VISIBLE : View.GONE);
        });

        binding.fabNewClient.setOnClickListener(v ->
                ClientFormDialogFragment.newCreateInstance()
                        .show(getChildFragmentManager(), TAG_CLIENT_FORM));
        binding.btnImportContacts.setOnClickListener(v -> openContactsImport());

        getChildFragmentManager().setFragmentResultListener(
                ClientFormDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String type = result.getString(ClientFormDialogFragment.KEY_RESULT_TYPE);
                    String clientId = result.getString(ClientFormDialogFragment.KEY_CLIENT_ID);
                    if (ClientFormDialogFragment.RESULT_OPEN_EXISTING.equals(type)
                            && clientId != null) {
                        openClientDetail(clientId);
                    }
                }
        );
    }

    private void observeData() {
        viewModel.observeMissingBirthdayCount().observe(getViewLifecycleOwner(), count -> {
            int safeCount = count == null ? 0 : count;
            binding.chipClientsFilterMissingBirthday.setText(
                    getString(R.string.clients_filter_missing_birthday_format, safeCount));
        });

        viewModel.observeOpenBirthdayEdit().observe(getViewLifecycleOwner(), event -> {
            ClientEntity entity = event.getContentIfNotHandled();
            if (entity != null && !getChildFragmentManager().isStateSaved()) {
                ClientFormDialogFragment.newEditFocusBirthdayInstance(entity)
                        .show(getChildFragmentManager(), TAG_CLIENT_FORM);
            }
        });

        viewModel.observeClients().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.clientsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.clientsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                renderEmptyState();
                adapter.submitList(null);
            } else {
                adapter.submitList(list);
            }
        });
    }

    private void renderEmptyState() {
        boolean hasQuery = !viewModel.currentQuery().trim().isEmpty();
        ClientFilterMode mode = viewModel.currentFilterMode();
        if (hasQuery) {
            binding.clientsEmptyState.setMessage(R.string.clients_empty_search);
            binding.clientsEmptyState.setAction(null, null);
            return;
        }
        if (mode == ClientFilterMode.MISSING_BIRTHDAY) {
            binding.clientsEmptyState.setMessage(R.string.clients_empty_missing_birthday);
            binding.clientsEmptyState.setAction(null, null);
            return;
        }
        if (mode == ClientFilterMode.RECENT_CONTACT) {
            binding.clientsEmptyState.setMessage(R.string.clients_empty_recent_contacts);
            binding.clientsEmptyState.setAction(null, null);
            return;
        }
        binding.clientsEmptyState.setMessage(R.string.empty_clients_title);
        binding.clientsEmptyState.setAction(
                R.string.clients_import_contacts_btn,
                v -> openContactsImport());
    }

    private void handleClientClick(@NonNull ClientRowProjection client) {
        if (viewModel.currentFilterMode() == ClientFilterMode.MISSING_BIRTHDAY) {
            viewModel.loadClientForBirthdayEdit(client.id);
        } else {
            openClientDetail(client.id);
        }
    }

    private static ClientFilterMode resolveFilterMode(int checkedId) {
        if (checkedId == R.id.chipClientsFilterMissingBirthday) {
            return ClientFilterMode.MISSING_BIRTHDAY;
        }
        if (checkedId == R.id.chipClientsFilterRecentContacts) {
            return ClientFilterMode.RECENT_CONTACT;
        }
        return ClientFilterMode.ALL;
    }

    private void openContactsImport() {
        ContactsImportDialogFragment.newInstance()
                .show(getChildFragmentManager(), ContactsImportDialogFragment.TAG);
    }

    private void openClientDetail(@NonNull String clientId) {
        Bundle args = new Bundle();
        args.putString(ClientDetailFragment.ARG_CLIENT_ID, clientId);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_clients_to_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
