/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.util.List;

/**
 * Stores and resolves the user-chosen backup folder, granted through the Storage Access
 * Framework (ACTION_OPEN_DOCUMENT_TREE). The folder is the single source for the backup
 * archive list, and the target for new backups. This avoids the legacy WRITE_EXTERNAL_STORAGE
 * path which no longer works under scoped storage (targetSdk 30+).
 */
public final class BackupFolder {

    /** MIME type used when creating backup documents. */
    public static final String MIME_TYPE = "application/octet-stream";

    private static final String PREF_KEY = "backup_folder_uri";

    private BackupFolder() {
    }

    /**
     * @return the persisted tree Uri if it is still readable/writable, or null otherwise (never
     * configured, or the persistable permission has been lost).
     */
    public static Uri getTreeUri(Context context) {
        String stored = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_KEY, null);
        if (stored == null) {
            return null;
        }
        Uri uri = Uri.parse(stored);
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            if (permission.getUri().equals(uri) && permission.isReadPermission() && permission.isWritePermission()) {
                return uri;
            }
        }
        return null;
    }

    /**
     * Persist the tree Uri returned by ACTION_OPEN_DOCUMENT_TREE, taking a persistable
     * read/write permission so it survives reboots.
     */
    public static void setTreeUri(Context context, Uri treeUri) {
        context.getContentResolver().takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_KEY, treeUri.toString())
                .apply();
    }

    /**
     * @return a writable {@link DocumentFile} for the configured backup folder, or null if no
     * folder is configured or it is no longer accessible.
     */
    public static DocumentFile getDir(Context context) {
        Uri uri = getTreeUri(context);
        if (uri == null) {
            return null;
        }
        DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
        if (dir == null || !dir.isDirectory() || !dir.canWrite()) {
            return null;
        }
        return dir;
    }

    /**
     * Create a new backup document with the given display name in the configured folder,
     * replacing any existing file with the same name.
     *
     * @return the created document, or null if no folder is configured/accessible.
     */
    public static DocumentFile createDoc(Context context, String name) {
        DocumentFile dir = getDir(context);
        if (dir == null) {
            return null;
        }
        DocumentFile existing = dir.findFile(name);
        if (existing != null && existing.isFile()) {
            existing.delete();
        }
        return dir.createFile(MIME_TYPE, name);
    }
}
