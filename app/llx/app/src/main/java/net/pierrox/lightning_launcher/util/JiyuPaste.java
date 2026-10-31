package net.pierrox.lightning_launcher.util;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/**
 * 「貼り付け」 — contract v1 with 白い熊 自由作業盤 (jiyusagyoban). Its 「起動作業を作る」 copies a
 * plain-text clip labelled {@link #CLIP_LABEL} whose first line is {@link #HEADER} and every further
 * line one task name. Each name is handed to jiyusagyoban's CreateTaskShortcutActivity via
 * {@link #EXTRA_TASK_NAME}, which skips its picker and returns the same shortcut result as a manual
 * pick (RESULT_CANCELED, silently, for an unknown name). The clipboard is deliberately never cleared.
 * Used by the add-item dialog (Dashboard) and the "Select action" chooser (EventActionSetup).
 */
public final class JiyuPaste {
    private static final String CLIP_LABEL = "白い熊 自由作業盤 task shortcut";
    private static final String HEADER = "shiroikuma.jiyusagyoban/task-shortcut/1";
    private static final String EXTRA_TASK_NAME = "shiroikuma.jiyusagyoban.extra.TASK_NAME";
    private static final String CREATE_SHORTCUT_ACTIVITY = "com.opentasker.widget.CreateTaskShortcutActivity";

    private JiyuPaste() {
    }

    /** The task names on the clipboard, or null when the clip is not a jiyusagyoban task list. The
     * description label is checked first so a foreign clip is never read. */
    public static ArrayList<String> readNames(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return null;
        ClipDescription desc = cm.getPrimaryClipDescription();
        if (desc == null || desc.getLabel() == null || !CLIP_LABEL.equals(desc.getLabel().toString())) {
            return null;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence text = clip.getItemAt(0).coerceToText(context);
        if (text == null) return null;
        String[] lines = text.toString().split("\\n");
        if (lines.length == 0 || !HEADER.equals(lines[0].trim())) return null;
        ArrayList<String> names = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String name = lines[i].trim();
            if (!name.isEmpty()) names.add(name);
        }
        return names.isEmpty() ? null : names;
    }

    /** The CREATE_SHORTCUT request that makes jiyusagyoban return the shortcut for {@code taskName}. */
    public static Intent createShortcutRequest(String taskName) {
        Intent i = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        i.setComponent(new ComponentName(JiyusagyobanWidgets.JIYU_PACKAGE, CREATE_SHORTCUT_ACTIVITY));
        i.putExtra(EXTRA_TASK_NAME, taskName);
        return i;
    }
}
