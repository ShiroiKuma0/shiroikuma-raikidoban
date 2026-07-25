package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * The gate in front of the external-automation intent surface ({@link StateExportReceiver}): a master
 * switch plus a shared secret every automation broadcast must carry — the sister-app model
 * (renrakusaki's {@code Config}, 自由作業盤 / kxkb / Kōjiki's {@code AutomationAuth}).
 *
 * <p>Device-local by design: these values live in their OWN SharedPreferences file, which is not one
 * of {@link net.pierrox.lightning_launcher.data.RkbExport.Cat}'s categories, so the token never
 * travels inside a backup ZIP and never leaves the phone.
 *
 * <p>Nothing is reachable until 白い熊 flips the switch on (default <b>false</b>); every request checks
 * the switch and the token separately so "disabled" and "bad token" stay distinct, debuggable failures.
 */
public final class AutomationAuth {

    private static final String PREFS_FILE = "rkb_automation";
    private static final String KEY_ENABLED = "automation_enabled";
    private static final String KEY_TOKEN = "automation_token";

    private AutomationAuth() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
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
     * ({@link MessageDigest#isEqual}) so a wrong token leaks nothing through timing.
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
