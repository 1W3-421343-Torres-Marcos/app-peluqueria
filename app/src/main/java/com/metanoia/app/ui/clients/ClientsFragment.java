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
import com.metanoia.app.databinding.FragmentClientsBinding;

/**
 * Listado y búsqueda SQL liviana de clientes del salón.
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

        adapter = new ClientsAdapter(client -> openClientDetail(client.id));
        binding.clientsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.clientsRecycler.setAdapter(adapter);

        binding.clientsSearchField.setOnQueryChangeListener(query -> viewModel.setQuery(query));
        binding.switchClientsIncludeInactive.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setIncludeInactive(isChecked));

        binding.fabNewClient.setOnClickListener(v ->
                ClientFormDialogFragment.newCreateInstance()
                        .show(getChildFragmentManager(), TAG_CLIENT_FORM));

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

        viewModel.observeClients().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.clientsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.clientsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                boolean hasQuery = !viewModel.currentQuery().trim().isEmpty();
                binding.clientsEmptyState.setMessage(hasQuery
                        ? R.string.clients_empty_search
                        : R.string.empty_clients_title);
                adapter.submitList(null);
            } else {
                adapter.submitList(list);
            }
        });
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
