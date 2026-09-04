package net.pierrox.lightning_launcher.automation;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import net.pierrox.lightning_launcher.data.RkbExport;
import net.pierrox.lightning_launcher.util.AutomationAuth;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The data door: export this launcher's own state, and put it back, for a caller we can identify.
 * The v2 half of the sister-app contract, and what makes a clean-phone restore possible. It sits
 * <i>alongside</i> {@link net.pierrox.lightning_launcher.util.StateExportReceiver}, and replaces
 * nothing.
 *
 * <h3>Why a provider and not the broadcast receiver next to it</h3>
 *
 * <b>A broadcast cannot tell you who sent it.</b> v1's answer to that was the shared secret, which
 * cannot survive the wipe this feature exists to recover from. A provider gets the caller's identity
 * from the framework — see {@link AutomationCallers} for what is checked and why a package-name
 * prefix would have been worse than the token it replaced.
 *
 * <p><b>And a list needs a synchronous answer.</b> 応用管理 draws a row per installed app before any
 * export exists; a broadcast round trip per app to fill a list is the wrong shape entirely.
 *
 * <h3>What does NOT happen here</h3>
 *
 * The payload. {@link #call} validates, starts a foreground service and returns — tens of megabytes
 * over minutes inside a binder call would block the caller, report no progress, refuse cancellation
 * and die silently if this process were killed. The bytes go through a file descriptor the caller
 * opened, and the terminal answer comes back on the broadcast the family already proved on EMUI.
 *
 * <h3>Why a descriptor and not a path</h3>
 *
 * Because a backup is not a stable directory while it is being assembled. 応用管理 writes into a
 * temporary path and renames on commit; it encrypts and checksums <b>per file it knows about</b>. A
 * file this app dropped into that directory itself would be renamed out from under it, would sit in
 * plaintext inside an otherwise encrypted backup, and would be unverified rather than
 * verified-and-failing. A descriptor is also a capability that <b>expires when it is closed</b>.
 *
 * <p>It also means this app needs no {@code MANAGE_EXTERNAL_STORAGE} for the automation path — that
 * permission was only ever required because v1 handed apps an absolute path.
 *
 * <h3>{@code import} exists ONLY here</h3>
 *
 * It never gets a broadcast action. An import overwrites this launcher's desktops, and the §1
 * receiver is {@code exported="true"} with no permission — an import there would let any app on the
 * phone wipe the home screen.
 */
public class AutomationProvider extends ContentProvider {

    public static final String METHOD_DESCRIBE = "describe";
    public static final String METHOD_EXPORT = "export";
    public static final String METHOD_IMPORT = "import";
    public static final String METHOD_CANCEL = "cancel";

    public static final String KEY_RESULT = "result";
    public static final String KEY_FD = "fd";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_JOB_ID = "job_id";
    public static final String KEY_ITEMS = "items";
    public static final String KEY_REPLY_ACTION = "reply_action";
    public static final String KEY_REPLY_PACKAGE = "reply_package";
    public static final String KEY_PROGRESS_ACTION = "progress_action";

    /** This app's archive format. Bumped when an older build could no longer read what we write. */
    public static final int FORMAT = RkbExport.VERSION;

    /**
     * The oldest archive this build can still read.
     *
     * <p>Version skew has a direction: old data into a newer app is normally fine, because an app
     * migrates its own storage; newer data into an older app is not. This field is what lets a caller
     * refuse the second case at discovery time, before anything is streamed.
     */
    public static final int MIN_FORMAT_READABLE = 1;

    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * Every method answers a {@link Bundle} with {@link #KEY_RESULT} — {@code OK…} or {@code ERROR:…},
     * the same vocabulary the broadcast contract uses, so a caller has one grammar to parse, not two.
     *
     * <p><b>A refusal is returned, never thrown</b>: an exception across a binder reaches the caller
     * as a {@code RuntimeException} carrying our stack trace, which tells 白い熊 nothing and tells a
     * misbehaving caller rather more than it should.
     */
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context ctx = getContext();
        if (ctx == null) {
            return fail("ERROR:not ready");
        }
        ctx = ctx.getApplicationContext();

        // WHO, before WHAT. A caller we cannot identify gets the same answer whatever it asked for.
        String refusedCaller = AutomationCallers.verify(ctx, getCallingPackage());
        if (refusedCaller != null) {
            return fail(refusedCaller);
        }
        // Then this app's own switches — a token is ignored unless this app asks for one (§2).
        String refused = AutomationAuth.refuse(ctx, extras == null ? null : extras.getString(KEY_TOKEN));
        if (refused != null) {
            return fail(refused);
        }

        if (METHOD_DESCRIBE.equals(method)) {
            return ok(describe(ctx));
        }
        if (METHOD_EXPORT.equals(method)) {
            return start(ctx, extras, false);
        }
        if (METHOD_IMPORT.equals(method)) {
            return start(ctx, extras, true);
        }
        if (METHOD_CANCEL.equals(method)) {
            AutomationJobs.cancel(extras == null ? null : extras.getString(KEY_JOB_ID));
            return ok("OK:cancelled");
        }
        return fail("ERROR:unknown method: " + method);
    }

    /**
     * What this app would export, answered without exporting anything.
     *
     * <p>Returned from the call rather than written into the archive, deliberately: 応用管理 must draw
     * a row before an export exists, and at restore must judge compatibility <b>before</b> streaming
     * tens of megabytes into an app that would reject them — which it cannot do if the header is
     * buried inside an encrypted archive.
     */
    private String describe(Context ctx) {
        JSONObject header = new JSONObject();
        try {
            header.put("app_id", ctx.getPackageName());
            long code = 0;
            String name = "";
            try {
                PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                @SuppressWarnings("deprecation")
                int legacy = info.versionCode;
                code = legacy;
                name = info.versionName == null ? "" : info.versionName;
            } catch (Exception e) {
                // pass — a header without a version is still a usable header
            }
            header.put("version_code", code);
            header.put("version_name", name);
            header.put("format", FORMAT);
            header.put("min_format_readable", MIN_FORMAT_READABLE);
            // The import merges into the engine's base dir and flushes/clears the caches first, and
            // 応用管理 force-stops us the moment we report success — so a never-launched install is
            // fine and this app does not have to claim the exception.
            header.put("requires_launch_first", false);
            JSONArray contains = new JSONArray();
            for (RkbExport.Cat cat : RkbExport.Cat.defaults()) {
                // Top-level parts only: `contains` is a short human summary for a list row, not the
                // category picker, which LIST_CATEGORIES already answers in full.
                if (cat.parentId == null) {
                    contains.put(ctx.getString(cat.labelRes));
                }
            }
            header.put("contains", contains);
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        return "OK:" + header.toString();
    }

    /**
     * Hand the descriptor to a foreground service and get out of the way.
     *
     * <p>The descriptor is <b>duplicated</b> before it leaves this method. The one in {@code extras}
     * belongs to the binder transaction and is closed when {@code call()} returns; a service reading
     * it afterwards would find it shut. That is a bug you only see under load, so it is not left to
     * the service to remember.
     */
    private Bundle start(Context ctx, Bundle extras, boolean importing) {
        ParcelFileDescriptor fd = extras == null ? null : extras.<ParcelFileDescriptor>getParcelable(KEY_FD);
        if (fd == null) {
            return fail("ERROR:no descriptor");
        }
        ParcelFileDescriptor dup;
        try {
            dup = fd.dup();
        } catch (Exception e) {
            return fail("ERROR:descriptor unusable");
        }
        String jobId = AutomationJobs.begin();
        try {
            AutomationDataService.start(ctx, jobId, dup, importing, extras);
        } catch (Exception e) {
            AutomationJobs.finish(jobId);
            closeQuietly(dup);
            return fail("ERROR:" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
        return ok("OK:" + jobId);
    }

    private static void closeQuietly(ParcelFileDescriptor fd) {
        try {
            fd.close();
        } catch (Exception e) {
            // pass
        }
    }

    private static Bundle ok(String result) {
        Bundle b = new Bundle();
        b.putString(KEY_RESULT, result);
        return b;
    }

    private static Bundle fail(String why) {
        Bundle b = new Bundle();
        b.putString(KEY_RESULT, why);
        return b;
    }

    // A provider that is only ever call()ed still has to answer these. Refusing loudly beats
    // returning an empty cursor, which reads downstream as "there is no data" rather than "wrong
    // door".

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        throw new UnsupportedOperationException("automation is call() only");
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("automation is call() only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("automation is call() only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("automation is call() only");
    }
}
