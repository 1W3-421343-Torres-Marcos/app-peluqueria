package com.metanoia.app.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.DialogInitialSetupBinding;
import com.metanoia.app.ui.agenda.AgendaFragment;
import com.metanoia.app.ui.agenda.AgendaViewModel;
import com.metanoia.app.ui.clients.importcontacts.ContactsImportDialogFragment;

/**
 * Asistente inicial mostrado una única vez cuando no hay clientes cargados ni fue descartado.
 */
public class InitialSetupDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "initial_setup_dialog";
    public static final String REQUEST_KEY = "initial_setup_result";
    public static final String KEY_ACTION = "action";
    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_SETTINGS = "settings";
    public static final String ACTION_SKIP = "skip";

    private DialogInitialSetupBinding binding;

    public static void bindToAgenda(
            @NonNull AgendaFragment host,
            @NonNull AgendaViewModel viewModel,
            @NonNull LifecycleOwner owner
    ) {
        host.getChildFragmentManager().setFragmentResultListener(
                REQUEST_KEY,
                owner,
                (requestKey, result) -> {
                    String action = result.getString(KEY_ACTION, ACTION_SKIP);
                    if (ACTION_IMPORT.equals(action)) {
                        ContactsImportDialogFragment.newInstance()
                                .show(host.getChildFragmentManager(),
                                        ContactsImportDialogFragment.TAG);
                    } else if (ACTION_SETTINGS.equals(action)) {
                        NavHostFragment.findNavController(host)
                                .navigate(R.id.action_agenda_to_settings);
                    }
                });

        viewModel.observeActiveClientsCount().observe(owner, count -> {
            if (count == null || !host.isAdded()) {
                return;
            }
            DevicePrefs prefs = MetanoiaApp.container(host.requireContext()).devicePrefs();
            maybeShowIfNeeded(host, prefs, count);
        });
    }

    public static void maybeShowIfNeeded(
            @NonNull Fragment host,
            @NonNull DevicePrefs prefs,
            int activeClientsCount
    ) {
        if (prefs.isInitialSetupDismissed() || activeClientsCount > 0) {
            return;
        }
        if (host.getChildFragmentManager().isStateSaved()
                || host.getChildFragmentManager().findFragmentByTag(TAG) != null) {
            return;
        }
        new InitialSetupDialogFragment().show(host.getChildFragmentManager(), TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogInitialSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnSetupImportContacts.setOnClickListener(v -> finishWithAction(ACTION_IMPORT));
        binding.btnSetupOpenSettings.setOnClickListener(v -> finishWithAction(ACTION_SETTINGS));
        binding.btnSetupSkip.setOnClickListener(v -> finishWithAction(ACTION_SKIP));
    }

    private void finishWithAction(@NonNull String action) {
        MetanoiaApp.container(requireContext()).devicePrefs().setInitialSetupDismissed(true);
        Bundle out = new Bundle();
        out.putString(KEY_ACTION, action);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, out);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
