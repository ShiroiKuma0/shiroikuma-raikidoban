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
    // Title defaults to TEXT (yellow), not the accent — a yellow title reads better on the black panel.
    DIALOG_TITLE("llui_dialog_title", UiGroup.DIALOGS, R.string.llui_dialog_title, false, true, "llui_text", 0),
    DIALOG_BUTTON("llui_dialog_button", UiGroup.DIALOGS, R.string.llui_dialog_button, false, true, "llui_accent", 0),
    // Frame around dialog panels (shortcut picker, action chooser, the action list, the Backup/Restore
    // dialogs, …). Colour cascades from TEXT (yellow by default); its WIDTH and corner RADIUS are stored
    // separately (see UiConfig; defaults {@link UiTheme#DEFAULT_BORDER_DP} / {@link UiTheme#DEFAULT_DIALOG_CORNER_DP}).
    // Width 0 = no border, radius 0 = square.
    DIALOG_BORDER("llui_dialog_border", UiGroup.DIALOGS, R.string.llui_dialog_border, false, false, "llui_text", 0),

    // --- Settings & Customize preference pages ---
    PREF_TITLE("llui_pref_title", UiGroup.SETTINGS, R.string.llui_pref_title, false, true, "llui_text", 0),
    PREF_SUMMARY("llui_pref_summary", UiGroup.SETTINGS, R.string.llui_pref_summary, false, true, "llui_text", 0),
    PREF_CATEGORY("llui_pref_category", UiGroup.SETTINGS, R.string.llui_pref_category, false, true, "llui_accent", 0),
    PREF_BG("llui_pref_bg", UiGroup.SETTINGS, R.string.llui_pref_bg, false, false, "llui_background", 0),

    // --- Toolbars + status bar ---
    TOOLBAR_BG("llui_toolbar_bg", UiGroup.TOOLBARS, R.string.llui_toolbar_bg, false, false, "llui_background", 0),
    TOOLBAR_TEXT("llui_toolbar_text", UiGroup.TOOLBARS, R.string.llui_toolbar_text, false, true, "llui_accent", 0),
    TOOLBAR_ICON("llui_toolbar_icon", UiGroup.TOOLBARS, R.string.llui_toolbar_icon, false, false, "llui_accent", 0),
    STATUSBAR_BG("llui_statusbar_bg", UiGroup.TOOLBARS, R.string.llui_statusbar_bg, false, false, "llui_background", 0),

    // --- Push buttons (e.g. the Backup/Restore screen). BUTTON_BORDER carries the border WIDTH and the
    // corner RADIUS (see UiConfig); width 0 = no border, radius 0 = square. ---
    BUTTON_BG("llui_button_bg", UiGroup.BUTTONS, R.string.llui_button_bg, false, false, "llui_background", 0),
    BUTTON_TEXT("llui_button_text", UiGroup.BUTTONS, R.string.llui_button_text, false, true, "llui_text", 0),
    BUTTON_BORDER("llui_button_border", UiGroup.BUTTONS, R.string.llui_button_border, false, false, "llui_text", 0),

    // --- Geometry box (the in-editor resize / reposition / re-layer panel). Split per region:
    // the panel, the top value tiles, the +/- cross, and the bottom layer-ordering buttons. Each
    // *_BORDER slot carries the border WIDTH + corner RADIUS; *_GLYPH / *_ICON tint the +/- text
    // and the z-order icons. Per-element sizes are stored as dp sizes in UiConfig, not here. ---
    GEOM_PANEL_BG("llui_geom_panel_bg", UiGroup.GEOMETRY, R.string.llui_geom_panel_bg, false, false, "llui_background", 0),
    GEOM_PANEL_BORDER("llui_geom_panel_border", UiGroup.GEOMETRY, R.string.llui_geom_panel_border, false, false, "llui_text", 0),
    GEOM_TILE_BG("llui_geom_tile_bg", UiGroup.GEOMETRY, R.string.llui_geom_tile_bg, false, false, "llui_background", 0),
    GEOM_TILE_BORDER("llui_geom_tile_border", UiGroup.GEOMETRY, R.string.llui_geom_tile_border, false, false, "llui_text", 0),
    GEOM_TILE_TEXT("llui_geom_tile_text", UiGroup.GEOMETRY, R.string.llui_geom_tile_text, false, true, "llui_text", 0),
    GEOM_CROSS_BG("llui_geom_cross_bg", UiGroup.GEOMETRY, R.string.llui_geom_cross_bg, false, false, "llui_background", 0),
    GEOM_CROSS_BORDER("llui_geom_cross_border", UiGroup.GEOMETRY, R.string.llui_geom_cross_border, false, false, "llui_text", 0),
    GEOM_CROSS_GLYPH("llui_geom_cross_glyph", UiGroup.GEOMETRY, R.string.llui_geom_cross_glyph, false, false, "llui_text", 0),
    GEOM_ZORDER_BG("llui_geom_zorder_bg", UiGroup.GEOMETRY, R.string.llui_geom_zorder_bg, false, false, "llui_background", 0),
    GEOM_ZORDER_BORDER("llui_geom_zorder_border", UiGroup.GEOMETRY, R.string.llui_geom_zorder_border, false, false, "llui_text", 0),
    GEOM_ZORDER_ICON("llui_geom_zorder_icon", UiGroup.GEOMETRY, R.string.llui_geom_zorder_icon, false, false, "llui_text", 0);

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

    /** A frame/stroke slot — it carries a border WIDTH and a corner RADIUS in addition to a colour. */
    public boolean isBorder() {
        return key.endsWith("_border");
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
