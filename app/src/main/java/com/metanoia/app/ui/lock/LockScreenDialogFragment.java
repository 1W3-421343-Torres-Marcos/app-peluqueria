package com.metanoia.app.ui.lock;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.DialogLockScreenBinding;
import com.metanoia.domain.rules.PinHasher;
import com.metanoia.domain.rules.PinLockPolicy;

import java.util.concurrent.Executor;

/**
 * Pantalla modal de desbloqueo por PIN (PBKDF2) o biometría al abrir o volver de segundo plano.
 */
public class LockScreenDialogFragment extends DialogFragment {

    public static final String TAG = "metanoia_lock_screen";
    private static final int BIOMETRIC_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.BIOMETRIC_WEAK;
    private static final long COOLDOWN_TICK_MS = 1_000L;

    private DialogLockScreenBinding binding;
    private final StringBuilder enteredPin = new StringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ImageView[] dots;
    private boolean verifying;
    private boolean biometricPromptedOnce;
    private final Runnable cooldownTicker = new Runnable() {
        @Override
        public void run() {
            if (!isAdded() || binding == null) {
                return;
            }
            if (refreshLockoutState()) {
                mainHandler.postDelayed(this, COOLDOWN_TICK_MS);
            }
        }
    };

    public static LockScreenDialogFragment newInstance() {
        return new LockScreenDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Theme_Metanoia);
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                requireActivity().moveTaskToBack(true);
                return true;
            }
            return false;
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogLockScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dots = new ImageView[]{
                binding.lockDot1, binding.lockDot2, binding.lockDot3,
                binding.lockDot4, binding.lockDot5, binding.lockDot6
        };
        bindKeypad();
        updatePinDots();
        refreshBiometricAvailability();
        refreshLockoutState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (refreshLockoutState()) {
            mainHandler.removeCallbacks(cooldownTicker);
            mainHandler.postDelayed(cooldownTicker, COOLDOWN_TICK_MS);
        } else if (!biometricPromptedOnce && canUseBiometric()) {
            biometricPromptedOnce = true;
            launchBiometricPrompt();
        }
    }

    private void bindKeypad() {
        View.OnClickListener digitListener = v -> {
            Object tag = v.getTag();
            if (tag instanceof String) {
                appendDigit((String) tag);
            }
        };
        View[] digitButtons = new View[]{
                binding.btnDigit0, binding.btnDigit1, binding.btnDigit2, binding.btnDigit3,
                binding.btnDigit4, binding.btnDigit5, binding.btnDigit6, binding.btnDigit7,
                binding.btnDigit8, binding.btnDigit9
        };
        for (int i = 0; i < digitButtons.length; i++) {
            digitButtons[i].setTag(String.valueOf(i));
            digitButtons[i].setOnClickListener(digitListener);
        }
        binding.btnLockBackspace.setOnClickListener(v -> removeLastDigit());
        binding.btnLockSubmit.setOnClickListener(v -> submitPin());
        binding.btnLockBiometric.setOnClickListener(v -> launchBiometricPrompt());
    }

    private void appendDigit(@NonNull String digit) {
        if (verifying || enteredPin.length() >= PinHasher.MAX_PIN_LENGTH) {
            return;
        }
        enteredPin.append(digit);
        binding.lockErrorText.setVisibility(View.INVISIBLE);
        updatePinDots();
        refreshSubmitEnabled();
    }

    private void removeLastDigit() {
        if (verifying || enteredPin.length() == 0) {
            return;
        }
        enteredPin.deleteCharAt(enteredPin.length() - 1);
        updatePinDots();
        refreshSubmitEnabled();
    }

    private void updatePinDots() {
        int count = enteredPin.length();
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageResource(i < count ? R.drawable.ic_dot_filled : R.drawable.ic_dot_empty);
        }
        binding.lockDotsContainer.setContentDescription(
                getString(R.string.security_lock_dots_cd_format, count));
    }

    private void refreshBiometricAvailability() {
        binding.btnLockBiometric.setVisibility(canUseBiometric() ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean canUseBiometric() {
        AppContainer container = MetanoiaApp.container(requireContext());
        if (!container.devicePrefs().isBiometricEnabled()) {
            return false;
        }
        int status = BiometricManager.from(requireContext())
                .canAuthenticate(BIOMETRIC_AUTHENTICATORS);
        return status == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void launchBiometricPrompt() {
        if (!canUseBiometric() || refreshLockoutState()) {
            return;
        }
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(
                this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (!isAdded()) {
                    return;
                }
                MetanoiaApp.container(requireContext()).lockController().markUnlocked();
                dismissAllowingStateLoss();
            }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.security_biometric_prompt_title))
                .setSubtitle(getString(R.string.security_biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.security_biometric_use_pin))
                .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
                .build();
        prompt.authenticate(info);
    }

    private void submitPin() {
        if (verifying || refreshLockoutState()) {
            return;
        }
        String candidate = enteredPin.toString();
        if (!PinHasher.isValidPinFormat(candidate)) {
            return;
        }
        AppContainer container = MetanoiaApp.container(requireContext());
        DevicePrefs prefs = container.devicePrefs();
        verifying = true;
        refreshSubmitEnabled();

        container.executors().io().execute(() -> {
            boolean ok = PinHasher.verify(
                    candidate,
                    prefs.getPinSalt(),
                    prefs.getPinHash(),
                    prefs.getPinIterations());
            long now = container.clock().nowMillis();
            if (ok) {
                container.lockController().markUnlocked();
            } else {
                prefs.recordFailedPinAttempt(now);
            }
            mainHandler.post(() -> handlePinVerificationResult(ok));
        });
    }

    private void handlePinVerificationResult(boolean ok) {
        if (!isAdded() || binding == null) {
            return;
        }
        verifying = false;
        if (ok) {
            enteredPin.setLength(0);
            dismissAllowingStateLoss();
            return;
        }
        enteredPin.setLength(0);
        updatePinDots();
        if (refreshLockoutState()) {
            mainHandler.removeCallbacks(cooldownTicker);
            mainHandler.postDelayed(cooldownTicker, COOLDOWN_TICK_MS);
        } else {
            binding.lockErrorText.setText(R.string.security_lock_wrong_pin);
            binding.lockErrorText.setVisibility(View.VISIBLE);
        }
        refreshSubmitEnabled();
    }

    private boolean refreshLockoutState() {
        AppContainer container = MetanoiaApp.container(requireContext());
        long remainingMs = PinLockPolicy.remainingLockoutMs(
                container.devicePrefs().getPinLockedUntilMillis(),
                container.clock().nowMillis());
        boolean lockedOut = remainingMs > 0L;
        if (lockedOut) {
            long seconds = Math.max(1L, (remainingMs + 999L) / 1_000L);
            binding.lockErrorText.setText(
                    getString(R.string.security_lock_cooldown_format, seconds));
            binding.lockErrorText.setVisibility(View.VISIBLE);
        } else if (binding.lockErrorText.getVisibility() == View.VISIBLE
                && getString(R.string.security_lock_wrong_pin)
                        .equals(binding.lockErrorText.getText().toString())) {
            binding.lockErrorText.setVisibility(View.VISIBLE);
        } else if (!lockedOut) {
            binding.lockErrorText.setVisibility(View.INVISIBLE);
        }
        refreshSubmitEnabled();
        return lockedOut;
    }

    private void refreshSubmitEnabled() {
        AppContainer container = MetanoiaApp.container(requireContext());
        long remainingMs = PinLockPolicy.remainingLockoutMs(
                container.devicePrefs().getPinLockedUntilMillis(),
                container.clock().nowMillis());
        binding.btnLockSubmit.setEnabled(
                !verifying
                        && remainingMs <= 0L
                        && enteredPin.length() >= PinHasher.MIN_PIN_LENGTH);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        enteredPin.setLength(0);
        binding = null;
    }
}
