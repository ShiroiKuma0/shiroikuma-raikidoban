package net.pierrox.lightning_launcher.automation;

import android.content.Context;
import android.content.Intent;

import net.pierrox.lightning_launcher.data.RkbExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The <b>one</b> §3 progress sender, shared by both automation doors.
 *
 * <p>There used to be two — one in {@link net.pierrox.lightning_launcher.util.StateExportReceiver}
 * for the broadcast contract, one inside {@link AutomationDataService} for the provider. The contract
 * is explicit that an app which already has a §1 sender must <b>parameterise that one on the
 * correlation-id extra rather than write a second</b>, because two implementations of the same
 * watchdog drift, and the one that drifts is always the one nobody is looking at. This is that one.
 *
 * <p>The only difference between the two doors is which extra carries the correlation id: the
 * broadcast door echoes {@code reply_id}, the provider door hands out a {@code job_id}. So the id is
 * written into <b>every</b> name in {@link #correlationExtras} — the provider passes both, so one
 * progress reader on the caller's side serves both doors.
 *
 * <h3>The heartbeat is the point, not a nicety</h3>
 *
 * 自由作業盤 treats every progress broadcast as proof the app is still alive and <b>presumes an app
 * silent for two minutes to be dead</b>, failing its slot. {@link RkbExport} only calls back when a
 * whole category is finished, and one category here can be every item icon or every desktop
 * wallpaper — easily longer than that on a large desktop. So a background thread re-sends the last
 * line every {@link #HEARTBEAT_MS} even when the numbers have not moved: a healthy export of a big
 * desktop must not be declared dead for being busy.
 *
 * <p>Inert when the caller passed no {@code progress_action} (or no reply package to aim it at), so
 * both callers can construct one unconditionally.
 */
public final class AutomationProgress implements RkbExport.Progress {

    /** What this app counts: categories. */
    private static final String UNIT = "区分";
    private static final long MIN_INTERVAL_MS = 500;
    /** Comfortably inside §3's 30 s floor, itself well inside the caller's two-minute patience. */
    private static final long HEARTBEAT_MS = 20_000;
    private static final long HEARTBEAT_TICK_MS = 5_000;

    /** Bytes written so far, when the caller's destination is one we count. */
    public interface Bytes {
        long written();
    }

    private final Context context;
    private final String action;
    private final String replyPackage;
    private final String[] correlationExtras;
    private final String correlationId;
    private final String appLabel;
    /** Not final: the import only learns which categories it is restoring after it has read the
     * archive, and it must keep ONE sender across both phases rather than build a second. */
    private volatile List<RkbExport.Cat> ordered;
    private final Bytes bytes;
    private final boolean active;

    private volatile String lastItem;
    private volatile String lastText;
    private volatile long lastCurrent;
    private volatile long lastTotal;
    private volatile long lastSentMs;
    private volatile boolean running;
    private Thread heartbeat;

    public AutomationProgress(Context context, String action, String replyPackage,
                              String correlationId, String[] correlationExtras, String appLabel,
                              Set<RkbExport.Cat> cats, Bytes bytes) {
        this.context = context.getApplicationContext();
        this.action = action == null ? "" : action.trim();
        this.replyPackage = replyPackage == null ? "" : replyPackage.trim();
        this.correlationExtras = correlationExtras;
        this.correlationId = correlationId == null ? "" : correlationId;
        this.appLabel = appLabel;
        this.ordered = ordered(cats);
        this.bytes = bytes;
        // A progress_action without a reply_package is not a weak progress channel, it is none at
        // all: since API 26 an implicit broadcast never reaches a manifest receiver, and every
        // broadcast we send must therefore carry setPackage.
        this.active = !this.action.isEmpty() && !this.replyPackage.isEmpty();
    }

    /**
     * Narrow the category list once it is actually known — the import resolves it only after the
     * archive has been read, and building a second sender for the second phase is exactly what this
     * class exists to prevent.
     */
    public void setCategories(Set<RkbExport.Cat> cats) {
        this.ordered = ordered(cats);
    }

    /** The order {@link RkbExport#export} walks them in, so a count can name the category it means. */
    public static List<RkbExport.Cat> ordered(Set<RkbExport.Cat> cats) {
        List<RkbExport.Cat> list = new ArrayList<>();
        for (RkbExport.Cat c : RkbExport.Cat.values()) {
            if (cats.contains(c)) {
                list.add(c);
            }
        }
        return list;
    }

    /** Begin the heartbeat. Safe to call on an inert sender (it does nothing). */
    public void start() {
        if (!active || running) {
            return;
        }
        running = true;
        lastSentMs = System.currentTimeMillis();
        heartbeat = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(HEARTBEAT_TICK_MS);
                } catch (InterruptedException e) {
                    return;
                }
                if (!running || lastText == null) {
                    continue;
                }
                if (System.currentTimeMillis() - lastSentMs >= HEARTBEAT_MS) {
                    // Same numbers, sent again: "still here", which is all the caller needs.
                    emit(lastItem, lastText, lastCurrent, lastTotal);
                }
            }
        }, "rkb-automation-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    /** Stop the heartbeat. Always call this in a {@code finally}. */
    public void stop() {
        running = false;
        Thread t = heartbeat;
        if (t != null) {
            t.interrupt();
            heartbeat = null;
        }
    }

    @Override
    public void onProgress(int done, int total, String categoryLabel) {
        if (!active) {
            return;
        }
        // At most one every 500 ms — but the final one always goes out.
        long now = System.currentTimeMillis();
        if (done < total && now - lastSentMs < MIN_INTERVAL_MS) {
            return;
        }
        // WHICH row is running: the panel cannot work that out from a bare count, and an app that
        // sent only numbers once put a four-digit count against a nine-row list and ticked nothing.
        String item = (done >= 1 && done <= ordered.size()) ? ordered.get(done - 1).id : null;
        emit(item, UNIT + " " + done + "/" + total + " — " + categoryLabel, done, total);
    }

    /**
     * A free-form line for a step that has no honest count of its own — the import, which
     * {@link RkbExport} does not call back through. It exists for the heartbeat's sake: the caller
     * needs to keep hearing something, and inventing category numbers we are not actually walking
     * would be worse than sending none.
     */
    public void note(String text) {
        if (!active) {
            return;
        }
        emit(null, text, 0, 0);
    }

    private void emit(String item, String text, long current, long total) {
        lastItem = item;
        lastText = text;
        lastCurrent = current;
        lastTotal = total;
        lastSentMs = System.currentTimeMillis();

        Intent out = new Intent(action);
        out.setPackage(replyPackage);
        // Without this a backgrounded caller never hears us, and on a clean phone the caller may not
        // have been launched at all.
        out.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        // The same id under every name the two doors use, so one reader serves both.
        for (String extra : correlationExtras) {
            out.putExtra(extra, correlationId);
        }
        out.putExtra("app", appLabel);
        if (item != null) {
            out.putExtra("item", item);
        }
        out.putExtra("text", text);
        out.putExtra("current", current);
        out.putExtra("total", total);
        out.putExtra("unit", UNIT);
        if (bytes != null) {
            out.putExtra("bytes", bytes.written());
        }
        context.sendBroadcast(out);
    }
}
