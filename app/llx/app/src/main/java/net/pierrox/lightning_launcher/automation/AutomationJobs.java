package net.pierrox.lightning_launcher.automation;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The jobs the data door has started, and the flag each of them watches to stop.
 *
 * <p>One at a time is not enforced here — a caller asking for two exports at once is asking for two
 * files, and refusing that is {@link AutomationDataService}'s business. What this owns is the mapping
 * from the id a caller was handed to a cancellation it can act on, which must outlive the binder call
 * that created it and be reachable from a service that never saw the caller.
 *
 * <p>Process-local and never persisted, deliberately: a persisted "running" flag survives the crash
 * that stranded it and wedges the app for good.
 */
public final class AutomationJobs {

    private static final ConcurrentHashMap<String, Boolean> sCancelled = new ConcurrentHashMap<>();

    private AutomationJobs() {
    }

    public static String begin() {
        String id = UUID.randomUUID().toString();
        sCancelled.put(id, Boolean.FALSE);
        return id;
    }

    /**
     * Ask a job to stop. A no-op for an id that is finished or was never real.
     *
     * <p>Deliberately silent: a cancel arriving after the work completed is the normal race, not an
     * error, and answering it as one would make every well-behaved caller look broken.
     */
    public static void cancel(String jobId) {
        if (jobId != null) {
            sCancelled.replace(jobId, Boolean.TRUE);
        }
    }

    /** Polled at write boundaries — never mid-write, so a cancelled archive is never half a file. */
    public static boolean isCancelled(String jobId) {
        return Boolean.TRUE.equals(sCancelled.get(jobId));
    }

    public static void finish(String jobId) {
        if (jobId != null) {
            sCancelled.remove(jobId);
        }
    }
}
