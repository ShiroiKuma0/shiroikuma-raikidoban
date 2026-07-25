package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Shortcut;

/**
 * Resolves the app a 白い熊 自由作業盤 run-task shortcut ultimately unfreezes/launches, so the
 * "App info" / "App store" / "Uninstall" bubble entries can target that app instead of jiyusagyoban
 * itself (the shortcut's intent component is always jiyusagyoban's TaskRunActivity).
 *
 * Two sources, in order: the {@link #EXTRA_TARGET_PACKAGE} extra baked into newer shortcuts at
 * creation time, else a live ordered-broadcast query ({@link #ACTION_GET_TASK_TARGET_PACKAGE},
 * answered by jiyusagyoban's GetTaskTargetPackageReceiver — same explicit-broadcast contract as the
 * widget bridges in {@link JiyusagyobanWidgets}). When neither yields a package (old jiyusagyoban
 * build, deleted task, %var-targeted task), the callback falls back to the jiyusagyoban package —
 * i.e. today's behaviour.
 */
public class JiyuTaskTarget {

    /** Mirrors com.opentasker.widget.TaskRunActivity's activity + extras. */
    private static final String JIYU_TASK_RUN_ACTIVITY = "com.opentasker.widget.TaskRunActivity";
    private static final String EXTRA_TASK_ID = "com.opentasker.widget.TASK_ID";
    private static final String EXTRA_TASK_NAME = "com.opentasker.widget.TASK_NAME";
    private static final String EXTRA_TARGET_PACKAGE = "com.opentasker.widget.TARGET_PACKAGE";

    /** Mirrors com.opentasker.widget.GetTaskTargetPackageReceiver's contract. */
    private static final String ACTION_GET_TASK_TARGET_PACKAGE = "shiroikuma.jiyusagyoban.action.GET_TASK_TARGET_PACKAGE";
    private static final String RESULT_EXTRA_TARGET_PACKAGE = "shiroikuma.jiyusagyoban.extra.TARGET_PACKAGE";

    public interface Callback {
        /** Always called on the main thread with a non-null package (jiyusagyoban as the fallback). */
        void onTarget(String packageName);
    }

    /** The launch intent when {@code item} is a jiyusagyoban run-task shortcut, else null. */
    public static Intent getTaskIntent(Item item) {
        if (!(item instanceof Shortcut)) return null;
        Intent intent = ((Shortcut) item).getIntent();
        if (intent == null) return null;
        ComponentName cn = intent.getComponent();
        if (cn == null
                || !JiyusagyobanWidgets.JIYU_PACKAGE.equals(cn.getPackageName())
                || !JIYU_TASK_RUN_ACTIVITY.equals(cn.getClassName())) {
            return null;
        }
        // A run-task shortcut always names its task; anything else pointing at the activity is not ours.
        return (intent.hasExtra(EXTRA_TASK_NAME) || intent.hasExtra(EXTRA_TASK_ID)) ? intent : null;
    }

    /** Resolve the target app of {@code taskIntent} (from {@link #getTaskIntent}) and hand it to {@code cb}. */
    public static void resolveTargetPackage(Context context, Intent taskIntent, final Callback cb) {
        String baked = taskIntent.getStringExtra(EXTRA_TARGET_PACKAGE);
        if (!TextUtils.isEmpty(baked)) {
            cb.onTarget(baked);
            return;
        }

        Intent query = new Intent(ACTION_GET_TASK_TARGET_PACKAGE);
        query.setPackage(JiyusagyobanWidgets.JIYU_PACKAGE); // explicit — implicit cross-app broadcasts are banned on API 31+
        query.putExtra(JiyusagyobanWidgets.EXTRA_PROTOCOL, JiyusagyobanWidgets.PROTOCOL_VERSION);
        long taskId = taskIntent.getLongExtra(EXTRA_TASK_ID, -1L);
        if (taskId >= 0) query.putExtra(EXTRA_TASK_ID, taskId);
        String taskName = taskIntent.getStringExtra(EXTRA_TASK_NAME);
        if (taskName != null) query.putExtra(EXTRA_TASK_NAME, taskName);

        BroadcastReceiver ack = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String pkg = null;
                if (getResultCode() == Activity.RESULT_OK) {
                    Bundle extras = getResultExtras(false);
                    if (extras != null) pkg = extras.getString(RESULT_EXTRA_TARGET_PACKAGE);
                }
                cb.onTarget(TextUtils.isEmpty(pkg) ? JiyusagyobanWidgets.JIYU_PACKAGE : pkg);
            }
        };
        context.sendOrderedBroadcast(query, null, ack, null, Activity.RESULT_CANCELED, null, null);
    }
}
