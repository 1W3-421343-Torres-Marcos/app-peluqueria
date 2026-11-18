package com.metanoia.app.ui.debug;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.metanoia.app.AppContainer;
import com.metanoia.app.BuildConfig;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.FragmentComponentCatalogBinding;
import com.metanoia.app.ui.common.ConfirmDialog;

/**
 * Pantalla de catálogo visual de componentes base (accesible solo en builds debug).
 */
public class ComponentCatalogFragment extends Fragment {

    private FragmentComponentCatalogBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentComponentCatalogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DevicePrefs prefs = MetanoiaApp.container(requireContext()).devicePrefs();
        int nightFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        binding.catalogThemeToggle.check(nightFlags == Configuration.UI_MODE_NIGHT_YES
                ? R.id.btnCatalogDark
                : R.id.btnCatalogLight);

        binding.catalogThemeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            prefs.setThemeMode(checkedId == R.id.btnCatalogDark
                    ? DevicePrefs.THEME_DARK
                    : DevicePrefs.THEME_LIGHT);
        });

        binding.moneySampleNegative.setMoneyCents(-345_000L);

        View.OnClickListener sampleAction = v -> Toast.makeText(
                requireContext(), R.string.catalog_action_feedback, Toast.LENGTH_SHORT).show();
        binding.btnCatalogPrimary.setOnClickListener(sampleAction);
        binding.btnCatalogSecondary.setOnClickListener(sampleAction);
        binding.btnCatalogText.setOnClickListener(sampleAction);
        binding.catalogAlertBanner.setAction(R.string.catalog_banner_action, sampleAction);
        binding.catalogEmptyState.setAction(R.string.catalog_empty_action, sampleAction);

        if (BuildConfig.DEBUG) {
            AppContainer appContainer = MetanoiaApp.container(requireContext());
            binding.catalogDebugSectionTitle.setVisibility(View.VISIBLE);
            binding.btnCatalogSeedVolume.setVisibility(View.VISIBLE);
            binding.btnCatalogSeedVolume.setOnClickListener(v -> ConfirmDialog.show(
                    requireContext(),
                    R.string.debug_seed_confirm_title,
                    R.string.debug_seed_confirm_message,
                    R.string.debug_seed_confirm_positive,
                    false,
                    () -> runVolumeSeeder(appContainer)
            ));
        }
    }

    private void runVolumeSeeder(@NonNull AppContainer container) {
        if (binding == null) {
            return;
        }
        binding.btnCatalogSeedVolume.setEnabled(false);
        Toast.makeText(requireContext(), R.string.debug_seed_running, Toast.LENGTH_SHORT).show();
        container.executors().io().execute(() -> {
            DebugSeeder.SeedResult result = DebugSeeder.seedDefaultVolume(container);
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.btnCatalogSeedVolume.setEnabled(true);
                if (result.appointmentsCount > 0) {
                    Toast.makeText(
                            requireContext(),
                            getString(
                                    R.string.debug_seed_success,
                                    result.clientsCount,
                                    result.appointmentsCount,
                                    result.elapsedMs
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    Toast.makeText(requireContext(), R.string.debug_seed_error, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
