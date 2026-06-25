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

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.text.TextUtils;

import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Widget;

/**
 * Helpers for 白い熊 自由作業盤 (jiyusagyoban) widgets — the same crash/restore name-survival problem
 * Tasker widgets have, but solved cleanly because jiyusagyoban is ours.
 *
 * jiyusagyoban stores each widget's name (e.g. "test-jiyu") in its own private state, keyed by the
 * live appWidgetId. That binding is lost whenever Lightning re-allocates appWidgetIds — on a crash, a
 * host restart, or (most importantly) a layout restore from backup. To survive that, Lightning records
 * the intended name on the widget ITEM as a tag, which is serialized into the page JSON and therefore
 * travels in Lightning backups.
 *
 * Unlike Tasker (third-party, only configurable through its Compose UI, hence the accessibility hack),
 * jiyusagyoban accepts the name HEADLESSLY: after Lightning re-binds a fresh appWidgetId it sends the
 * explicit ordered broadcast built by {@link #buildSetNameIntent(Widget)}, and jiyusagyoban persists
 * the name -> appWidgetId mapping and re-renders. No UI, no accessibility service.
 */
public final class JiyusagyobanWidgets {

    /** jiyusagyoban's package. Any widget whose provider lives here is a jiyusagyoban widget — this
     * covers both the Styled and the Task providers. */
    public static final String JIYU_PACKAGE = "shiroikuma.jiyusagyoban";

    /** Item tag id under which the jiyusagyoban widget name is stored. Persisted in the page JSON. */
    public static final String NAME_TAG = "rkb.jiyuWidgetName";

    // ---- Cross-app contract (the hand-off spec jiyusagyoban implements on its side) ----

    /** Action of the explicit ordered broadcast that asks jiyusagyoban to bind a name to an appWidgetId. */
    public static final String ACTION_SET_WIDGET_NAME = "shiroikuma.jiyusagyoban.action.SET_WIDGET_NAME";
    /** String extra: the name to bind (from {@link #NAME_TAG}). */
    public static final String EXTRA_WIDGET_NAME = "shiroikuma.jiyusagyoban.extra.WIDGET_NAME";
    /** String extra: the flattened provider ComponentName (Styled vs Task), for disambiguation. */
    public static final String EXTRA_PROVIDER = "shiroikuma.jiyusagyoban.extra.PROVIDER";
    /** Int extra: the contract version, so jiyusagyoban can reject unknown protocols. */
    public static final String EXTRA_PROTOCOL = "shiroikuma.jiyusagyoban.extra.PROTOCOL";
    public static final int PROTOCOL_VERSION = 1;

    /** The spelled-out widget-id key from the hand-off spec. NB the framework constant
     * {@link AppWidgetManager#EXTRA_APPWIDGET_ID} is actually the string {@code "appWidgetId"}, not
     * this — so {@link #buildSetNameIntent(Widget)} writes the id under BOTH keys, and jiyusagyoban can
     * read either without relying on a fallback. */
    public static final String EXTRA_APPWIDGET_ID_SPEC = "android.appwidget.extra.APPWIDGET_ID";

    /** Ordered-broadcast result code jiyusagyoban returns when it can't apply the name yet (data not
     * loaded). Lightning treats anything other than {@link Activity#RESULT_OK} as a failure, and the
     * initial code is {@link Activity#RESULT_CANCELED}, so a missing/old jiyusagyoban counts as a
     * failure automatically. */
    public static final int RESULT_NOT_READY = Activity.RESULT_FIRST_USER;

    private JiyusagyobanWidgets() {
    }

    /** @return true if the item is a jiyusagyoban widget (by its provider package). */
    public static boolean isJiyuWidget(Item item) {
        if (!(item instanceof Widget)) {
            return false;
        }
        ComponentName cn = ((Widget) item).getComponentName();
        return cn != null && JIYU_PACKAGE.equals(cn.getPackageName());
    }

    /** @return the jiyusagyoban name recorded on this item, or null if none. */
    public static String getStoredName(Item item) {
        String name = item.getTag(NAME_TAG);
        return TextUtils.isEmpty(name) ? null : name;
    }

    /** Record (or clear, when name is null/empty) the jiyusagyoban name on this item. */
    public static void setStoredName(Item item, String name) {
        item.setTag(NAME_TAG, TextUtils.isEmpty(name) ? null : name.trim());
    }

    /** @return true if this item is a jiyusagyoban widget that has a recorded name to re-initialize. */
    public static boolean hasStoredName(Item item) {
        return isJiyuWidget(item) && getStoredName(item) != null;
    }

    /** Build the explicit ordered broadcast that tells jiyusagyoban to bind this widget's stored name to
     * its (live) appWidgetId. The Intent is kept explicit via {@code setPackage} — implicit broadcasts
     * to another app are banned on API 31+. */
    public static Intent buildSetNameIntent(Widget w) {
        Intent intent = new Intent(ACTION_SET_WIDGET_NAME);
        intent.setPackage(JIYU_PACKAGE);
        int appWidgetId = w.getAppWidgetId();
        // Write the id under both the framework constant ("appWidgetId") and the spelled-out spec key.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra(EXTRA_APPWIDGET_ID_SPEC, appWidgetId);
        intent.putExtra(EXTRA_WIDGET_NAME, getStoredName(w));
        ComponentName cn = w.getComponentName();
        if (cn != null) {
            intent.putExtra(EXTRA_PROVIDER, cn.flattenToString());
        }
        intent.putExtra(EXTRA_PROTOCOL, PROTOCOL_VERSION);
        return intent;
    }
}
