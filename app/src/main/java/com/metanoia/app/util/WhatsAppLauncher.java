package com.metanoia.app.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.R;
import com.metanoia.domain.rules.WhatsAppUrlBuilder;

import java.util.Optional;

/**
 * Abre chats de WhatsApp mediante {@code https://wa.me/<tel>?text=...} sin permiso INTERNET.
 */
public final class WhatsAppLauncher {

    private WhatsAppLauncher() {
    }

    public static boolean hasValidPhone(@Nullable String phoneE164) {
        return WhatsAppUrlBuilder.hasValidPhone(phoneE164);
    }

    public static boolean openChat(
            @NonNull Context context,
            @Nullable String phoneE164,
            @NonNull String message
    ) {
        Optional<String> urlOpt = WhatsAppUrlBuilder.buildUrl(phoneE164, message);
        if (!urlOpt.isPresent()) {
            Toast.makeText(context, R.string.reminder_no_phone_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlOpt.get()));
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
