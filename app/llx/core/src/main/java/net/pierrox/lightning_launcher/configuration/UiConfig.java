package net.pierrox.lightning_launcher.configuration;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import net.pierrox.lightning_launcher.LLApp;

import java.util.Locale;

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
    /** Upper bound of the border-width seekbar, in dp. 0 means "no border". */
    public static final int MAX_BORDER_WIDTH_DP = 12;
    /** Upper bound of the corner-roundness seekbar, in dp. 0 means "square corners". */
    public static final int MAX_CORNER_RADIUS_DP = 28;

    private static final String PREFS = "llui";
    /** In-app UI locale: "" = follow the system, else a BCP-47 tag ("en", "ja"). */
    public static final String PREF_LOCALE = "ui_locale";
    private static final String FONT_FAMILY_PREFIX = "font_family_";
    private static final String FONT_WEIGHT_PREFIX = "font_weight_";
    private static final String FONT_SIZE_PREFIX = "font_size_";
    private static final String BORDER_WIDTH_PREFIX = "border_width_";
    private static final String CORNER_RADIUS_PREFIX = "corner_radius_";

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

    // --- border width (in dp). A missing key means "inherit the default" (see UiTheme); a present
    // key (including 0 = no border) is an explicit per-slot value. ---

    public boolean hasBorderWidth(String key) {
        return mPrefs.contains(BORDER_WIDTH_PREFIX + key);
    }

    /** Stored border width in dp, or -1 when unset (caller resolves the default). */
    public int getBorderWidth(String key) {
        return mPrefs.getInt(BORDER_WIDTH_PREFIX + key, -1);
    }

    public void setBorderWidth(String key, int dp) {
        mPrefs.edit().putInt(BORDER_WIDTH_PREFIX + key, dp).apply();
    }

    public void clearBorderWidth(String key) {
        mPrefs.edit().remove(BORDER_WIDTH_PREFIX + key).apply();
    }

    // --- corner radius (in dp). A missing key means "inherit the default" (see UiTheme); a present key
    // (including 0 = square) is an explicit per-slot value. ---

    public boolean hasCornerRadius(String key) {
        return mPrefs.contains(CORNER_RADIUS_PREFIX + key);
    }

    /** Stored corner radius in dp, or -1 when unset (caller resolves the default). */
    public int getCornerRadius(String key) {
        return mPrefs.getInt(CORNER_RADIUS_PREFIX + key, -1);
    }

    public void setCornerRadius(String key, int dp) {
        mPrefs.edit().putInt(CORNER_RADIUS_PREFIX + key, dp).apply();
    }

    public void clearCornerRadius(String key) {
        mPrefs.edit().remove(CORNER_RADIUS_PREFIX + key).apply();
    }

    // --- recently picked colours (a small shared MRU list powering the colour picker's one-tap swatches) ---

    private static final String PREF_RECENT_COLORS = "recent_colors";
    /** How many recently-picked colours to remember / show as one-tap swatches. */
    public static final int MAX_RECENT_COLORS = 8;
    // Seed the list (first run only) with the theme's own palette so the swatches are useful immediately.
    private static final int[] SEED_RECENT_COLORS = {0xFFFFFF00, 0xFFFFA500, 0xFF000000};

    /** Recently-picked colours, most-recent first. Seeded with the theme palette when nothing is stored. */
    public int[] getRecentColors() {
        String csv = mPrefs.getString(PREF_RECENT_COLORS, null);
        if (csv == null || csv.isEmpty()) {
            return SEED_RECENT_COLORS.clone();
        }
        String[] parts = csv.split(",");
        int[] out = new int[parts.length];
        int n = 0;
        for (String p : parts) {
            try {
                out[n++] = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                // skip a corrupt entry
            }
        }
        if (n == out.length) {
            return out;
        }
        int[] trimmed = new int[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    /** Record a picked colour at the front of the MRU list (de-duplicated, capped at {@link #MAX_RECENT_COLORS}). */
    public void addRecentColor(int color) {
        int[] current = getRecentColors();
        StringBuilder sb = new StringBuilder();
        sb.append(color);
        int count = 1;
        for (int c : current) {
            if (c == color || count >= MAX_RECENT_COLORS) {
                continue;
            }
            sb.append(',').append(c);
            count++;
        }
        mPrefs.edit().putString(PREF_RECENT_COLORS, sb.toString()).apply();
    }

    // --- in-app language (forced locale, independent of the system / any language pack) ---

    public String getLocaleTag() {
        return mPrefs.getString(PREF_LOCALE, "");
    }

    public void setLocaleTag(String tag) {
        // commit() (synchronous) — the language change is immediately followed by a process kill/restart,
        // so the value must be flushed to disk before exit(0); apply() would lose it.
        mPrefs.edit().putString(PREF_LOCALE, tag == null ? "" : tag).commit();
    }

    /**
     * Wrap a base context with the stored UI locale, if any. Reads SharedPreferences directly so it is
     * safe to call from {@code attachBaseContext} (before {@link LLApp#get()} is ready). Returns the
     * context unchanged when no in-app locale is set (follow the system). This is the device-independent
     * path — it does not rely on the platform per-app-locale service (absent on this HarmonyOS/API 31).
     */
    public static Context applyStoredLocale(Context base) {
        Locale locale = getStoredLocale(base);
        if (locale == null) {
            return base;
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }

    /** The stored UI locale, or null to follow the system. Reads prefs directly (attachBaseContext-safe). */
    public static Locale getStoredLocale(Context base) {
        String tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_LOCALE, "");
        return (tag == null || tag.isEmpty()) ? null : Locale.forLanguageTag(tag);
    }
}
