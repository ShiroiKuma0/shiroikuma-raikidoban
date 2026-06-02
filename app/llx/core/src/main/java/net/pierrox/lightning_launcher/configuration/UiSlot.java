package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * One customizable chrome element. Foundation slots own a literal default colour (today's
 * yellow-on-black) and are the cascade roots; every group slot inherits its COLOUR from a parent
 * foundation slot (via {@link #parentKey}) until given an explicit override. FONT, for any text slot,
 * always inherits from the foundation {@link #TEXT} slot (the single global chrome font) unless the
 * slot sets its own family / weight / size. See {@link UiTheme} for resolution.
 */
public enum UiSlot {

    // --- Foundation (cascade roots; literal defaults = today's look) ---
    BACKGROUND("llui_background", UiGroup.FOUNDATION, R.string.llui_background, true, false, null, 0xFF000000),
    TEXT("llui_text", UiGroup.FOUNDATION, R.string.llui_text, true, true, null, 0xFFFFFF00),
    ACCENT("llui_accent", UiGroup.FOUNDATION, R.string.llui_accent, true, false, null, 0xFFFFA500),

    // --- Launcher menus (bubble) ---
    MENU_TEXT("llui_menu_text", UiGroup.MENUS, R.string.llui_menu_text, false, true, "llui_text", 0),
    MENU_BG("llui_menu_bg", UiGroup.MENUS, R.string.llui_menu_bg, false, false, "llui_background", 0),
    MENU_ACCENT("llui_menu_accent", UiGroup.MENUS, R.string.llui_menu_accent, false, false, "llui_accent", 0),

    // --- Custom dialogs (Gestures / Fold matrix / pickers / flashes) ---
    DIALOG_TEXT("llui_dialog_text", UiGroup.DIALOGS, R.string.llui_dialog_text, false, true, "llui_text", 0),
    DIALOG_BG("llui_dialog_bg", UiGroup.DIALOGS, R.string.llui_dialog_bg, false, false, "llui_background", 0),
    DIALOG_TITLE("llui_dialog_title", UiGroup.DIALOGS, R.string.llui_dialog_title, false, true, "llui_accent", 0),
    DIALOG_BUTTON("llui_dialog_button", UiGroup.DIALOGS, R.string.llui_dialog_button, false, false, "llui_accent", 0),

    // --- Settings & Customize preference pages ---
    PREF_TITLE("llui_pref_title", UiGroup.SETTINGS, R.string.llui_pref_title, false, true, "llui_text", 0),
    PREF_SUMMARY("llui_pref_summary", UiGroup.SETTINGS, R.string.llui_pref_summary, false, true, "llui_text", 0),
    PREF_CATEGORY("llui_pref_category", UiGroup.SETTINGS, R.string.llui_pref_category, false, true, "llui_accent", 0),
    PREF_BG("llui_pref_bg", UiGroup.SETTINGS, R.string.llui_pref_bg, false, false, "llui_background", 0),

    // --- Toolbars + status bar ---
    TOOLBAR_BG("llui_toolbar_bg", UiGroup.TOOLBARS, R.string.llui_toolbar_bg, false, false, "llui_background", 0),
    TOOLBAR_TEXT("llui_toolbar_text", UiGroup.TOOLBARS, R.string.llui_toolbar_text, false, true, "llui_accent", 0),
    TOOLBAR_ICON("llui_toolbar_icon", UiGroup.TOOLBARS, R.string.llui_toolbar_icon, false, false, "llui_accent", 0),
    STATUSBAR_BG("llui_statusbar_bg", UiGroup.TOOLBARS, R.string.llui_statusbar_bg, false, false, "llui_background", 0);

    public final String key;
    public final UiGroup group;
    public final int labelRes;
    public final boolean isFoundation;
    /** True for concrete text elements: family / weight / size are configurable. */
    public final boolean hasFont;
    /** Key of the foundation slot this one inherits its COLOUR from (null for foundation slots). */
    public final String parentKey;
    /** Literal default colour for foundation slots (ignored for group slots, which inherit). */
    public final int defaultColor;

    UiSlot(String key, UiGroup group, int labelRes, boolean isFoundation, boolean hasFont,
           String parentKey, int defaultColor) {
        this.key = key;
        this.group = group;
        this.labelRes = labelRes;
        this.isFoundation = isFoundation;
        this.hasFont = hasFont;
        this.parentKey = parentKey;
        this.defaultColor = defaultColor;
    }

    public UiSlot parent() {
        return parentKey == null ? null : byKey(parentKey);
    }

    public static UiSlot byKey(String key) {
        for (UiSlot s : values()) {
            if (s.key.equals(key)) {
                return s;
            }
        }
        return null;
    }

    public static List<UiSlot> forGroup(UiGroup group) {
        List<UiSlot> out = new ArrayList<>();
        for (UiSlot s : values()) {
            if (s.group == group) {
                out.add(s);
            }
        }
        return out;
    }
}
