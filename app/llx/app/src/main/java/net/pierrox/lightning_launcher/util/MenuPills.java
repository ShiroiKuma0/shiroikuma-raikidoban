package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.util.UiIcons.Does;
import net.pierrox.lightning_launcher.util.UiIcons.Icon;
import net.pierrox.lightning_launcher_extreme.R;

/**
 * 白い熊: the rows of the launcher's menus — the bubble (long-press, Lightning menu, drawer menu,
 * script and plugin rows) and the toolbar ⋮ overflow. Every action row is an outlined pill as wide as
 * the menu, with its icon inside at the left, coloured by what the row does; a › at the right when it
 * opens something; frame, icon and label red when it deletes.
 * <p>
 * Menu rows carry no "opens something" or "destructive" flag, so this table, keyed by row id, supplies
 * those and the icons. A label ending in "…" is taken as "opens something" too.
 */
public final class MenuPills {

    private static final class Entry {
        final Icon icon;
        final Does does;
        final boolean opens;

        Entry(Icon icon, Does does, boolean opens) {
            this.icon = icon;
            this.does = does;
            this.opens = opens;
        }
    }

    private static final SparseArray<Entry> TABLE = new SparseArray<>();

    private static void put(int id, Icon icon, Does does, boolean opens) {
        TABLE.put(id, new Entry(icon, does, opens));
    }

    /** A row of a pure choice list (a sort mode, a visibility filter): a pill, but no icon column. */
    private static void choice(int id) {
        TABLE.put(id, new Entry(null, Does.VIEW, false));
    }

    static {
        // Lightning menu / drawer menu
        put(R.id.mi_l, Icon.LOCK, Does.BLOCK, false);
        put(R.id.mi_i, Icon.GRID, Does.CONTACT, true);
        put(R.id.mi_sel, Icon.SELECT, Does.VIEW, true);
        put(R.id.mi_s, Icon.CODE, Does.CONNECT, true);
        put(R.id.mi_gesture_actions, Icon.POINTER, Does.VIEW, true);
        put(R.id.mi_fold_matrix, Icon.LAYERS, Does.VIEW, true);
        put(R.id.mi_fold_migrate, Icon.BRANCH, Does.SHARE, true);
        put(R.id.mi_llui, Icon.PALETTE, Does.VIEW, true);
        put(R.id.mi_dm_customize, Icon.GEAR, Does.VIEW, true);
        put(R.id.mi_dmc_c, Icon.SLIDERS, Does.VIEW, true);
        put(R.id.mi_dmc_r, Icon.GEAR, Does.VIEW, true);
        put(R.id.mi_android_settings, Icon.DEVICE, Does.VIEW, true);
        put(R.id.mi_es_refresh, Icon.RESET, Does.CONNECT, false);
        put(R.id.mi_v, Icon.EYE, Does.VIEW, true);
        // item menu
        put(R.id.mi_edit, Icon.EDIT, Does.MESSAGE, true);
        put(R.id.mi_position, Icon.MOVE, Does.SHARE, true);
        put(R.id.mi_actions, Icon.PLAY, Does.CONNECT, true);
        put(R.id.mi_widget_options, Icon.SLIDERS, Does.VIEW, true);
        put(R.id.mi_set_jiyu_widget_name, Icon.TEXT, Does.MESSAGE, true);
        put(R.id.mi_reinit_jiyu_widgets, Icon.RESET, Does.CONNECT, true);
        put(R.id.mi_capture_jiyu_binding, Icon.IMPORT, Does.SHARE, false);
        put(R.id.mi_edit_icon, Icon.IMAGE, Does.MESSAGE, true);
        put(R.id.mi_edit_label, Icon.TEXT, Does.MESSAGE, true);
        put(R.id.mi_edit_launch_action, Icon.PLAY, Does.MESSAGE, true);
        put(R.id.mi_pick_widget, Icon.WIDGET, Does.CONTACT, true);
        put(R.id.mi_hide_unhide, Icon.EYE, Does.BLOCK, false);
        put(R.id.mi_ef_edit_layout, Icon.FRAME, Does.MESSAGE, true);
        put(R.id.mi_edit_more, Icon.SLIDERS, Does.VIEW, true);
        put(R.id.mi_lm, Icon.MOVE, Does.SHARE, false);
        put(R.id.mi_pin, Icon.PIN, Does.BLOCK, false);
        put(R.id.mi_move_out_of_folder, Icon.EXPORT, Does.SHARE, false);
        put(R.id.mi_move_to_screen, Icon.DESKTOP, Does.SHARE, true);
        put(R.id.mi_copy_to_screen, Icon.COPY, Does.SHARE, true);
        put(R.id.mi_cpf, Icon.DIRECTORY, Does.SHARE, false);
        put(R.id.mi_cfp, Icon.FRAME, Does.SHARE, false);
        put(R.id.mi_add_to_launcher, Icon.ADD, Does.CONNECT, false);
        put(R.id.mi_app_details, Icon.INFO, Does.CONTACT, true);
        put(R.id.mi_app_store, Icon.STORE, Does.CONTACT, true);
        put(R.id.mi_kill, Icon.STOP, Does.BLOCK, false);
        put(R.id.mi_uninstall, Icon.DELETE, Does.DELETE, true);
        put(R.id.mi_remove, Icon.DELETE, Does.DELETE, false);
        // items / scripts / selection submenus
        put(R.id.mi_h, Icon.BRANCH, Does.VIEW, true);
        put(R.id.mi_ic, Icon.GRID, Does.SHARE, false);
        put(R.id.mi_isa, Icon.SORT, Does.SHARE, false);
        put(R.id.mi_isd, Icon.SORT, Does.SHARE, false);
        put(R.id.mi_ls, Icon.IMPORT, Does.SHARE, true);
        put(R.id.mi_sa, Icon.SELECT, Does.VIEW, false);
        put(R.id.mi_sn, Icon.CLOSE, Does.VIEW, false);
        put(R.id.mi_ss, Icon.DESKTOP, Does.VIEW, false);
        put(R.id.mi_sr, Icon.FRAME, Does.VIEW, false);
        put(R.id.mi_si, Icon.RESET, Does.VIEW, false);
        // pure choice lists of the app drawer
        choice(R.id.mi_va);
        choice(R.id.mi_vov);
        choice(R.id.mi_voh);
        choice(R.id.mi_mode_custom);
        choice(R.id.mi_mode_by_name);
        choice(R.id.mi_mode_frequently_used);
        choice(R.id.mi_mode_recent_apps);
        choice(R.id.mi_mode_recently_updated);
        // toolbar overflow (Lightning Settings, Customize)
        put(R.id.h, Icon.HELP, Does.VIEW, true);
        put(R.id.rs, Icon.GEAR, Does.VIEW, true);
    }

    private MenuPills() {
    }

    public static Icon icon(int id) {
        Entry e = TABLE.get(id);
        return e == null ? Icon.LIST : e.icon;
    }

    public static Does does(int id) {
        Entry e = TABLE.get(id);
        return e == null ? Does.VIEW : e.does;
    }

    public static boolean opens(int id) {
        Entry e = TABLE.get(id);
        return e != null && e.opens;
    }

    public static boolean isDestructive(int id) {
        return does(id) == Does.DELETE;
    }

    /**
     * Give every item of an options menu (the toolbar ⋮ overflow) its icon, coloured by what the item
     * does, and make the overflow show icons. The rows themselves are drawn by {@code PillMenuItemView}.
     */
    @android.annotation.SuppressLint("RestrictedApi")
    public static void decorate(Context context, android.view.Menu menu) {
        if (menu == null) {
            return;
        }
        if (menu instanceof androidx.appcompat.view.menu.MenuBuilder) {
            ((androidx.appcompat.view.menu.MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem item = menu.getItem(i);
            if (item.hasSubMenu()) {
                decorate(context, item.getSubMenu());
            }
            if (item.isCheckable() || item.getIcon() != null) {
                continue;
            }
            int id = item.getItemId();
            Icon icon = icon(id);
            item.setIcon(UiIcons.drawable(context, icon == null ? Icon.LIST : icon, does(id)));
        }
    }

    /** A row with a known id; see {@link #row(TextView, Icon, Does, boolean)}. */
    public static void row(TextView row, int id) {
        Entry e = TABLE.get(id);
        if (id == R.id.mi_nos) {
            // "No script": a note, not an action
            plain(row);
            return;
        }
        if (e == null) {
            row(row, Icon.LIST, Does.VIEW, false);
        } else {
            row(row, e.icon, e.does, e.opens);
        }
    }

    /**
     * Turn one bubble row into a menu pill.
     *
     * @param icon  null for a row of a pure choice list (no icon column)
     * @param opens the row opens a screen, dialog or submenu (a label ending in "…" counts too)
     */
    public static void row(TextView row, Icon icon, Does does, boolean opens) {
        Context context = row.getContext();
        boolean deletes = does == Does.DELETE;
        int color = deletes ? UiTheme.color(UiSlot.DELETE) : UiTheme.color(UiSlot.MENU_TEXT);
        int frame = deletes ? color : UiTheme.color(UiSlot.MENU_ACCENT);
        CharSequence text = row.getText();
        if (!opens && text != null && text.toString().endsWith("…")) {
            opens = true;
        }

        row.setBackground(frame(context, frame));
        row.setTextColor(color);
        UiTheme.applyFont(row, UiSlot.MENU_TEXT);
        row.setAllCaps(false);
        row.setStateListAnimator(null);
        row.setMinHeight(0);
        row.setMinimumHeight(0);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        row.setPadding(UiPills.dp(context, 14), UiPills.dp(context, 9), UiPills.dp(context, 14), UiPills.dp(context, 9));
        Drawable start = icon == null ? null : UiIcons.drawable(context, icon, does);
        Drawable end = opens ? UiIcons.drawable(context, Icon.CHEVRON, color, 16f) : null;
        row.setCompoundDrawablesRelative(start, null, end, null);
        row.setCompoundDrawablePadding(UiPills.dp(context, 10));
        margins(row);
    }

    /** A row that is not an action (a note, a hint): no frame. */
    public static void plain(TextView row) {
        row.setBackground(null);
        row.setTextColor(UiTheme.color(UiSlot.MENU_TEXT));
        UiTheme.applyFont(row, UiSlot.MENU_TEXT);
    }

    /** A ready-made row view (an app shortcut with its own icon): the pill frame and the row spacing. */
    public static void frameRow(View row) {
        row.setBackground(frame(row.getContext(), UiTheme.color(UiSlot.MENU_ACCENT)));
        margins(row);
    }

    // 3 dp above and below each pill; the panel's own padding keeps it 8 dp from the menu's sides.
    private static void margins(View row) {
        int v = UiPills.dp(row.getContext(), 3);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, v, 0, v);
        row.setLayoutParams(lp);
    }

    // The bubble disables its rows while it closes, so the frame colour is fixed here rather than
    // dimming with the view state — otherwise every pill would flash dim on the way out.
    private static Drawable frame(Context context, int color) {
        float radius = UiPills.dp(context, 100);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(radius);
        pill.setColor(Color.TRANSPARENT);
        pill.setStroke(Math.max(1, UiPills.dp(context, UiPills.REGULAR_DP)), color);
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(radius);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(UiPills.ripple(color)), pill, mask);
    }
}
