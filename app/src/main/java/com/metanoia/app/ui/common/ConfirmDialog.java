package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.R;

/**
 * Helper para diálogos de confirmación coherentes con {@code docs/design.md} §7.
 * Las acciones destructivas resaltan su botón afirmativo con el color {@code accent}.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static void show(
            @NonNull Context context,
            @StringRes int titleRes,
            @StringRes int messageRes,
            @StringRes int positiveTextRes,
            boolean destructive,
            @NonNull Runnable onConfirm
    ) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(positiveTextRes, (d, which) -> onConfirm.run())
                .create();
        dialog.show();
        if (destructive) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(context, R.color.accent));
        }
    }
}
