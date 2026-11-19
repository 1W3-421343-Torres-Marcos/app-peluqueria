package com.metanoia.app.ui.reports;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.metanoia.app.R;

import java.io.File;

/**
 * Helper liviano para diálogos de guardado SAF o compartir por {@link FileProvider} desde reportes.
 */
final class ReportExportUiHelper {

    private ReportExportUiHelper() {
    }

    static void showSaveOrShareDialog(
            @NonNull Context context,
            @NonNull String formatLabel,
            @NonNull Runnable onSaveSelected,
            @NonNull Runnable onShareSelected
    ) {
        CharSequence[] options = new CharSequence[]{
                context.getString(R.string.reports_export_option_save),
                context.getString(R.string.reports_export_option_share)
        };
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.reports_export_dialog_title, formatLabel))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        onSaveSelected.run();
                    } else {
                        onShareSelected.run();
                    }
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    static void shareGeneratedFile(
            @NonNull Context context,
            @NonNull File file,
            @NonNull String mimeType,
            @NonNull String formatLabel
    ) {
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType(mimeType);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                context.getString(R.string.reports_export_share_subject, formatLabel));
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(
                    sendIntent,
                    context.getString(R.string.reports_export_dialog_title, formatLabel)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.reports_export_no_app, Toast.LENGTH_SHORT).show();
        }
    }
}
