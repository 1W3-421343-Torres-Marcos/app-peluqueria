package com.metanoia.app.ui.lock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;

import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.FragmentSecuritySettingsBinding;

/**
 * Pantalla de configuración de bloqueo local por PIN, biometría y tiempo en segundo plano.
 */
public class SecuritySettingsFragment extends Fragment {

    private static final int BIOMETRIC_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.BIOMETRIC_WEAK;
    private static final String TAG_PIN_SETUP = "pin_setup_dialog";

    private FragmentSecuritySettingsBinding binding;
    private DevicePrefs prefs;
    private boolean bindingUi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = MetanoiaApp.container(requireContext()).devicePrefs();

        getChildFragmentManager().setFragmentResultListener(
                PinSetupDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (key, bundle) -> refreshState());

        binding.btnSecurityEnablePin.setOnClickListener(v ->
                PinSetupDialogFragment.newInstance(PinSetupDialogFragment.Mode.CREATE)
                        .show(getChildFragmentManager(), TAG_PIN_SETUP));
        binding.btnSecurityChangePin.setOnClickListener(v ->
                PinSetupDialogFragment.newInstance(PinSetupDialogFragment.Mode.CHANGE)
                        .show(getChildFragmentManager(), TAG_PIN_SETUP));
        binding.btnSecurityDisablePin.setOnClickListener(v ->
                PinSetupDialogFragment.newInstance(PinSetupDialogFragment.Mode.DISABLE)
                        .show(getChildFragmentManager(), TAG_PIN_SETUP));

        binding.switchSecurityBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingUi) {
                prefs.setBiometricEnabled(isChecked);
                refreshState();
            }
        });

        binding.securityTimeoutGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!bindingUi && isChecked) {
                        prefs.setLockTimeoutMinutes(timeoutForButtonId(checkedId));
                    }
                });

        refreshState();
    }

    private void refreshState() {
        if (binding == null) {
            return;
        }
        bindingUi = true;
        boolean pinEnabled = prefs.isPinEnabled();
        binding.securityPinStatusText.setText(pinEnabled
                ? R.string.security_pin_status_enabled
                : R.string.security_pin_status_disabled);
        binding.btnSecurityEnablePin.setVisibility(pinEnabled ? View.GONE : View.VISIBLE);
        binding.securityPinActionsRow.setVisibility(pinEnabled ? View.VISIBLE : View.GONE);

        int biometricStatus = BiometricManager.from(requireContext())
                .canAuthenticate(BIOMETRIC_AUTHENTICATORS);
        boolean biometricReady = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS;
        binding.switchSecurityBiometric.setEnabled(pinEnabled && biometricReady);
        binding.switchSecurityBiometric.setChecked(
                pinEnabled && biometricReady && prefs.isBiometricEnabled());

        if (!pinEnabled) {
            binding.securityBiometricHelperText.setText(R.string.security_biometric_need_pin);
            binding.securityBiometricHelperText.setVisibility(View.VISIBLE);
        } else if (!biometricReady) {
            binding.securityBiometricHelperText.setText(R.string.security_biometric_unavailable);
            binding.securityBiometricHelperText.setVisibility(View.VISIBLE);
        } else {
            binding.securityBiometricHelperText.setVisibility(View.GONE);
        }

        int timeoutBtn = buttonIdForTimeout(prefs.getLockTimeoutMinutes());
        if (binding.securityTimeoutGroup.getCheckedButtonId() != timeoutBtn) {
            binding.securityTimeoutGroup.check(timeoutBtn);
        }
        setTimeoutButtonsEnabled(pinEnabled);
        bindingUi = false;
    }

    private void setTimeoutButtonsEnabled(boolean enabled) {
        binding.btnTimeoutImmediate.setEnabled(enabled);
        binding.btnTimeout1m.setEnabled(enabled);
        binding.btnTimeout5m.setEnabled(enabled);
        binding.btnTimeout15m.setEnabled(enabled);
    }

    private static int buttonIdForTimeout(int minutes) {
        if (minutes == 0) {
            return R.id.btnTimeoutImmediate;
        }
        if (minutes == 1) {
            return R.id.btnTimeout1m;
        }
        if (minutes == 15) {
            return R.id.btnTimeout15m;
        }
        return R.id.btnTimeout5m;
    }

    private static int timeoutForButtonId(int checkedId) {
        if (checkedId == R.id.btnTimeoutImmediate) {
            return 0;
        }
        if (checkedId == R.id.btnTimeout1m) {
            return 1;
        }
        if (checkedId == R.id.btnTimeout15m) {
            return 15;
        }
        return 5;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
