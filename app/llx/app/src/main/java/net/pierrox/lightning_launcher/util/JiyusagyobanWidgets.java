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
 * jiyusagyoban accepts the binding HEADLESSLY: after Lightning re-binds a fresh appWidgetId it sends the
 * explicit ordered broadcast built by {@link #buildRestoreBindingIntent(Widget)}, and jiyusagyoban
 * persists the binding -> appWidgetId mapping and re-renders. No UI, no accessibility service.
 *
 * For <em>pull widgets</em> the name is irrelevant; the durable identity is the pull-source template
 * (see {@link #TEMPLATE_TAG}). Because the template is authored in jiyusagyoban (not here), the launcher
 * learns it via {@link #buildGetBindingIntent(Widget)} (capture) and restores it via the same
 * {@code buildRestoreBindingIntent} broadcast (now carrying {@link #EXTRA_WIDGET_TEMPLATE}).
 */
public final class JiyusagyobanWidgets {

    /** jiyusagyoban's package. Any widget whose provider lives here is a jiyusagyoban widget — this
     * covers both the Styled and the Task providers. */
    public static final String JIYU_PACKAGE = "shiroikuma.jiyusagyoban";

    /** Item tag id under which the jiyusagyoban widget name is stored. Persisted in the page JSON. */
    public static final String NAME_TAG = "rkb.jiyuWidgetName";

    /** Item tag id under which a jiyusagyoban <em>pull widget</em>'s source template name is stored.
     * For pull widgets the name is irrelevant — this template is the identity that must survive a
     * crash/restore. Authored in jiyusagyoban's config UI (not here), so the launcher learns it via
     * {@link #buildGetBindingIntent(Widget)} and mirrors it onto this tag. Persisted in the page JSON. */
    public static final String TEMPLATE_TAG = "rkb.jiyuWidgetTemplate";

    // ---- Cross-app contract (the hand-off spec jiyusagyoban implements on its side) ----

    /** Action of the explicit ordered broadcast that asks jiyusagyoban to bind a name AND/OR template to
     * an appWidgetId (the restore path). Carries {@link #EXTRA_WIDGET_NAME} and/or
     * {@link #EXTRA_WIDGET_TEMPLATE}, whichever the item has stored. */
    public static final String ACTION_SET_WIDGET_NAME = "shiroikuma.jiyusagyoban.action.SET_WIDGET_NAME";
    /** Action of the explicit ordered broadcast that asks jiyusagyoban to report a widget's current
     * binding (the capture path). jiyusagyoban replies via the ordered-broadcast result extras with
     * {@link #EXTRA_WIDGET_TEMPLATE} (and optionally {@link #EXTRA_WIDGET_NAME}). */
    public static final String ACTION_GET_WIDGET_BINDING = "shiroikuma.jiyusagyoban.action.GET_WIDGET_BINDING";
    /** String extra: the name to bind (from {@link #NAME_TAG}). */
    public static final String EXTRA_WIDGET_NAME = "shiroikuma.jiyusagyoban.extra.WIDGET_NAME";
    /** String extra: the pull-source template name (from {@link #TEMPLATE_TAG}). */
    public static final String EXTRA_WIDGET_TEMPLATE = "shiroikuma.jiyusagyoban.extra.WIDGET_TEMPLATE";
    /** String extra: the flattened provider ComponentName (Styled vs Task), for disambiguation. */
    public static final String EXTRA_PROVIDER = "shiroikuma.jiyusagyoban.extra.PROVIDER";
    /** Int extra: the contract version, so jiyusagyoban can reject unknown protocols. */
    public static final String EXTRA_PROTOCOL = "shiroikuma.jiyusagyoban.extra.PROTOCOL";
    public static final int PROTOCOL_VERSION = 1;

    /** The spelled-out widget-id key from the hand-off spec. NB the framework constant
     * {@link AppWidgetManager#EXTRA_APPWIDGET_ID} is actually the string {@code "appWidgetId"}, not
     * this — so the intent builders write the id under BOTH keys, and jiyusagyoban can read either
     * without relying on a fallback. */
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

    /** @return the jiyusagyoban pull-source template recorded on this item, or null if none. */
    public static String getStoredTemplate(Item item) {
        String tpl = item.getTag(TEMPLATE_TAG);
        return TextUtils.isEmpty(tpl) ? null : tpl;
    }

    /** Record (or clear, when template is null/empty) the jiyusagyoban pull-source template on this
     * item. Mirrored authoritatively from jiyusagyoban during capture. */
    public static void setStoredTemplate(Item item, String template) {
        item.setTag(TEMPLATE_TAG, TextUtils.isEmpty(template) ? null : template.trim());
    }

    /** @return true if this item is a jiyusagyoban widget carrying any restorable binding — a name or a
     * pull-source template. This (not {@link #hasStoredName}) is the predicate for restore, so that
     * unnamed pull widgets are restored too. */
    public static boolean hasBinding(Item item) {
        return isJiyuWidget(item) && (getStoredName(item) != null || getStoredTemplate(item) != null);
    }

    /** Build the explicit ordered broadcast that tells jiyusagyoban to restore this widget's stored
     * binding (name and/or pull-source template) onto its (live) appWidgetId. The Intent is kept
     * explicit via {@code setPackage} — implicit broadcasts to another app are banned on API 31+. */
    public static Intent buildRestoreBindingIntent(Widget w) {
        Intent intent = new Intent(ACTION_SET_WIDGET_NAME);
        intent.setPackage(JIYU_PACKAGE);
        writeIdAndProvider(intent, w);
        String name = getStoredName(w);
        if (name != null) {
            intent.putExtra(EXTRA_WIDGET_NAME, name);
        }
        String template = getStoredTemplate(w);
        if (template != null) {
            intent.putExtra(EXTRA_WIDGET_TEMPLATE, template);
        }
        return intent;
    }

    /** Build the explicit ordered broadcast that asks jiyusagyoban to report this widget's current
     * binding (the capture path). The answer comes back in the ordered-broadcast result extras. */
    public static Intent buildGetBindingIntent(Widget w) {
        Intent intent = new Intent(ACTION_GET_WIDGET_BINDING);
        intent.setPackage(JIYU_PACKAGE);
        writeIdAndProvider(intent, w);
        return intent;
    }

    private static void writeIdAndProvider(Intent intent, Widget w) {
        int appWidgetId = w.getAppWidgetId();
        // Write the id under both the framework constant ("appWidgetId") and the spelled-out spec key.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra(EXTRA_APPWIDGET_ID_SPEC, appWidgetId);
        ComponentName cn = w.getComponentName();
        if (cn != null) {
            intent.putExtra(EXTRA_PROVIDER, cn.flattenToString());
        }
        intent.putExtra(EXTRA_PROTOCOL, PROTOCOL_VERSION);
    }
}
