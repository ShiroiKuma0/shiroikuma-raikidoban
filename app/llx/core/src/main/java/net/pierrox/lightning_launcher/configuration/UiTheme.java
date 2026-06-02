package net.pierrox.lightning_launcher.configuration;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Resolves a {@link UiSlot} to its effective colour / typeface / size and applies it at render time.
 *
 * Colour: the user's override if set, else a cascade — a group slot inherits its parent foundation
 * slot's resolved colour (foundation slots return their literal default). Font: any text slot inherits
 * the global chrome font (the foundation {@link UiSlot#TEXT} family / weight / size) unless it sets its
 * own. With nothing stored, every slot resolves to today's yellow-on-black look.
 */
public final class UiTheme {

    private UiTheme() {
    }

    public static int color(UiSlot slot) {
        int override = UiConfig.get().getOverride(slot.key);
        if (override != UiConfig.UNSET) {
            return override;
        }
        return defaultColor(slot);
    }

    private static int defaultColor(UiSlot slot) {
        if (slot.isFoundation) {
            return slot.defaultColor;
        }
        UiSlot parent = slot.parent();
        int base = parent != null ? color(parent) : slot.defaultColor;
        if (slot == UiSlot.PREF_SUMMARY) {
            base = adjustAlpha(base, 0.6f);
        }
        return base;
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    // --- font resolution (the global chrome font cascades from the foundation TEXT slot) ---

    public static String family(UiSlot slot) {
        UiConfig c = UiConfig.get();
        if (c.hasFontFamily(slot.key)) {
            return c.getFontFamily(slot.key);
        }
        return slot == UiSlot.TEXT ? "" : family(UiSlot.TEXT);
    }

    public static int weight(UiSlot slot) {
        UiConfig c = UiConfig.get();
        if (c.hasFontWeight(slot.key)) {
            return c.getFontWeight(slot.key);
        }
        return slot == UiSlot.TEXT ? 0 : weight(UiSlot.TEXT);
    }

    public static int size(UiSlot slot) {
        UiConfig c = UiConfig.get();
        if (c.hasFontSize(slot.key)) {
            return c.getFontSize(slot.key);
        }
        return slot == UiSlot.TEXT ? 0 : size(UiSlot.TEXT);
    }

    public static Typeface typeface(UiSlot slot, int baseStyle) {
        return UiFonts.typeface(family(slot), weight(slot), baseStyle);
    }

    // --- apply helpers ---

    /** Apply a slot's colour, and (if it is a text slot) its typeface + size, to a text view. */
    public static void applyTo(TextView tv, UiSlot slot) {
        if (tv == null) {
            return;
        }
        tv.setTextColor(color(slot));
        if (slot.hasFont) {
            applyFont(tv, slot);
        }
    }

    /** Apply only a text slot's typeface + size (keeps the view's own colour). */
    public static void applyFont(TextView tv, UiSlot slot) {
        if (tv == null) {
            return;
        }
        tv.setTypeface(typeface(slot, Typeface.NORMAL));
        int sp = size(slot);
        if (sp > 0) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }
    }

    public static void applyStatusBar(Activity activity, UiSlot slot) {
        if (activity != null) {
            activity.getWindow().setStatusBarColor(color(slot));
        }
    }
}
