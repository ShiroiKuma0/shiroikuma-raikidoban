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

import android.content.ComponentName;
import android.text.TextUtils;

import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Widget;

/**
 * Helpers for Tasker "Widget V2" widgets.
 *
 * Tasker stores each widget's internal name (e.g. "test.item.1") in its own private state, keyed
 * by the live appWidgetId. That binding is lost whenever Lightning re-allocates appWidgetIds — on a
 * crash, a host restart, or (most importantly) a layout restore from backup — so the widget must be
 * re-configured in Tasker's dialog to re-establish name -> appWidgetId.
 *
 * To survive that, Lightning records the intended Tasker name on the widget ITEM as a tag, which is
 * serialized into the page JSON and therefore travels in Lightning backups. After a restore the name
 * is right there on the item, and the re-initialization flow uses it (clipboard + Tasker config).
 */
public final class TaskerWidgets {

    /** Tasker's package. */
    public static final String TASKER_PACKAGE = "net.dinglisch.android.taskerm";

    /** The Widget V2 AppWidget provider (Glance-based). This is the {@code componentName} Lightning
     * stores for a Widget V2 instance. */
    public static final ComponentName WIDGET_V2_PROVIDER = new ComponentName(
            TASKER_PACKAGE, "com.joaomgcd.taskerwidgetv2.WidgetV2Receiver");

    /** Item tag id under which the Tasker widget name is stored. Persisted in the page JSON. */
    public static final String NAME_TAG = "rkb.taskerWidgetName";

    private TaskerWidgets() {
    }

    /** @return true if the item is a Tasker Widget V2 instance (by its AppWidget provider). */
    public static boolean isTaskerWidgetV2(Item item) {
        if (!(item instanceof Widget)) {
            return false;
        }
        ComponentName cn = ((Widget) item).getComponentName();
        return cn != null && WIDGET_V2_PROVIDER.equals(cn);
    }

    /** @return the Tasker name recorded on this item, or null if none. */
    public static String getStoredName(Item item) {
        String name = item.getTag(NAME_TAG);
        return TextUtils.isEmpty(name) ? null : name;
    }

    /** Record (or clear, when name is null/empty) the Tasker name on this item. */
    public static void setStoredName(Item item, String name) {
        item.setTag(NAME_TAG, TextUtils.isEmpty(name) ? null : name.trim());
    }

    /** @return true if this item is a Tasker widget that has a recorded name to re-initialize. */
    public static boolean hasStoredName(Item item) {
        return isTaskerWidgetV2(item) && getStoredName(item) != null;
    }
}
