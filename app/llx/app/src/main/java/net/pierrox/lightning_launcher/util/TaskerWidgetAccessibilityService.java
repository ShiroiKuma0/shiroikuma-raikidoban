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

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Auto-fills the Tasker "Widget V2" configuration screen during the "Reinitialize Tasker widgets"
 * flow, so the user doesn't have to type/paste the widget name.
 *
 * Tasker offers no headless way to set a widget's name (the name lives in Tasker's private state and
 * is only writable via its Compose config UI). Cross-app keystroke injection is impossible for a
 * normal app, so the only way to drive that UI is an accessibility service.
 *
 * It is deliberately tightly scoped: the service is registered for Tasker's package only (see
 * res/xml/tasker_accessibility_service.xml), and it does NOTHING unless the reinit loop has "armed"
 * it with a name via {@link #arm(String)}. When armed and Tasker's WidgetV2Configuration is showing,
 * it sets the focused text field to the name (ACTION_SET_TEXT) and confirms with the IME enter
 * action, then disarms. The clipboard+paste path in the launcher remains as a fallback when this
 * service is disabled.
 */
public class TaskerWidgetAccessibilityService extends AccessibilityService {

    private static final String TAG = "RkbTaskerA11y";
    private static final String TASKER_PACKAGE = "net.dinglisch.android.taskerm";
    private static final String CONFIG_ACTIVITY = "WidgetV2Configuration";

    /** The name to fill on the next Tasker config window, or null when not armed. */
    private static volatile String sArmedName = null;
    private static volatile boolean sConnected = false;

    private boolean mInConfigWindow = false;
    private boolean mLoggedTree = false;

    /** Arm the service to fill {@code name} into the next Tasker Widget V2 config window. */
    public static void arm(String name) {
        sArmedName = name;
    }

    public static void disarm() {
        sArmedName = null;
    }

    /** @return true if the user has enabled this accessibility service (so auto-fill is available). */
    public static boolean isConnected() {
        return sConnected;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sConnected = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sConnected = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null
                || !TASKER_PACKAGE.contentEquals(event.getPackageName())) {
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence cls = event.getClassName();
            mInConfigWindow = cls != null && cls.toString().contains(CONFIG_ACTIVITY);
            mLoggedTree = false;
        }
        if (!mInConfigWindow) {
            return;
        }

        String name = sArmedName;
        if (name == null) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        if (!mLoggedTree) {
            mLoggedTree = true;
            logTree(root, 0);
        }

        AccessibilityNodeInfo field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) {
            field = findFirstEditable(root);
        }
        if (field == null) {
            // Compose may not have rendered the field yet; wait for the next content-changed event.
            return;
        }

        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, name);
        boolean set = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

        // The confirm control is a "Save" button in the top app bar (a clickable View whose child
        // carries content-desc "Save"); the field's IME action does not submit it.
        boolean confirmed = clickConfirm(root);

        Log.d(TAG, "auto-filled '" + name + "' set=" + set + " confirmed=" + confirmed);

        // Act once per arming; if confirm failed the name is still filled and the user can tap OK.
        sArmedName = null;
    }

    /** Content-descriptions of the confirm control in Tasker's Widget V2 config (the top-bar icon). */
    private static final String[] CONFIRM_DESCS = {"Save", "Done", "OK", "Apply", "Confirm"};

    /** Find the confirm/Save control and click it (or its nearest clickable ancestor). */
    private static boolean clickConfirm(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo target = findConfirmNode(root);
        while (target != null && !target.isClickable()) {
            target = target.getParent();
        }
        return target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private static AccessibilityNodeInfo findConfirmNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String d = desc.toString().trim();
            for (String c : CONFIRM_DESCS) {
                if (c.equalsIgnoreCase(d)) {
                    return node;
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findConfirmNode(node.getChild(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static AccessibilityNodeInfo findFirstEditable(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        if (node.isEditable()) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findFirstEditable(node.getChild(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Debug aid: log editable/clickable nodes so the confirm step can be tuned to Tasker's UI. */
    private static void logTree(AccessibilityNodeInfo node, int depth) {
        if (node == null) {
            return;
        }
        if (node.isEditable() || node.isClickable()) {
            Log.d(TAG, "node d=" + depth
                    + " editable=" + node.isEditable()
                    + " clickable=" + node.isClickable()
                    + " cls=" + node.getClassName()
                    + " text=" + node.getText()
                    + " desc=" + node.getContentDescription());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            logTree(node.getChild(i), depth + 1);
        }
    }
}
