package net.pierrox.lightning_launcher.configuration;

import android.content.Context;
import android.content.SharedPreferences;

import net.pierrox.lightning_launcher.LLApp;

/**
 * App-wide store for 「白い熊 雷起動盤 UI」 — the launcher's own chrome appearance (fonts + colours).
 * Mirrors the key scheme of the sister repos' theme config: a per-slot colour override (an int, where
 * {@link #UNSET} means "inherit the default / foundation"), plus a per-slot font family / weight / size
 * under stable key prefixes. Backed by a dedicated SharedPreferences file so it is independent of the
 * JSON GlobalConfig and of the translation {@code ResourcesWrapper}.
 *
 * Out of the box NOTHING is stored, so {@link UiTheme} resolves every slot to its hard-coded default
 * (today's yellow-on-black) and an untouched install looks byte-identical to before.
 */
public final class UiConfig {

    /** Stored colour sentinel meaning "no override — follow the inherited default". */
    public static final int UNSET = Integer.MIN_VALUE;
    /** Upper bound of the size seekbar, in sp. 0 still means "leave the default size". */
    public static final int MAX_FONT_SIZE_SP = 40;

    private static final String PREFS = "llui";
    private static final String FONT_FAMILY_PREFIX = "font_family_";
    private static final String FONT_WEIGHT_PREFIX = "font_weight_";
    private static final String FONT_SIZE_PREFIX = "font_size_";

    private static UiConfig sInstance;

    private final SharedPreferences mPrefs;

    private UiConfig(Context ctx) {
        mPrefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static UiConfig get() {
        if (sInstance == null) {
            sInstance = new UiConfig(LLApp.get());
        }
        return sInstance;
    }

    // --- colour override (UNSET = inherit) ---

    public int getOverride(String key) {
        return mPrefs.getInt(key, UNSET);
    }

    public void setOverride(String key, int color) {
        mPrefs.edit().putInt(key, color).apply();
    }

    public void clearOverride(String key) {
        mPrefs.edit().remove(key).apply();
    }

    public boolean hasOverride(String key) {
        return mPrefs.contains(key);
    }

    // --- font family / weight / size ---
    // A missing key means "inherit the global default font" (see UiTheme); a present key (even "")
    // is an explicit per-slot value.

    public boolean hasFontFamily(String key) {
        return mPrefs.contains(FONT_FAMILY_PREFIX + key);
    }

    public String getFontFamily(String key) {
        return mPrefs.getString(FONT_FAMILY_PREFIX + key, "");
    }

    public void setFontFamily(String key, String value) {
        mPrefs.edit().putString(FONT_FAMILY_PREFIX + key, value).apply();
    }

    public boolean hasFontWeight(String key) {
        return mPrefs.contains(FONT_WEIGHT_PREFIX + key);
    }

    public int getFontWeight(String key) {
        return mPrefs.getInt(FONT_WEIGHT_PREFIX + key, 0);
    }

    public void setFontWeight(String key, int value) {
        mPrefs.edit().putInt(FONT_WEIGHT_PREFIX + key, value).apply();
    }

    public boolean hasFontSize(String key) {
        return mPrefs.contains(FONT_SIZE_PREFIX + key);
    }

    public int getFontSize(String key) {
        return mPrefs.getInt(FONT_SIZE_PREFIX + key, 0);
    }

    public void setFontSize(String key, int value) {
        mPrefs.edit().putInt(FONT_SIZE_PREFIX + key, value).apply();
    }
}
