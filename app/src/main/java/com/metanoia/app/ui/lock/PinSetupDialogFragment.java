package com.metanoia.app.ui.lock;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.databinding.DialogPinSetupBinding;
import com.metanoia.domain.rules.PinHasher;

/**
 * Diálogo reutilizable para activar, cambiar o desactivar el PIN local de seguridad.
 */
public class PinSetupDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "pin_setup_result";
    private static final String ARG_MODE = "arg_mode";

    public enum Mode {
        CREATE,
        CHANGE,
        DISABLE
    }

    private DialogPinSetupBinding binding;
    private Mode mode = Mode.CREATE;
    private boolean busy;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static PinSetupDialogFragment newInstance(@NonNull Mode mode) {
        PinSetupDialogFragment fragment = new PinSetupDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode.name());
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String rawMode = getArguments() == null ? null : getArguments().getString(ARG_MODE);
        if (rawMode != null) {
            mode = Mode.valueOf(rawMode);
        }
        binding = DialogPinSetupBinding.inflate(getLayoutInflater());
        configureFields();

        int titleRes = R.string.security_pin_setup_create_title;
        if (mode == Mode.CHANGE) {
            titleRes = R.string.security_pin_setup_change_title;
        } else if (mode == Mode.DISABLE) {
            titleRes = R.string.security_pin_setup_disable_title;
        }

        int positiveRes = mode == Mode.DISABLE
                ? R.string.security_pin_disable_btn
                : R.string.security_pin_save_btn;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(positiveRes, null)
                .create();
        dialog.setOnShowListener(d ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(v -> onSubmit(dialog)));
        return dialog;
    }

    private void configureFields() {
        boolean needCurrent = mode == Mode.CHANGE || mode == Mode.DISABLE;
        boolean needNew = mode == Mode.CREATE || mode == Mode.CHANGE;
        binding.tilCurrentPin.setVisibility(needCurrent ? View.VISIBLE : View.GONE);
        binding.tilNewPin.setVisibility(needNew ? View.VISIBLE : View.GONE);
        binding.tilConfirmPin.setVisibility(needNew ? View.VISIBLE : View.GONE);
    }

    private void onSubmit(@NonNull AlertDialog dialog) {
        if (busy) {
            return;
        }
        binding.tilCurrentPin.setError(null);
        binding.tilNewPin.setError(null);
        binding.tilConfirmPin.setError(null);

        String currentPin = textOf(binding.inputCurrentPin);
        String newPin = textOf(binding.inputNewPin);
        String confirmPin = textOf(binding.inputConfirmPin);

        if ((mode == Mode.CHANGE || mode == Mode.DISABLE)
                && !PinHasher.isValidPinFormat(currentPin)) {
            binding.tilCurrentPin.setError(getString(R.string.security_pin_error_format));
            return;
        }
        if (mode == Mode.CREATE || mode == Mode.CHANGE) {
            if (!PinHasher.isValidPinFormat(newPin)) {
                binding.tilNewPin.setError(getString(R.string.security_pin_error_format));
                return;
            }
            if (!newPin.equals(confirmPin)) {
                binding.tilConfirmPin.setError(getString(R.string.security_pin_error_mismatch));
                return;
            }
        }

        AppContainer container = MetanoiaApp.container(requireContext());
        DevicePrefs prefs = container.devicePrefs();
        busy = true;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

        container.executors().io().execute(() -> {
            boolean currentValid = mode == Mode.CREATE || PinHasher.verify(
                    currentPin,
                    prefs.getPinSalt(),
                    prefs.getPinHash(),
                    prefs.getPinIterations());
            if (!currentValid) {
                mainHandler.post(() -> showWrongCurrentPin(dialog));
                return;
            }
            if (mode == Mode.DISABLE) {
                prefs.clearPin();
                container.lockController().markUnlocked();
            } else {
                PinHasher.HashedPin hashed = PinHasher.hash(newPin);
                prefs.savePinHash(
                        hashed.saltBase64(), hashed.hashBase64(), hashed.iterations());
                container.lockController().onPinEnabledNow();
            }
            mainHandler.post(this::finishSuccess);
        });
    }

    private void showWrongCurrentPin(@NonNull AlertDialog dialog) {
        if (!isAdded() || binding == null) {
            return;
        }
        busy = false;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        binding.tilCurrentPin.setError(getString(R.string.security_pin_error_wrong_current));
    }

    private void finishSuccess() {
        if (!isAdded()) {
            return;
        }
        busy = false;
        int feedbackRes = mode == Mode.DISABLE
                ? R.string.security_pin_disabled_msg
                : R.string.security_pin_saved;
        Toast.makeText(requireContext(), feedbackRes, Toast.LENGTH_SHORT).show();
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, Bundle.EMPTY);
        dismissAllowingStateLoss();
    }

    @NonNull
    private static String textOf(@NonNull com.google.android.material.textfield.TextInputEditText et) {
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
