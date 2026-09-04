package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * The gate in front of the external-automation surface — the {@link StateExportReceiver} broadcasts
 * and the {@link net.pierrox.lightning_launcher.automation.AutomationProvider} data door — as the
 * sister-app contract <b>v2</b> defines it (renrakusaki's {@code Config}, 自由作業盤 / kxkb / Kōjiki's
 * {@code AutomationAuth}).
 *
 * <p>Device-local by design: these values live in their OWN SharedPreferences file, which is not one
 * of {@link net.pierrox.lightning_launcher.data.RkbExport.Cat}'s categories, so the token never
 * travels inside a backup ZIP and never leaves the phone.
 *
 * <h3>What v2 changed, and why it had to</h3>
 *
 * v1 shipped this app <b>closed</b>: the switch defaulted to false and every request also had to
 * carry a 48-character secret 白い熊 had pasted from here into the caller. That is the wrong shape for
 * where this is going — 応用管理 restoring apps <i>and their data</i> onto a wiped phone, where
 * nothing has been configured and nobody has pasted anything. <b>A pasted secret cannot survive a
 * wipe</b>, so a gate that only works once the phone is already set up is no gate for setting one up.
 *
 * <p>Hence: {@code automation_enabled} now defaults to <b>true</b>, and the token is opt-in through
 * the new {@code automation_require_token}, default <b>false</b>. The switch stays rather than being
 * removed because it is the only way to close this app off again, and a feature that can be turned on
 * but never off is one 白い熊 cannot retreat from.
 *
 * <h3>Idempotent about the token — required, not a nicety</h3>
 *
 * <b>A token handed to an app that does not require one is IGNORED. It is never an error.</b> Tokens
 * live in task arguments and workspace variables that outlive the setting they were pasted for, and a
 * caller still sending one — because it was configured last year, or because another app on the batch
 * does want one — must be served. Refusing it would turn "白い熊 turned a switch off" into "half the
 * batch mysteriously fails", which is exactly the friction the switch exists to remove.
 *
 * <p>The whole decision lives in {@link #refuse} and nowhere else: two checks written out at each
 * entry point is how "disabled" and "bad token" drift apart across forty-two apps.
 */
public final class AutomationAuth {

    private static final String PREFS_FILE = "rkb_automation";
    private static final String KEY_ENABLED = "automation_enabled";
    private static final String KEY_REQUIRE_TOKEN = "automation_require_token";
    private static final String KEY_TOKEN = "automation_token";

    private AutomationAuth() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /** The master switch. <b>Default ON</b> (v2) — a clean phone has nothing to turn on. */
    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    /** Whether a caller must also present the token. <b>Default OFF</b> (v2). */
    public static boolean isTokenRequired(Context context) {
        return prefs(context).getBoolean(KEY_REQUIRE_TOKEN, false);
    }

    public static void setTokenRequired(Context context, boolean required) {
        prefs(context).edit().putBoolean(KEY_REQUIRE_TOKEN, required).apply();
    }

    /**
     * The one gate. Returns {@code null} to proceed, otherwise the exact {@code ERROR:} line to
     * answer with — "automation disabled" and "bad token" stay distinct because they debug
     * differently.
     *
     * <p>When the token is not required, {@code candidate} is not even looked at: that is the
     * idempotency rule above, and it is why this is a single function rather than two checks.
     */
    public static String refuse(Context context, String candidate) {
        if (!isEnabled(context)) {
            return "ERROR:automation disabled";
        }
        if (isTokenRequired(context) && !isTokenValid(context, candidate)) {
            return "ERROR:bad token";
        }
        return null;
    }

    /** The shared secret — 24 random bytes, hex; generated on first read so the row always shows one. */
    public static String token(Context context) {
        String stored = prefs(context).getString(KEY_TOKEN, null);
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return regenerateToken(context);
    }

    public static String regenerateToken(Context context) {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        String token = sb.toString();
        prefs(context).edit().putString(KEY_TOKEN, token).apply();
        return token;
    }

    /** Abbreviated form for the settings row — {@code 80922d8c…4c49a87c}. */
    public static String abbreviate(String token) {
        if (token == null) {
            return "";
        }
        if (token.length() <= 20) {
            return token;
        }
        return token.substring(0, 8) + "…" + token.substring(token.length() - 8);
    }

    /**
     * True when the caller's token matches the stored secret. Compared <b>constant-time</b>
     * ({@link MessageDigest#isEqual}) so a wrong token leaks nothing through timing — kept for the
     * case where the token <i>is</i> required.
     */
    public static boolean isTokenValid(Context context, String candidate) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                token(context).getBytes(StandardCharsets.UTF_8));
    }
}
