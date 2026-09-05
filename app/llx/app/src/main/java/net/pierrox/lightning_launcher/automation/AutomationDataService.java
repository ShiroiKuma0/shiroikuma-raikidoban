package net.pierrox.lightning_launcher.automation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;

import net.pierrox.lightning_launcher.data.RkbExport;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Where a data export or import started through {@link AutomationProvider} actually runs.
 *
 * <h3>Why a foreground service and not the provider call</h3>
 *
 * The call returns in milliseconds; this can run for minutes. Two hard reasons it cannot be done
 * anywhere cheaper:
 *
 * <ul>
 * <li><b>A binder call holds the caller.</b> 応用管理 is drawing a list; a multi-minute synchronous
 * call would freeze its UI, report no progress and refuse cancellation.</li>
 * <li><b>A backgrounded app writing for minutes is frozen mid-stream on this phone</b>, which yields
 * a truncated archive underneath a success reply — the worst possible failure, because it is
 * indistinguishable from a good backup until the day it is restored.</li>
 * </ul>
 *
 * <p>EMUI additionally force-releases a background app's partial wakelock seconds after it starts, so
 * one is held here for the duration and released in the {@code finally}.
 *
 * <h3>The descriptor has an owner from the moment it leaves the map</h3>
 *
 * It was duplicated by {@link AutomationProvider} before it got here, because the original belongs to
 * the binder transaction and is closed the moment {@code call()} returns. Taking it out of
 * {@link #HANDOVER} and calling {@code startForeground} now happen inside <b>one</b>
 * {@code try}/{@code finally}, because {@code startForeground} can fail: a service started from a
 * binder call is a <i>background</i> start, and API 31+ can refuse it outright with
 * {@code ForegroundServiceStartNotAllowedException} unless the app is exempt from battery
 * optimisation. On a phone where 雷起動盤 is not exempt that refusal is the ordinary path, not an
 * exotic one — and before this, it left the caller's file open with no reply ever sent, so the caller
 * could neither checksum nor encrypt it and was told nothing.
 */
public class AutomationDataService extends Service {

    private static final String CHANNEL = "automation_data";
    private static final int NOTIFICATION_ID = 9714;
    private static final String EXTRA_JOB = "job";
    private static final String EXTRA_IMPORTING = "importing";

    /** Long enough for a desktop full of icons and wallpapers, short enough not to strand the CPU. */
    private static final long WAKELOCK_TIMEOUT_MS = 15 * 60 * 1000L;

    /**
     * The descriptor's way across, because an Intent is the wrong vehicle for one.
     *
     * <p>A {@link ParcelFileDescriptor} in an Intent extra is duplicated by the system on delivery
     * and the copy's lifetime stops being ours to reason about. Handing it through a map keyed by the
     * job id keeps exactly one open descriptor with exactly one owner.
     */
    private static final ConcurrentHashMap<String, ParcelFileDescriptor> HANDOVER = new ConcurrentHashMap<>();

    public static void start(Context context, String jobId, ParcelFileDescriptor fd, boolean importing,
                             Bundle extras) {
        HANDOVER.put(jobId, fd);
        Intent intent = new Intent(context, AutomationDataService.class);
        intent.putExtra(EXTRA_JOB, jobId);
        intent.putExtra(EXTRA_IMPORTING, importing);
        if (extras != null) {
            intent.putExtra(AutomationProvider.KEY_ITEMS, extras.getString(AutomationProvider.KEY_ITEMS));
            intent.putExtra(AutomationProvider.KEY_REPLY_ACTION,
                    extras.getString(AutomationProvider.KEY_REPLY_ACTION));
            intent.putExtra(AutomationProvider.KEY_REPLY_PACKAGE,
                    extras.getString(AutomationProvider.KEY_REPLY_PACKAGE));
            intent.putExtra(AutomationProvider.KEY_PROGRESS_ACTION,
                    extras.getString(AutomationProvider.KEY_PROGRESS_ACTION));
        }
        try {
            context.startForegroundService(intent);
        } catch (RuntimeException e) {
            // Never strand the descriptor if the service could not be started at all.
            HANDOVER.remove(jobId);
            throw e;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        final String jobId = intent == null ? null : intent.getStringExtra(EXTRA_JOB);
        if (jobId == null) {
            return stop(startId);
        }
        final boolean importing = intent.getBooleanExtra(EXTRA_IMPORTING, false);
        final String items = intent.getStringExtra(AutomationProvider.KEY_ITEMS);
        final String replyAction = intent.getStringExtra(AutomationProvider.KEY_REPLY_ACTION);
        final String replyPackage = intent.getStringExtra(AutomationProvider.KEY_REPLY_PACKAGE);
        final String progressAction = intent.getStringExtra(AutomationProvider.KEY_PROGRESS_ACTION);

        // Built BEFORE anything that can throw, so a failure to even start still gets reported
        // instead of leaving the caller waiting on a reply that can never come.
        final AtomicBoolean replied = new AtomicBoolean(false);
        final Replier reply = result -> {
            // Exactly one terminal answer per job, whatever path got here — a synchronous failure and
            // an asynchronous success must never both fire. The same guard the broadcast contract has
            // carried since the first sister app.
            if (!replied.compareAndSet(false, true)) {
                return;
            }
            AutomationJobs.finish(jobId);
            if (replyAction == null || replyAction.isEmpty()
                    || replyPackage == null || replyPackage.isEmpty()) {
                return;
            }
            Intent out = new Intent(replyAction);
            out.setPackage(replyPackage);
            // Without this a backgrounded caller never hears the answer, and on a clean phone the
            // caller may not have been launched at all.
            out.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            // The same id under both names, so one reader on the caller's side serves both doors.
            out.putExtra(AutomationProvider.KEY_JOB_ID, jobId);
            out.putExtra(AutomationProvider.KEY_REPLY_ID, jobId);
            out.putExtra(AutomationProvider.KEY_RESULT, result);
            sendBroadcast(out);
        };

        final ParcelFileDescriptor fd = HANDOVER.remove(jobId);
        if (fd == null) {
            AutomationJobs.finish(jobId);
            return stop(startId);
        }

        // From here the descriptor is ours and MUST be closed on every path out.
        boolean started = false;
        try {
            // MUST be within 5 s of the service starting, or the system kills us for that alone —
            // and it can be refused outright, which is what the finally below exists for.
            startForeground(NOTIFICATION_ID, notification(importing));

            new Thread(() -> {
                PowerManager.WakeLock wakeLock = null;
                try {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                "raikidoban:automation-data");
                        wakeLock.acquire(WAKELOCK_TIMEOUT_MS);
                    }
                    if (importing) {
                        runImport(jobId, fd, items, progressAction, replyPackage, reply);
                    } else {
                        runExport(jobId, fd, items, progressAction, replyPackage, reply);
                    }
                } catch (RkbExport.CancelledException e) {
                    reply.send("ERROR:cancelled");
                } catch (Throwable t) {
                    String message = t.getMessage();
                    reply.send("ERROR:" + (message != null ? message : t.getClass().getSimpleName()));
                } finally {
                    closeQuietly(fd);
                    if (wakeLock != null && wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                    stopForeground(true);
                    stopSelf(startId);
                }
            }, "rkb-automation-data").start();
            started = true;
        } catch (Throwable t) {
            String message = t.getMessage();
            reply.send("ERROR:" + (message != null ? message : t.getClass().getSimpleName()));
        } finally {
            if (!started) {
                // The worker never took ownership, so the descriptor is still ours to close.
                closeQuietly(fd);
                try {
                    stopForeground(true);
                } catch (Throwable ignored) {
                    // we may never have been foreground at all
                }
                stopSelf(startId);
            }
        }

        return START_NOT_STICKY;
    }

    // ---------------------------------------------------------------------------------------------
    // export
    // ---------------------------------------------------------------------------------------------

    private void runExport(final String jobId, ParcelFileDescriptor fd, String items,
                           String progressAction, String replyPackage, Replier reply)
            throws IOException {
        final Set<RkbExport.Cat> cats = resolve(items);
        if (cats == null) {
            reply.send("ERROR:unknown category in items: " + items);
            return;
        }

        final long[] written = {0};
        // The shared §3 sender. The correlation id is this job's id, written into BOTH `job_id` and
        // `reply_id` so one progress reader on the caller's side serves this door and the broadcast
        // one alike. It carries the heartbeat too, so an export spending a minute inside one category
        // of icons is not presumed dead.
        AutomationProgress progress = new AutomationProgress(this, progressAction, replyPackage,
                jobId, new String[]{AutomationProvider.KEY_JOB_ID, AutomationProvider.KEY_REPLY_ID},
                getString(R.string.app_name), cats, () -> written[0]);
        progress.start();

        OutputStream raw = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
        try {
            // Counted as it goes rather than stat'ed afterwards: the caller owns the file and we may
            // not be able to see it at all — it can be an anonymous pipe, or a descriptor into a
            // directory this app cannot list.
            OutputStream counting = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    raw.write(b);
                    written[0]++;
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    raw.write(b, off, len);
                    written[0] += len;
                }
            };
            RkbExport.export(this, cats, counting, progress, () -> AutomationJobs.isCancelled(jobId));
            counting.flush();
        } finally {
            progress.stop();
            raw.close();
        }

        if (AutomationJobs.isCancelled(jobId)) {
            reply.send("ERROR:cancelled");
        } else {
            reply.send("OK:" + written[0] + "|" + RkbExport.humanSize(written[0])
                    + "|" + cats.size() + " categories");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // import — the half that exists ONLY behind the provider
    // ---------------------------------------------------------------------------------------------

    /**
     * Spool the archive to a cache file, validate it there, then apply it.
     *
     * <p>Reading the whole archive before touching anything is deliberate: a partial read that failed
     * half way would import half an archive, and a half-restored launcher is worse than one that
     * refused. But "read it all first" must not mean "hold it all in RAM" — this app's backup carries
     * every item icon and every desktop wallpaper, so it is exactly the archive that gets big, and a
     * big enough one used to be an {@code OutOfMemoryError} in the middle of a restore. The guarantee
     * is unchanged; only the bound moves from memory to disk.
     */
    private void runImport(String jobId, ParcelFileDescriptor fd, String items,
                           String progressAction, String replyPackage, Replier reply)
            throws IOException {
        File spool = new File(getCacheDir(), "automation-import-" + jobId + ".zip");
        // The import has no per-category callback to report through, so this exists for the
        // heartbeat: the caller must keep hearing something, and inventing counts we are not walking
        // would be worse than sending none.
        AutomationProgress progress = new AutomationProgress(this, progressAction, replyPackage,
                jobId, new String[]{AutomationProvider.KEY_JOB_ID, AutomationProvider.KEY_REPLY_ID},
                getString(R.string.app_name), RkbExport.Cat.defaults(), null);
        progress.start();
        try {
            progress.note(getString(R.string.rkb_auto_notif_import));

            long total = 0;
            InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd);
            try {
                OutputStream out = new FileOutputStream(spool);
                try {
                    byte[] chunk = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(chunk)) != -1) {
                        out.write(chunk, 0, read);
                        total += read;
                    }
                    out.flush();
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

            if (total == 0) {
                reply.send("ERROR:empty archive");
                return;
            }

            // Every category the archive actually carries, not every category we know about: asking
            // for one the archive lacks is how a restore ends up reporting success over nothing.
            List<String> present = RkbExport.categoriesIn(spool);
            if (present.isEmpty()) {
                reply.send("ERROR:archive carries no categories");
                return;
            }
            Set<RkbExport.Cat> wanted = resolve(items);
            if (wanted == null) {
                reply.send("ERROR:unknown category in items: " + items);
                return;
            }
            Set<RkbExport.Cat> cats = new LinkedHashSet<>();
            for (String id : present) {
                RkbExport.Cat cat = RkbExport.Cat.byId(id);
                if (cat != null && wanted.contains(cat)) {
                    cats.add(cat);
                }
            }
            if (cats.isEmpty()) {
                reply.send("ERROR:archive carries none of the requested categories");
                return;
            }

            RkbExport.importZip(this, spool, cats);
            // 応用管理 force-stops us straight after this, and that belongs on its side: a running
            // process writes its cached SharedPreferences back out at orderly shutdown and would
            // silently undo the import that just happened.
            reply.send("OK:" + cats.size() + " categories restored");
        } finally {
            progress.stop();
            // Never leave it behind: it is a complete copy of the desktop's backup sitting in cache.
            if (spool.exists() && !spool.delete()) {
                spool.deleteOnExit();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** {@code null} means an id in {@code items} is not one of ours — the caller gets told which. */
    private static Set<RkbExport.Cat> resolve(String items) {
        if (items == null || items.trim().isEmpty()) {
            return RkbExport.Cat.defaults();
        }
        Set<RkbExport.Cat> resolved = new LinkedHashSet<>();
        for (String raw : items.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            RkbExport.Cat cat = RkbExport.Cat.byId(id);
            if (cat == null) {
                return null;
            }
            resolved.add(cat);
        }
        return resolved.isEmpty() ? RkbExport.Cat.defaults() : resolved;
    }

    private Notification notification(boolean importing) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    getString(R.string.rkb_auto_notif_channel), NotificationManager.IMPORTANCE_LOW));
        }
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle(getString(importing
                        ? R.string.rkb_auto_notif_import : R.string.rkb_auto_notif_export))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .build();
    }

    private static void closeQuietly(ParcelFileDescriptor fd) {
        try {
            fd.close();
        } catch (Exception e) {
            // pass — an already-closed descriptor is the normal case here
        }
    }

    private int stop(int startId) {
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private interface Replier {
        void send(String result);
    }
}
