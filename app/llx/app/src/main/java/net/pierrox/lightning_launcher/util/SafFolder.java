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

/**
 * The two halves of a folder picked through the Storage Access Framework, and the one rule that
 * follows from them.
 *
 * <p>A picked tree is a Uri string — plain app data, ours to keep and to carry inside a backup —
 * <b>plus a persisted grant</b>, which is not ours at all: the platform issues it to one package on
 * one device when the picker returns, and drops it when that install goes away. No backup can carry
 * the grant, and nothing can take one for a Uri the picker never handed to this install
 * ({@code takePersistableUriPermission} throws {@link SecurityException} for anything else).
 *
 * <p>So a folder restored from a backup comes back <i>remembered</i> rather than working: the path
 * is known, it is shown instead of "not set", and it seeds the picker
 * ({@link android.provider.DocumentsContract#EXTRA_INITIAL_URI}) so a single tap in the folder
 * already on screen turns it back into a granted one. On the same device the grant is usually still
 * there, {@link #reclaim} finds it, and the restore is seamless.
 */
public final class SafFolder {

    private SafFolder() {
    }

    /** True when this install currently holds a persisted read+write grant for {@code uri}. */
    public static boolean hasGrant(Context context, Uri uri) {
        if (uri == null) {
            return false;
        }
        for (UriPermission p : context.getContentResolver().getPersistedUriPermissions()) {
            if (p.getUri().equals(uri) && p.isReadPermission() && p.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hold on to a tree this install may access — the picker has just returned it, or the grant
     * outlived a restore onto the same install.
     *
     * @return whether this install now holds a persisted grant. A Uri that came from another device
     * simply answers false, which is the caller's cue to keep it as a remembered folder instead of a
     * working one, never to drop it.
     */
    public static boolean reclaim(Context context, Uri uri) {
        if (uri == null) {
            return false;
        }
        if (hasGrant(context, uri)) {
            return true;
        }
        try {
            context.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return true;
        } catch (Exception e) {
            // Expected whenever the Uri belongs to another install: a new phone, or this app
            // reinstalled. Some providers also refuse a persistable grant, in which case the
            // session grant the picker gave us still works for this run.
            return false;
        }
    }

    /** Parse a stored tree Uri, or null when nothing is stored (or it is unparseable). */
    public static Uri parse(String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            return Uri.parse(stored);
        } catch (Exception e) {
            return null;
        }
    }
}
