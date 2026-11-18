package com.metanoia.app.data.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.metanoia.app.data.prefs.DevicePrefs;

import java.util.List;

/**
 * Gestiona y verifica el permiso persistente SAF sobre la carpeta de backups.
 */
public final class BackupFolderAccess {

    public enum FolderState {
        NOT_CONFIGURED,
        PERMISSION_LOST,
        READY
    }

    public static final class FolderStatus {
        public final FolderState state;
        @Nullable
        public final Uri treeUri;
        @NonNull
        public final String displayName;

        public FolderStatus(
                @NonNull FolderState state,
                @Nullable Uri treeUri,
                @NonNull String displayName
        ) {
            this.state = state;
            this.treeUri = treeUri;
            this.displayName = displayName;
        }
    }

    private BackupFolderAccess() {
    }

    public static boolean persistTreePermission(
            @NonNull Context context,
            @NonNull Uri treeUri,
            @NonNull DevicePrefs prefs
    ) {
        try {
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(treeUri, flags);
            prefs.setBackupTreeUri(treeUri.toString());
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }

    @NonNull
    public static FolderStatus inspect(@NonNull Context context, @NonNull DevicePrefs prefs) {
        String rawUri = prefs.getBackupTreeUri();
        if (rawUri == null || rawUri.trim().isEmpty()) {
            return new FolderStatus(FolderState.NOT_CONFIGURED, null, "");
        }
        Uri treeUri = Uri.parse(rawUri);
        ContentResolver resolver = context.getContentResolver();
        List<UriPermission> persisted = resolver.getPersistedUriPermissions();
        boolean hasPermission = false;
        for (UriPermission perm : persisted) {
            if (perm.getUri().equals(treeUri)
                    && perm.isReadPermission()
                    && perm.isWritePermission()) {
                hasPermission = true;
                break;
            }
        }
        String fallbackName = resolveFallbackName(treeUri);
        if (!hasPermission) {
            return new FolderStatus(FolderState.PERMISSION_LOST, treeUri, fallbackName);
        }
        DocumentFile folder = DocumentFile.fromTreeUri(context, treeUri);
        if (folder == null || !folder.exists() || !folder.isDirectory() || !folder.canWrite()) {
            return new FolderStatus(FolderState.PERMISSION_LOST, treeUri, fallbackName);
        }
        String docName = folder.getName();
        String displayName = (docName == null || docName.trim().isEmpty())
                ? fallbackName
                : docName.trim();
        return new FolderStatus(FolderState.READY, treeUri, displayName);
    }

    @NonNull
    private static String resolveFallbackName(@NonNull Uri treeUri) {
        String last = treeUri.getLastPathSegment();
        if (last == null || last.trim().isEmpty()) {
            return treeUri.toString();
        }
        int colonIdx = last.indexOf(':');
        if (colonIdx >= 0 && colonIdx + 1 < last.length()) {
            return last.substring(colonIdx + 1);
        }
        return last;
    }
}
