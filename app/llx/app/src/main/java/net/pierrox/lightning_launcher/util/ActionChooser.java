package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.data.Action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 白い熊: the body of the "Select an action" dialog — a chip index on top (one chip per group, a tap
 * unfolds the group and scrolls to it), the ungrouped actions, then every group as a folding section
 * (heading with its icon, › folded / ⌄ open) whose rows are plain: one icon column, one label edge,
 * each action with its own icon coloured by what it does. Fold state is remembered in "llui"
 * ({@link UiPageState}, keys {@code act_cat_<n>}); every group starts folded.
 */
public final class ActionChooser {

    public interface Listener {
        void onChosen(Action action);
    }

    private ActionChooser() {
    }

    /**
     * @param actions the offered actions in display order, {@link GlobalConfig#CATEGORY} rows marking
     *                where a group starts
     * @param extra   icons of the caller's synthetic (negative) action codes; may be null
     */
    public static View build(final Context context, List<Action> actions, Map<Integer, Object[]> extra,
                             final Listener listener) {
        final int accent = UiTheme.color(UiSlot.ACCENT);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        final UiSectionIndex.Flow chips = new UiSectionIndex.Flow(context, dp(context, 6));
        chips.setPadding(dp(context, 18), dp(context, 4), dp(context, 18), dp(context, 10));
        root.addView(chips);
        root.addView(line(context, 0));

        final ScrollView scroll = new ScrollView(context);
        final LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final Map<String, View> wrappers = new HashMap<>();
        final Map<String, View> bodies = new HashMap<>();
        final Map<String, TextView> chevrons = new HashMap<>();

        LinearLayout into = body;
        boolean first = true;
        for (final Action action : actions) {
            if (action.action == GlobalConfig.CATEGORY) {
                final String key = "act_cat_" + action.category;
                UiIcons.Icon icon = categoryIcon(action.category);
                int color = UiIcons.color(categoryDoes(action.category));
                String label = context.getString(action.label);

                TextView chip = UiPills.chip(context, label, accent);
                Drawable chipIcon = UiIcons.drawable(context, icon, color, 14f);
                chip.setCompoundDrawablesRelative(chipIcon, null, null, null);
                chip.setCompoundDrawablePadding(dp(context, 5));
                chip.setPadding(dp(context, 10), dp(context, 5), dp(context, 12), dp(context, 5));
                chips.addView(chip);

                LinearLayout wrapper = new LinearLayout(context);
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.addView(line(context, 0));

                LinearLayout header = new LinearLayout(context);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                header.setPadding(dp(context, 18), dp(context, 12), dp(context, 14), dp(context, 10));
                header.setBackgroundResource(selectable(context));

                View iconView = new View(context);
                iconView.setBackground(UiIcons.drawable(context, icon, color, 20f));
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(context, 20), dp(context, 20));
                ilp.setMarginEnd(dp(context, 10));
                header.addView(iconView, ilp);

                LinearLayout labelCol = new LinearLayout(context);
                labelCol.setOrientation(LinearLayout.VERTICAL);
                TextView title = new TextView(context);
                title.setText(label);
                title.setTextColor(accent);
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                title.setTypeface(UiTheme.typeface(UiSlot.DIALOG_TEXT, Typeface.BOLD), Typeface.BOLD);
                title.setMaxLines(1);
                labelCol.addView(title, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                View underline = new View(context);
                underline.setBackgroundColor(accent);
                labelCol.addView(underline, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 2))));
                header.addView(labelCol, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                View spacer = new View(context);
                header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

                final TextView chevron = new TextView(context);
                chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                chevron.setTextColor(accent);
                chevron.setIncludeFontPadding(false);
                header.addView(chevron);
                wrapper.addView(header);

                final LinearLayout inner = new LinearLayout(context);
                inner.setOrientation(LinearLayout.VERTICAL);
                wrapper.addView(inner);

                boolean open = UiPageState.isOpen(key);
                inner.setVisibility(open ? View.VISIBLE : View.GONE);
                chevron.setText(open ? "⌄" : "›");

                wrappers.put(key, wrapper);
                bodies.put(key, inner);
                chevrons.put(key, chevron);

                header.setOnClickListener(v -> setOpen(key, !UiPageState.isOpen(key), bodies, chevrons));
                chip.setOnClickListener(v -> {
                    setOpen(key, true, bodies, chevrons);
                    final View w = wrappers.get(key);
                    body.post(() -> scroll.smoothScrollTo(0, w.getTop()));
                });

                body.addView(wrapper);
                into = inner;
                first = true;
                continue;
            }

            if (!first) {
                into.addView(line(context, dp(context, 18)));
            }
            first = false;

            Object[] look = extra == null ? null : extra.get(action.action);
            UiIcons.Icon icon = look != null ? (UiIcons.Icon) look[0] : actionIcon(action.action);
            UiIcons.Does does = look != null ? (UiIcons.Does) look[1] : actionDoes(action.action);

            TextView row = new TextView(context);
            row.setText(action.label);
            UiTheme.applyTo(row, UiSlot.DIALOG_TEXT);
            if (UiTheme.size(UiSlot.DIALOG_TEXT) <= 0) {
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 10));
            row.setBackgroundResource(selectable(context));
            UiIcons.onRow(row, icon, does);
            row.setOnClickListener(v -> listener.onChosen(action));
            into.addView(row);
        }
        body.addView(line(context, 0));
        return root;
    }

    private static void setOpen(String key, boolean open, Map<String, View> bodies, Map<String, TextView> chevrons) {
        UiPageState.setOpen(key, open);
        View inner = bodies.get(key);
        TextView chevron = chevrons.get(key);
        if (inner != null) {
            inner.setVisibility(open ? View.VISIBLE : View.GONE);
        }
        if (chevron != null) {
            chevron.setText(open ? "⌄" : "›");
        }
    }

    private static View line(Context context, int inset) {
        View v = new View(context);
        v.setBackgroundColor(inset == 0 ? UiPills.divider()
                : UiPills.blend(UiTheme.color(UiSlot.DIALOG_BG), UiTheme.color(UiSlot.ACCENT), 0.3f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(context, inset == 0 ? 1 : 0.5f)));
        lp.setMarginStart(inset);
        lp.setMarginEnd(inset);
        v.setLayoutParams(lp);
        return v;
    }

    private static int selectable(Context context) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private static int dp(Context context, float v) {
        return UiPills.dp(context, v);
    }

    // --- which icon, coloured by which role ---

    private static UiIcons.Icon categoryIcon(int category) {
        switch (category) {
            case Action.CAT_LAUNCH_AND_APPS:
                return UiIcons.Icon.PLAY;
            case Action.CAT_NAVIGATION:
                return UiIcons.Icon.HOME;
            case Action.CAT_MENU_STATUS_BAR:
                return UiIcons.Icon.LIST;
            case Action.CAT_FOLDERS:
                return UiIcons.Icon.DIRECTORY;
            case Action.CAT_EDITION:
                return UiIcons.Icon.EDIT;
            case Action.CAT_EXTERNAL:
                return UiIcons.Icon.DEVICE;
            default:
                return UiIcons.Icon.CODE;
        }
    }

    /** No two neighbouring groups share a colour. */
    private static UiIcons.Does categoryDoes(int category) {
        switch (category) {
            case Action.CAT_LAUNCH_AND_APPS:
                return UiIcons.Does.CONNECT;
            case Action.CAT_NAVIGATION:
                return UiIcons.Does.MESSAGE;
            case Action.CAT_MENU_STATUS_BAR:
                return UiIcons.Does.SHARE;
            case Action.CAT_FOLDERS:
                return UiIcons.Does.CONTACT;
            case Action.CAT_EDITION:
                return UiIcons.Does.CONNECT;
            case Action.CAT_EXTERNAL:
                return UiIcons.Does.SHARE;
            default:
                return UiIcons.Does.DELETE;
        }
    }

    private static UiIcons.Icon actionIcon(int action) {
        switch (action) {
            case GlobalConfig.LAUNCH_APP:
                return UiIcons.Icon.APP;
            case GlobalConfig.LAUNCH_SHORTCUT:
                return UiIcons.Icon.LINK;
            case GlobalConfig.LAUNCH_ITEM:
                return UiIcons.Icon.PLAY;
            case GlobalConfig.SHOW_APP_SHORTCUTS:
                return UiIcons.Icon.LIST;
            case GlobalConfig.APP_DRAWER:
                return UiIcons.Icon.GRID;
            case GlobalConfig.SEARCH_APP:
            case GlobalConfig.SEARCH:
                return UiIcons.Icon.SEARCH;
            case GlobalConfig.RESTART:
                return UiIcons.Icon.RESET;
            case GlobalConfig.GO_HOME:
            case GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN:
                return UiIcons.Icon.HOME;
            case GlobalConfig.GO_DESKTOP_POSITION:
                return UiIcons.Icon.PIN;
            case GlobalConfig.PREVIOUS_DESKTOP:
                return UiIcons.Icon.LEFT;
            case GlobalConfig.NEXT_DESKTOP:
                return UiIcons.Icon.RIGHT;
            case GlobalConfig.SELECT_DESKTOP_TO_GO_TO:
                return UiIcons.Icon.DESKTOP;
            case GlobalConfig.ZOOM_TO_ORIGIN:
                return UiIcons.Icon.SIZE;
            case GlobalConfig.ZOOM_FULL_SCALE:
                return UiIcons.Icon.FRAME;
            case GlobalConfig.SWITCH_FULL_SCALE_OR_ORIGIN:
            case GlobalConfig.SHOW_HIDE_STATUS_BAR:
            case GlobalConfig.SHOW_HIDE_APP_MENU_STATUS_BAR:
                return UiIcons.Icon.TOGGLE;
            case GlobalConfig.BACK:
                return UiIcons.Icon.HISTORY;
            case GlobalConfig.LAUNCHER_MENU:
            case GlobalConfig.ITEM_MENU:
                return UiIcons.Icon.LIST;
            case GlobalConfig.CUSTOM_MENU:
                return UiIcons.Icon.SLIDERS;
            case GlobalConfig.USER_MENU:
                return UiIcons.Icon.STAR;
            case GlobalConfig.SHOW_NOTIFICATIONS:
                return UiIcons.Icon.INFO;
            case GlobalConfig.OPEN_FOLDER:
                return UiIcons.Icon.DIRECTORY;
            case GlobalConfig.CLOSE_TOPMOST_FOLDER:
            case GlobalConfig.CLOSE_ALL_FOLDERS:
                return UiIcons.Icon.CLOSE;
            case GlobalConfig.EDIT_LAYOUT:
                return UiIcons.Icon.EDIT;
            case GlobalConfig.ADD_ITEM:
                return UiIcons.Icon.ADD;
            case GlobalConfig.MOVE_ITEM:
                return UiIcons.Icon.MOVE;
            case GlobalConfig.CUSTOMIZE_ITEM:
                return UiIcons.Icon.SLIDERS;
            case GlobalConfig.CUSTOMIZE_DESKTOP:
                return UiIcons.Icon.DESKTOP;
            case GlobalConfig.CUSTOMIZE_LAUNCHER:
                return UiIcons.Icon.GEAR;
            case GlobalConfig.OPEN_HIERARCHY_SCREEN:
                return UiIcons.Icon.BRANCH;
            case GlobalConfig.SELECT_WALLPAPER:
                return UiIcons.Icon.IMAGE;
            case GlobalConfig.WALLPAPER_TAP:
            case GlobalConfig.WALLPAPER_SECONDARY_TAP:
                return UiIcons.Icon.POINTER;
            case GlobalConfig.UNLOCK_SCREEN:
                return UiIcons.Icon.UNLOCK;
            case GlobalConfig.SHOW_FLOATING_DESKTOP:
            case GlobalConfig.HIDE_FLOATING_DESKTOP:
                return UiIcons.Icon.LAYERS;
            case GlobalConfig.RUN_SCRIPT:
                return UiIcons.Icon.CODE;
            case GlobalConfig.SET_VARIABLE:
                return UiIcons.Icon.TEXT;
            default:
                return UiIcons.Icon.BLOCK;
        }
    }

    private static UiIcons.Does actionDoes(int action) {
        switch (action) {
            // runs, launches, adds
            case GlobalConfig.LAUNCH_APP:
            case GlobalConfig.LAUNCH_SHORTCUT:
            case GlobalConfig.LAUNCH_ITEM:
            case GlobalConfig.RESTART:
            case GlobalConfig.ADD_ITEM:
            case GlobalConfig.WALLPAPER_TAP:
            case GlobalConfig.WALLPAPER_SECONDARY_TAP:
            case GlobalConfig.RUN_SCRIPT:
                return UiIcons.Does.CONNECT;
            // moves about the desktops; edits
            case GlobalConfig.GO_HOME:
            case GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN:
            case GlobalConfig.GO_DESKTOP_POSITION:
            case GlobalConfig.PREVIOUS_DESKTOP:
            case GlobalConfig.NEXT_DESKTOP:
            case GlobalConfig.SELECT_DESKTOP_TO_GO_TO:
            case GlobalConfig.BACK:
            case GlobalConfig.EDIT_LAYOUT:
            case GlobalConfig.SET_VARIABLE:
                return UiIcons.Does.MESSAGE;
            // is about one item
            case GlobalConfig.ITEM_MENU:
            case GlobalConfig.OPEN_FOLDER:
            case GlobalConfig.CUSTOMIZE_ITEM:
                return UiIcons.Does.CONTACT;
            case GlobalConfig.MOVE_ITEM:
                return UiIcons.Does.SHARE;
            // hides, closes, locks
            case GlobalConfig.SHOW_HIDE_STATUS_BAR:
            case GlobalConfig.SHOW_HIDE_APP_MENU_STATUS_BAR:
            case GlobalConfig.CLOSE_TOPMOST_FOLDER:
            case GlobalConfig.CLOSE_ALL_FOLDERS:
            case GlobalConfig.UNLOCK_SCREEN:
            case GlobalConfig.HIDE_FLOATING_DESKTOP:
            case GlobalConfig.NOTHING:
                return UiIcons.Does.BLOCK;
            default:
                return UiIcons.Does.VIEW;
        }
    }
}
