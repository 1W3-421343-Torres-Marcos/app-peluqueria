package com.metanoia.app.ui.more;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.metanoia.app.BuildConfig;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.FragmentMoreBinding;

/**
 * Pantalla Más: selector de apariencia y acceso a módulos secundarios.
 */
public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DevicePrefs prefs = MetanoiaApp.container(requireContext()).devicePrefs();
        selectCurrentThemeButton(prefs.getThemeMode());
        binding.themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnThemeLight) {
                prefs.setThemeMode(DevicePrefs.THEME_LIGHT);
            } else if (checkedId == R.id.btnThemeDark) {
                prefs.setThemeMode(DevicePrefs.THEME_DARK);
            } else {
                prefs.setThemeMode(DevicePrefs.THEME_SYSTEM);
            }
        });

        binding.rowServices.setOnClickListener(v -> NavHostFragment
                .findNavController(this)
                .navigate(R.id.action_more_to_services));
        binding.rowProfessionals.setOnClickListener(v -> NavHostFragment
                .findNavController(this)
                .navigate(R.id.action_more_to_professionals));
        binding.rowSettings.setOnClickListener(v -> NavHostFragment
                .findNavController(this)
                .navigate(R.id.action_more_to_settings));
        binding.rowBackup.setOnClickListener(v -> NavHostFragment
                .findNavController(this)
                .navigate(R.id.action_more_to_backup));
        binding.rowReminders.setOnClickListener(v -> NavHostFragment
                .findNavController(this)
                .navigate(R.id.action_more_to_reminders));
        binding.rowAbout.setOnClickListener(v -> NavHostFragment
                .findNavController(this)
                .navigate(R.id.action_more_to_about));

        if (BuildConfig.DEBUG) {
            binding.rowComponentCatalog.setVisibility(View.VISIBLE);
            binding.rowComponentCatalog.setOnClickListener(v -> NavHostFragment
                    .findNavController(this)
                    .navigate(R.id.action_more_to_catalog));
        }
    }

    private void selectCurrentThemeButton(int mode) {
        int buttonId;
        if (mode == DevicePrefs.THEME_LIGHT) {
            buttonId = R.id.btnThemeLight;
        } else if (mode == DevicePrefs.THEME_DARK) {
            buttonId = R.id.btnThemeDark;
        } else {
            buttonId = R.id.btnThemeSystem;
        }
        binding.themeToggleGroup.check(buttonId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
