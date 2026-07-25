package net.pierrox.lightning_launcher.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import net.pierrox.lightning_launcher.data.RkbExport;
import net.pierrox.lightning_launcher_extreme.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The sister-app <b>state-export automation contract</b> (保存復元) — the wire shape every 白い熊 app
 * exposes so one 自由作業盤 task can back them all up headlessly.
 *
 * <ul>
 * <li>{@code <pkg>.action.EXPORT_STATE}: run the category-ZIP export ({@link RkbExport}) with no UI.
 * Extras (all String): {@code token} (required), {@code path} (optional absolute directory; wins over
 * the configured export directory when this app may write it), {@code items} (optional comma list of
 * {@link RkbExport.Cat} ids, sub-option ids included; absent/empty = everything),
 * {@code progress_action} (optional), plus the reply trio {@code reply_action} / {@code reply_package}
 * / {@code reply_id}.</li>
 * <li>{@code <pkg>.action.LIST_CATEGORIES}: token-gated, instant enumeration for the caller's picker.
 * One {@code id<TAB>label} line per category, with a third TAB field naming the parent for a
 * sub-option (item icons / wallpapers sit under Desktops, imported fonts under UI).</li>
 * </ul>
 *
 * <p><b>ONE ZIP per request, always</b> — every category is an entry inside the single archive, named
 * {@code shiroikuma-raikidoban_<yyyy-MM-dd_HH-mm-ss>.zip} (identical to what the Export/Import panel
 * writes, and importable by it).
 *
 * <p>Reply: a FRESH broadcast to {@code reply_package} with action {@code reply_action}, extras
 * {@code reply_id} (echoed verbatim) + {@code result} = {@code OK:<path>|<bytes>|<human size>|<n>
 * categories}, {@code OK:} + the category lines, or {@code ERROR:<reason>}. Exactly one terminal
 * reply, guarded by an {@link AtomicBoolean}. NO binders (ResultReceiver/PendingIntent/Messenger) and
 * NO reliance on the ordered-broadcast result — EMUI severs both between third-party apps (verified on
 * 白い熊's Mate XT, 2026-07-23); the plain reply broadcast is the only channel that works.
 * {@link Intent#FLAG_INCLUDE_STOPPED_PACKAGES} so a backgrounded/stopped caller still hears us.
 *
 * <p>Storage: this app writes through the Storage Access Framework (no All-Files-Access), so an
 * absolute {@code path} is honoured only when 白い熊 has granted All-Files-Access anyway; otherwise the
 * configured export directory is used, and with neither the reply is {@code ERROR:no-storage-access} /
 * {@code ERROR:no-directory}.
 *
 * <p>Security: exported with NO {@code android:permission} (the caller cannot hold one) — the master
 * switch plus the token are the gate ({@link AutomationAuth}); both live on the 白い熊 雷起動盤 UI page
 * under Export / Import.
 */
public class StateExportReceiver extends BroadcastReceiver {

    public static final String ACTION_EXPORT_STATE = BuildConfig.APPLICATION_ID + ".action.EXPORT_STATE";
    public static final String ACTION_LIST_CATEGORIES = BuildConfig.APPLICATION_ID + ".action.LIST_CATEGORIES";

    // Contract extras — deliberately bare names, shared verbatim by every sister app.
    private static final String EXTRA_TOKEN = "token";
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_ITEMS = "items";
    private static final String EXTRA_PROGRESS_ACTION = "progress_action";
    private static final String EXTRA_REPLY_ACTION = "reply_action";
    private static final String EXTRA_REPLY_PACKAGE = "reply_package";
    private static final String EXTRA_REPLY_ID = "reply_id";
    private static final String EXTRA_RESULT = "result";
    private static final String EXTRA_PROGRESS_APP = "app";
    private static final String EXTRA_PROGRESS_TEXT = "text";
    private static final String EXTRA_PROGRESS_CURRENT = "current";
    private static final String EXTRA_PROGRESS_TOTAL = "total";
    private static final String EXTRA_PROGRESS_UNIT = "unit";

    private static final long PROGRESS_MIN_INTERVAL_MS = 500;
    private static final String PROGRESS_UNIT = "区分"; // categories — what this app counts

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final String action = intent.getAction();
        if (action == null) {
            return;
        }
        final String token = intent.getStringExtra(EXTRA_TOKEN);
        final String replyAction = trimmed(intent.getStringExtra(EXTRA_REPLY_ACTION));
        final String replyPackage = trimmed(intent.getStringExtra(EXTRA_REPLY_PACKAGE));
        final String replyId = trimmed(intent.getStringExtra(EXTRA_REPLY_ID));
        final String progressAction = trimmed(intent.getStringExtra(EXTRA_PROGRESS_ACTION));
        final String pathOverride = trimmed(intent.getStringExtra(EXTRA_PATH));
        final String items = trimmed(intent.getStringExtra(EXTRA_ITEMS));

        final AtomicBoolean replied = new AtomicBoolean(false);
        final Replier reply = result -> {
            if (!replied.compareAndSet(false, true)) {
                return;
            }
            if (replyAction.isEmpty() || replyPackage.isEmpty()) {
                return;
            }
            Intent out = new Intent(replyAction);
            out.setPackage(replyPackage);
            out.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            out.putExtra(EXTRA_REPLY_ID, replyId);
            out.putExtra(EXTRA_RESULT, result);
            app.sendBroadcast(out);
        };

        // Gate first — "disabled" and "bad token" are distinct on purpose (they debug differently).
        if (!AutomationAuth.isEnabled(app)) {
            reply.send("ERROR:automation disabled");
            return;
        }
        if (!AutomationAuth.isTokenValid(app, token)) {
            reply.send("ERROR:bad token");
            return;
        }

        if (ACTION_LIST_CATEGORIES.equals(action)) {
            StringBuilder sb = new StringBuilder("OK:");
            boolean first = true;
            for (RkbExport.Cat cat : RkbExport.Cat.values()) {
                if (!first) {
                    sb.append('\n');
                }
                first = false;
                sb.append(cat.id).append('\t').append(app.getString(cat.labelRes));
                if (cat.parentId != null) {
                    sb.append('\t').append(cat.parentId);
                }
            }
            reply.send(sb.toString());
            return;
        }

        if (!ACTION_EXPORT_STATE.equals(action)) {
            reply.send("ERROR:unknown action: " + action);
            return;
        }

        final Set<RkbExport.Cat> cats;
        if (items.isEmpty()) {
            cats = RkbExport.Cat.all();
        } else {
            Set<RkbExport.Cat> resolved = new LinkedHashSet<>();
            List<String> unknown = new ArrayList<>();
            for (String raw : items.split(",")) {
                String id = raw.trim();
                if (id.isEmpty()) {
                    continue;
                }
                RkbExport.Cat cat = RkbExport.Cat.byId(id);
                if (cat == null) {
                    unknown.add(id);
                } else {
                    resolved.add(cat);
                }
            }
            if (!unknown.isEmpty()) {
                reply.send("ERROR:unknown category in items: " + items);
                return;
            }
            cats = resolved;
        }

        final String appLabel = app.getString(net.pierrox.lightning_launcher_extreme.R.string.app_name);
        final String fileName = RkbExport.exportFileName();
        final long[] lastProgressMs = {0};
        final RkbExport.Progress progress = (done, total, catLabel) -> {
            if (progressAction.isEmpty() || replyPackage.isEmpty()) {
                return;
            }
            long now = System.currentTimeMillis();
            // At most one every 500 ms — but the final one always goes out.
            if (done < total && now - lastProgressMs[0] < PROGRESS_MIN_INTERVAL_MS) {
                return;
            }
            lastProgressMs[0] = now;
            Intent out = new Intent(progressAction);
            out.setPackage(replyPackage);
            out.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            out.putExtra(EXTRA_REPLY_ID, replyId);
            out.putExtra(EXTRA_PROGRESS_APP, appLabel);
            out.putExtra(EXTRA_PROGRESS_TEXT, PROGRESS_UNIT + " " + done + "/" + total + " — " + catLabel);
            out.putExtra(EXTRA_PROGRESS_CURRENT, (long) done);
            out.putExtra(EXTRA_PROGRESS_TOTAL, (long) total);
            out.putExtra(EXTRA_PROGRESS_UNIT, PROGRESS_UNIT);
            app.sendBroadcast(out);
        };

        // The export walks the whole data dir — hold the broadcast open and work off the main thread.
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                // Directory precedence: `path` extra -> configured export directory -> error. Writing
                // an arbitrary absolute path needs All-Files-Access; without it we may only fall back
                // to the configured SAF directory (contract §1).
                boolean useAbsolute = !pathOverride.isEmpty() && hasAllFilesAccess();
                DocumentFile safDir = RkbExport.exportDir(app);
                if (!pathOverride.isEmpty() && !useAbsolute && safDir == null) {
                    reply.send("ERROR:no-storage-access");
                    return;
                }
                long bytes;
                String shownPath;
                if (useAbsolute) {
                    File dir = new File(pathOverride);
                    dir.mkdirs();
                    if (!dir.isDirectory()) {
                        throw new IOException("not a directory: " + pathOverride);
                    }
                    File file = new File(dir, fileName);
                    OutputStream os = new FileOutputStream(file);
                    try {
                        RkbExport.export(app, cats, os, progress);
                    } finally {
                        os.close();
                    }
                    bytes = file.length();
                    shownPath = file.getAbsolutePath();
                } else {
                    if (safDir == null) {
                        reply.send("ERROR:no-directory");
                        return;
                    }
                    DocumentFile doc = safDir.createFile("application/zip", fileName);
                    if (doc == null) {
                        throw new IOException("cannot create " + fileName + " in the export directory");
                    }
                    OutputStream os = app.getContentResolver().openOutputStream(doc.getUri());
                    if (os == null) {
                        throw new IOException("cannot open " + fileName + " for writing");
                    }
                    try {
                        RkbExport.export(app, cats, os, progress);
                    } finally {
                        os.close();
                    }
                    bytes = doc.length();
                    String abs = RkbExport.absolutePathOf(safDir, doc.getName() == null ? fileName : doc.getName());
                    shownPath = abs != null ? abs : safDir.getName() + "/" + fileName;
                }
                reply.send("OK:" + shownPath + "|" + bytes + "|" + RkbExport.humanSize(bytes)
                        + "|" + cats.size() + " categories");
            } catch (Throwable t) {
                String message = t.getMessage();
                reply.send("ERROR:" + (message != null ? message : t.getClass().getSimpleName()));
            } finally {
                pending.finish();
            }
        }, "rkb-state-export").start();
    }

    /** All-Files-Access — required to write a caller-supplied absolute path on API 30+. */
    private static boolean hasAllFilesAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private static String trimmed(String s) {
        return s == null ? "" : s.trim();
    }

    private interface Replier {
        void send(String result);
    }
}
