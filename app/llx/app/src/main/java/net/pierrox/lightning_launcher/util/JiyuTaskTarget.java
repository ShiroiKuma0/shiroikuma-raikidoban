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
import net.pierrox.lightning_launcher_extreme.R;

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

    /**
     * jiyusagyoban's status query (answered by its AutomationTargetReceiver). Never gated — a read is
     * always allowed, even on an app the user has shut down — and it carries
     * {@link #RESULT_EXTRA_STOPPED} precisely when that is true.
     */
    private static final String ACTION_QUERY_STATUS = "com.opentasker.action.QUERY_STATUS";
    private static final String RESULT_EXTRA_STOPPED = "com.opentasker.extra.STOPPED";

    /**
     * The last answer to {@link #isJiyuStopped}. Read on every item-alpha update, so it is a plain
     * cached boolean rather than a broadcast per frame: the flag changes at most a few times a day,
     * and the desktop re-probes on resume.
     */
    private static volatile boolean sStopped;

    public interface Callback {
        /** Always called on the main thread with a non-null package (jiyusagyoban as the fallback). */
        void onTarget(String packageName);
    }

    public interface StoppedCallback {
        /** Always called on the main thread. No answer counts as "not stopped". */
        void onStopped(boolean stopped);
    }

    /** The launch intent when {@code item} is a jiyusagyoban run-task shortcut, else null. */
    public static Intent getTaskIntent(Item item) {
        if (!(item instanceof Shortcut)) return null;
        Intent intent = ((Shortcut) item).getIntent();
        return isTaskIntent(intent) ? intent : null;
    }

    /** True when {@code intent} launches a jiyusagyoban task (a shortcut's, or a gesture binding's). */
    public static boolean isTaskIntent(Intent intent) {
        if (intent == null) return false;
        ComponentName cn = intent.getComponent();
        if (cn == null
                || !JiyusagyobanWidgets.JIYU_PACKAGE.equals(cn.getPackageName())
                || !JIYU_TASK_RUN_ACTIVITY.equals(cn.getClassName())) {
            return false;
        }
        // A run-task shortcut always names its task; anything else pointing at the activity is not ours.
        return intent.hasExtra(EXTRA_TASK_NAME) || intent.hasExtra(EXTRA_TASK_ID);
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

    // ---- stopped state ---------------------------------------------------------------------------

    /**
     * Ask jiyusagyoban whether the user has shut it down from its ⋮ menu ("Exit app fully"). While
     * that flag is set every task shortcut finishes with a toast and runs nothing, so the launcher
     * dims them rather than letting a tap look like it worked.
     *
     * <p>Sent exactly like {@link #resolveTargetPackage}'s query — explicit {@code setPackage},
     * protocol extra, {@code RESULT_CANCELED} as the initial code — so an old build, a missing app or
     * a receiver that simply does not answer all land on <b>not stopped</b>. That default is
     * deliberate: a launcher that dims everything because a broadcast went unanswered is worse than
     * one that lets a tap toast.
     *
     * <p>This is a read. It never starts jiyusagyoban's engine — the one thing the stop flag exists
     * to prevent.
     */
    public static void isJiyuStopped(Context context, final StoppedCallback cb) {
        Intent query = new Intent(ACTION_QUERY_STATUS);
        query.setPackage(JiyusagyobanWidgets.JIYU_PACKAGE);
        query.putExtra(JiyusagyobanWidgets.EXTRA_PROTOCOL, JiyusagyobanWidgets.PROTOCOL_VERSION);

        BroadcastReceiver ack = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                boolean stopped = false;
                if (getResultCode() == Activity.RESULT_OK) {
                    Bundle extras = getResultExtras(false);
                    if (extras != null) {
                        stopped = extras.getBoolean(RESULT_EXTRA_STOPPED, false);
                    }
                }
                sStopped = stopped;
                cb.onStopped(stopped);
            }
        };
        try {
            context.sendOrderedBroadcast(query, null, ack, null, Activity.RESULT_CANCELED, null, null);
        } catch (Exception e) {
            sStopped = false;
            cb.onStopped(false);
        }
    }

    /** The cached answer of the last {@link #isJiyuStopped} probe. False until one has answered. */
    public static boolean isStopped() {
        return sStopped;
    }

    /** True when this item is a task shortcut that currently cannot run — i.e. draw it faded. */
    public static boolean isDimmedTaskShortcut(Item item) {
        return sStopped && getTaskIntent(item) != null;
    }

    /**
     * Swallow a task launch while 自由作業盤 is stopped: say why, then open 自由作業盤 instead of firing
     * the shortcut. Wired in as the {@code Screen.LaunchInterceptor}, so a dimmed item, a folder
     * entry and a gesture binding all answer the same way. Returns true when it took the launch.
     *
     * <p>It never tries to start 自由作業盤's engine to work around the stop — opening the app is the
     * user's own way to clear the flag, and starting the engine behind their back is precisely what
     * the flag exists to prevent.
     */
    public static boolean interceptStoppedTaskLaunch(Context context, Intent intent) {
        if (!sStopped || !isTaskIntent(intent)) {
            return false;
        }
        Flash.show(context, R.string.jiyu_stopped);
        openJiyu(context);
        return true;
    }

    /**
     * Open jiyusagyoban itself — what a tap on a dimmed task shortcut does instead of firing it.
     * Opening it is also what clears the stop flag, so this is the fix as well as the explanation.
     */
    public static void openJiyu(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(JiyusagyobanWidgets.JIYU_PACKAGE);
        if (launch == null) {
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(launch);
        } catch (Exception e) {
            // pass — nothing better to offer than the flash the caller already showed
        }
    }
}
