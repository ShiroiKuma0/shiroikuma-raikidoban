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
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

/**
 * Stores and resolves the user-chosen backup folder, granted through the Storage Access
 * Framework (ACTION_OPEN_DOCUMENT_TREE). The folder is the single source for the backup
 * archive list, and the target for new backups. This avoids the legacy WRITE_EXTERNAL_STORAGE
 * path which no longer works under scoped storage (targetSdk 30+).
 */
public final class BackupFolder {

    /** MIME type used when creating backup documents. */
    public static final String MIME_TYPE = "application/octet-stream";

    /**
     * Where the chosen tree Uri is remembered — in the app's default preferences, and therefore part
     * of what a backup carries (see {@code RkbExport}'s {@code prefs.json}). The grant that makes it
     * usable is not: see {@link SafFolder}.
     */
    public static final String PREF_KEY = "backup_folder_uri";

    private BackupFolder() {
    }

    /**
     * @return the persisted tree Uri if it is still readable/writable, or null otherwise (never
     * configured, or the persistable permission has been lost).
     */
    public static Uri getTreeUri(Context context) {
        Uri uri = getRememberedUri(context);
        return SafFolder.hasGrant(context, uri) ? uri : null;
    }

    /**
     * The folder the user last chose, grant or no grant — what a restored backup brings back, and
     * what the screen shows instead of "no folder selected" so the picker can open right there.
     * Never use it to write: {@link #getDir} is the only thing that says a folder is usable.
     */
    public static Uri getRememberedUri(Context context) {
        return SafFolder.parse(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_KEY, null));
    }

    /**
     * Persist the tree Uri returned by ACTION_OPEN_DOCUMENT_TREE, taking a persistable
     * read/write permission so it survives reboots.
     */
    public static void setTreeUri(Context context, Uri treeUri) {
        SafFolder.reclaim(context, treeUri);
        remember(context, treeUri);
    }

    /**
     * Write down the folder without asking for a grant — for a Uri restored from a backup, where
     * asking would throw. {@link SafFolder#reclaim} has already had its chance by then.
     */
    public static void remember(Context context, Uri treeUri) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_KEY, treeUri == null ? null : treeUri.toString())
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
