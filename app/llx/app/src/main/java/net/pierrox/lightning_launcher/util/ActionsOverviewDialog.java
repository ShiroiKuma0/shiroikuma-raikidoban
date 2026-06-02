/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceCategory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.dinglisch.android.tasker.TaskerIntent;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.data.Action;
import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A themed (night yellow-on-black) overview of every event slot of a desktop or an item, showing the
 * resolved action for each (app name / script name / "Tasker: &lt;Task&gt;" / ...), with the ability
 * to change a slot (tap the row) or clear it (the X). It is shown from an Activity so it inherits the
 * activity theme; the host activity drives the actual picking/persisting via {@link Callback}.
 */
public class ActionsOverviewDialog {

    /** Host (Dashboard) hooks: invoked when a row is tapped (change) or its X is tapped (clear). */
    public interface Callback {
        void onChangeSlot(Slot slot);

        void onClearSlot(Slot slot);
    }

    /** Reads/writes one EventAction field, plus its global default (for inheritance display). */
    public interface Accessor {
        EventAction get();

        void set(EventAction ea);

        /** The global default this slot inherits when unset, or null when there is none. */
        EventAction global();
    }

    public static class Slot {
        public final int categoryRes;
        public final int titleRes;
        public final int type;       // Action.FLAG_TYPE_*
        public final boolean forItem;
        public final Accessor acc;

        public Slot(int categoryRes, int titleRes, int type, boolean forItem, Accessor acc) {
            this.categoryRes = categoryRes;
            this.titleRes = titleRes;
            this.type = type;
            this.forItem = forItem;
            this.acc = acc;
        }

        public EventAction get() {
            return acc.get();
        }

        public void set(EventAction ea) {
            acc.set(ea);
        }

        /** The effective action: the slot's own value, or the inherited global default when unset. */
        public EventAction resolved() {
            EventAction ea = acc.get();
            if (ea == null || ea.action == GlobalConfig.UNSET) {
                EventAction g = acc.global();
                if (g != null) {
                    return g;
                }
            }
            return ea;
        }

        public boolean isInherited() {
            EventAction ea = acc.get();
            return (ea == null || ea.action == GlobalConfig.UNSET) && acc.global() != null;
        }
    }

    private final Activity mActivity;
    private final LightningEngine mEngine;
    private final Item mItem; // the item this dialog is for, or null for a desktop/page
    private final Callback mCallback;
    private final ActionsDescription mActions;
    private final List<Object> mRows = new ArrayList<>(); // Integer category title res, or Slot
    private final SlotAdapter mAdapter;
    private AlertDialog mDialog;

    public ActionsOverviewDialog(Activity activity, LightningEngine engine, Item item, List<Slot> slots, Callback callback) {
        mActivity = activity;
        mEngine = engine;
        mItem = item;
        mCallback = callback;

        boolean forItem = !slots.isEmpty() && slots.get(0).forItem;
        int type = slots.isEmpty() ? Action.FLAG_TYPE_DESKTOP : slots.get(0).type;
        mActions = new ActionsDescription(activity, type, forItem);

        int lastCategory = 0;
        for (Slot slot : slots) {
            if (slot.categoryRes != lastCategory) {
                mRows.add(Integer.valueOf(slot.categoryRes));
                lastCategory = slot.categoryRes;
            }
            mRows.add(slot);
        }

        mAdapter = new SlotAdapter();
    }

    public void show() {
        ListView list = new ListView(mActivity);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = mRows.get(position);
                if (o instanceof Slot && mCallback != null) {
                    mCallback.onChangeSlot((Slot) o);
                }
            }
        });

        mDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.acd_actions_title)
                .setView(list)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        net.pierrox.lightning_launcher.util.UiDialogStyler.style(mDialog);
    }

    /** Re-read every slot and redraw; call after a slot has been edited or cleared. */
    public void refresh() {
        mAdapter.notifyDataSetChanged();
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private String valueText(Slot slot) {
        EventAction resolved = slot.resolved();
        String txt = describeValue(resolved);
        if (slot.isInherited() && resolved != null
                && resolved.action != GlobalConfig.UNSET && resolved.action != GlobalConfig.NOTHING) {
            txt = mActivity.getString(R.string.acd_default_suffix, txt);
        }
        return txt;
    }

    private String describeValue(EventAction ea) {
        if (ea == null) {
            return mActivity.getString(R.string.acd_none);
        }
        int a = ea.action;
        if (a == GlobalConfig.UNSET || a == GlobalConfig.NOTHING) {
            return mActivity.getString(R.string.acd_none);
        }
        if (a == GlobalConfig.LAUNCH_ITEM && mItem != null) {
            // "Launch the item" runs the item's own action -> show what THAT launches (the app, folder,
            // Tasker task, ...), not the generic "Launch item" label.
            String d = itemLaunchLabel(mItem);
            if (d != null) {
                return d;
            }
        }
        if (a == GlobalConfig.LAUNCH_SHORTCUT && ea.data != null) {
            try {
                Intent i = Intent.parseUri(ea.data, 0);
                String lbl = i.getStringExtra(LightningIntent.INTENT_EXTRA_SHORTCUT_LABEL);
                if (lbl != null) {
                    return lbl;
                }
                String be = shortcutLaunchLabel(mActivity, i, null);
                if (be != null) {
                    return be;
                }
            } catch (Exception e) {
                // pass
            }
        }
        String d = ea.describe(mEngine);
        if (d != null) {
            return d;
        }
        return mActions.getActionName(a);
    }

    // What does launching this item actually do: the app/shortcut/Tasker task it opens (a folder item
    // opens its folder). Returns null if it can't be determined (caller falls back to "Launch item").
    private String itemLaunchLabel(Item item) {
        if (item instanceof Shortcut) {
            Shortcut sc = (Shortcut) item;
            Intent intent = sc.getIntent();
            if (intent != null) {
                if (LLApp.get().isLightningIntent(intent)) {
                    EventAction inner = Utils.decodeEventActionFromLightningIntent(intent);
                    if (inner != null) {
                        String d = describeValue(inner);
                        if (d != null) {
                            return d;
                        }
                    }
                } else {
                    String lbl = intent.getStringExtra(LightningIntent.INTENT_EXTRA_SHORTCUT_LABEL);
                    if (lbl != null) {
                        return lbl;
                    }
                    String be = shortcutLaunchLabel(mActivity, intent, null);
                    if (be != null) {
                        return be;
                    }
                    PackageManager pm = mActivity.getPackageManager();
                    ResolveInfo act = pm.resolveActivity(intent, 0);
                    if (act != null) {
                        return act.loadLabel(pm).toString();
                    }
                }
            }
            String label = sc.getLabel();
            if (label != null && label.length() > 0) {
                return label;
            }
        }
        return null;
    }

    /**
     * Build a human-readable name for a picked shortcut launch intent. For a Tasker task this is
     * "Tasker: &lt;task&gt;"; otherwise the supplied fallback (typically EXTRA_SHORTCUT_NAME) is used.
     * Returns null when nothing better than the fallback is available and the fallback is null.
     */
    public static String shortcutLaunchLabel(Context ctx, Intent launchIntent, String fallbackName) {
        if (launchIntent != null) {
            String pkg = launchIntent.getPackage();
            if (pkg == null && launchIntent.getComponent() != null) {
                pkg = launchIntent.getComponent().getPackageName();
            }
            if (TaskerIntent.TASKER_PACKAGE_MARKET.equals(pkg) || TaskerIntent.TASKER_PACKAGE.equals(pkg)) {
                String task = launchIntent.getStringExtra(TaskerIntent.EXTRA_TASK_NAME);
                if (task == null) {
                    task = taskNameFromMacro(launchIntent.getStringExtra("mcro"));
                }
                if (task == null) {
                    task = fallbackName;
                }
                if (task != null) {
                    return ctx.getString(R.string.acd_tasker_prefix, task);
                }
            }
        }
        return fallbackName;
    }

    // Tasker "run task" intents (action ...WIDICKYUM, component taskerm/.IntentHandler) carry the task
    // as URL-encoded XML in the "mcro" string extra; the task name is its <nme> element. This recovers
    // it for bindings made before the name was captured explicitly, so they read "Tasker: <task>".
    private static String taskNameFromMacro(String mcro) {
        if (mcro == null) {
            return null;
        }
        if (mcro.indexOf("<nme>") < 0 && mcro.indexOf('%') >= 0) {
            try {
                mcro = Uri.decode(mcro);
            } catch (Exception e) {
                // pass
            }
        }
        int a = mcro.indexOf("<nme>");
        if (a < 0) {
            return null;
        }
        int b = mcro.indexOf("</nme>", a + 5);
        if (b < 0) {
            return null;
        }
        return mcro.substring(a + 5, b);
    }

    public static List<Slot> buildPageSlots(final Page page, final GlobalConfig gc) {
        final PageConfig pc = page.config;
        final int type = page.id == Page.APP_DRAWER_PAGE ? Action.FLAG_TYPE_APP_DRAWER : Action.FLAG_TYPE_DESKTOP;
        List<Slot> s = new ArrayList<>();

        s.add(new Slot(R.string.acd_cat_keys, R.string.ev_home, type, false, new Accessor() {
            public EventAction get() { return pc.homeKey; } public void set(EventAction e) { pc.homeKey = e; } public EventAction global() { return gc.homeKey; } }));
        s.add(new Slot(R.string.acd_cat_keys, R.string.ev_menu, type, false, new Accessor() {
            public EventAction get() { return pc.menuKey; } public void set(EventAction e) { pc.menuKey = e; } public EventAction global() { return gc.menuKey; } }));
        s.add(new Slot(R.string.acd_cat_keys, R.string.ev_menul, type, false, new Accessor() {
            public EventAction get() { return pc.longMenuKey; } public void set(EventAction e) { pc.longMenuKey = e; } public EventAction global() { return gc.longMenuKey; } }));
        s.add(new Slot(R.string.acd_cat_keys, R.string.ev_back, type, false, new Accessor() {
            public EventAction get() { return pc.backKey; } public void set(EventAction e) { pc.backKey = e; } public EventAction global() { return gc.backKey; } }));
        s.add(new Slot(R.string.acd_cat_keys, R.string.ev_backl, type, false, new Accessor() {
            public EventAction get() { return pc.longBackKey; } public void set(EventAction e) { pc.longBackKey = e; } public EventAction global() { return gc.longBackKey; } }));
        s.add(new Slot(R.string.acd_cat_keys, R.string.ev_search, type, false, new Accessor() {
            public EventAction get() { return pc.searchKey; } public void set(EventAction e) { pc.searchKey = e; } public EventAction global() { return gc.searchKey; } }));

        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_bg_tap, type, false, new Accessor() {
            public EventAction get() { return pc.bgTap; } public void set(EventAction e) { pc.bgTap = e; } public EventAction global() { return gc.bgTap; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_bg_dtap, type, false, new Accessor() {
            public EventAction get() { return pc.bgDoubleTap; } public void set(EventAction e) { pc.bgDoubleTap = e; } public EventAction global() { return gc.bgDoubleTap; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_bg_ltap, type, false, new Accessor() {
            public EventAction get() { return pc.bgLongTap; } public void set(EventAction e) { pc.bgLongTap = e; } public EventAction global() { return gc.bgLongTap; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_r, type, false, new Accessor() {
            public EventAction get() { return pc.swipeLeft; } public void set(EventAction e) { pc.swipeLeft = e; } public EventAction global() { return gc.swipeLeft; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_l, type, false, new Accessor() {
            public EventAction get() { return pc.swipeRight; } public void set(EventAction e) { pc.swipeRight = e; } public EventAction global() { return gc.swipeRight; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_u, type, false, new Accessor() {
            public EventAction get() { return pc.swipeUp; } public void set(EventAction e) { pc.swipeUp = e; } public EventAction global() { return gc.swipeUp; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_d, type, false, new Accessor() {
            public EventAction get() { return pc.swipeDown; } public void set(EventAction e) { pc.swipeDown = e; } public EventAction global() { return gc.swipeDown; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe2_r, type, false, new Accessor() {
            public EventAction get() { return pc.swipe2Left; } public void set(EventAction e) { pc.swipe2Left = e; } public EventAction global() { return gc.swipe2Left; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe2_l, type, false, new Accessor() {
            public EventAction get() { return pc.swipe2Right; } public void set(EventAction e) { pc.swipe2Right = e; } public EventAction global() { return gc.swipe2Right; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe2_u, type, false, new Accessor() {
            public EventAction get() { return pc.swipe2Up; } public void set(EventAction e) { pc.swipe2Up = e; } public EventAction global() { return gc.swipe2Up; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe2_d, type, false, new Accessor() {
            public EventAction get() { return pc.swipe2Down; } public void set(EventAction e) { pc.swipe2Down = e; } public EventAction global() { return gc.swipe2Down; } }));

        s.add(new Slot(R.string.acd_cat_screen, R.string.ev_screen_on, type, false, new Accessor() {
            public EventAction get() { return pc.screenOn; } public void set(EventAction e) { pc.screenOn = e; } public EventAction global() { return gc.screenOn; } }));
        s.add(new Slot(R.string.acd_cat_screen, R.string.ev_screen_off, type, false, new Accessor() {
            public EventAction get() { return pc.screenOff; } public void set(EventAction e) { pc.screenOff = e; } public EventAction global() { return gc.screenOff; } }));
        s.add(new Slot(R.string.acd_cat_screen, R.string.ev_op, type, false, new Accessor() {
            public EventAction get() { return pc.orientationPortrait; } public void set(EventAction e) { pc.orientationPortrait = e; } public EventAction global() { return gc.orientationPortrait; } }));
        s.add(new Slot(R.string.acd_cat_screen, R.string.ev_ol, type, false, new Accessor() {
            public EventAction get() { return pc.orientationLandscape; } public void set(EventAction e) { pc.orientationLandscape = e; } public EventAction global() { return gc.orientationLandscape; } }));

        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_load, type, false, new Accessor() {
            public EventAction get() { return pc.load; } public void set(EventAction e) { pc.load = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_resumed, type, false, new Accessor() {
            public EventAction get() { return pc.resumed; } public void set(EventAction e) { pc.resumed = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_paused, type, false, new Accessor() {
            public EventAction get() { return pc.paused; } public void set(EventAction e) { pc.paused = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_pos, type, false, new Accessor() {
            public EventAction get() { return pc.posChanged; } public void set(EventAction e) { pc.posChanged = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_cia, type, false, new Accessor() {
            public EventAction get() { return pc.itemAdded; } public void set(EventAction e) { pc.itemAdded = e; } public EventAction global() { return gc.itemAdded; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_cir, type, false, new Accessor() {
            public EventAction get() { return pc.itemRemoved; } public void set(EventAction e) { pc.itemRemoved = e; } public EventAction global() { return gc.itemRemoved; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_m, type, false, new Accessor() {
            public EventAction get() { return pc.menu; } public void set(EventAction e) { pc.menu = e; } public EventAction global() { return gc.menu; } }));

        return s;
    }

    public static List<Slot> buildItemSlots(final Item item, final GlobalConfig gc) {
        final int type = Action.FLAG_TYPE_DESKTOP;
        List<Slot> s = new ArrayList<>();

        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_tap, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().tap; } public void set(EventAction e) { item.modifyItemConfig().tap = e; } public EventAction global() { return gc.itemTap; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_long_tap, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().longTap; } public void set(EventAction e) { item.modifyItemConfig().longTap = e; } public EventAction global() { return gc.itemLongTap; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_r, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().swipeLeft; } public void set(EventAction e) { item.modifyItemConfig().swipeLeft = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_l, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().swipeRight; } public void set(EventAction e) { item.modifyItemConfig().swipeRight = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_u, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().swipeUp; } public void set(EventAction e) { item.modifyItemConfig().swipeUp = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_swipe_d, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().swipeDown; } public void set(EventAction e) { item.modifyItemConfig().swipeDown = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_touch, R.string.ev_touch, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().touch; } public void set(EventAction e) { item.modifyItemConfig().touch = e; } public EventAction global() { return null; } }));

        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_resumed, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().resumed; } public void set(EventAction e) { item.modifyItemConfig().resumed = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_paused, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().paused; } public void set(EventAction e) { item.modifyItemConfig().paused = e; } public EventAction global() { return null; } }));
        s.add(new Slot(R.string.acd_cat_lifecycle, R.string.ev_m, type, true, new Accessor() {
            public EventAction get() { return item.getItemConfig().menu; } public void set(EventAction e) { item.modifyItemConfig().menu = e; } public EventAction global() { return null; } }));

        return s;
    }

    private class SlotAdapter extends BaseAdapter {
        private final LayoutInflater mInflater = LayoutInflater.from(mActivity);
        private final int mCategoryLayout = new PreferenceCategory(mActivity).getLayoutResource();

        @Override
        public int getCount() {
            return mRows.size();
        }

        @Override
        public Object getItem(int position) {
            return mRows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return mRows.get(position) instanceof Slot ? 1 : 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return mRows.get(position) instanceof Slot;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object o = mRows.get(position);
            if (!(o instanceof Slot)) {
                if (convertView == null) {
                    convertView = mInflater.inflate(mCategoryLayout, parent, false);
                }
                TextView title = convertView.findViewById(android.R.id.title);
                title.setText((Integer) o);
                net.pierrox.lightning_launcher.configuration.UiTheme.applyTo(title,
                        net.pierrox.lightning_launcher.configuration.UiSlot.DIALOG_TITLE);
                return convertView;
            }

            final Slot slot = (Slot) o;
            if (convertView == null || convertView.findViewById(R.id.label) == null) {
                convertView = mInflater.inflate(R.layout.actions_overview_item, parent, false);
                TextView delete = convertView.findViewById(R.id.delete);
                delete.setTypeface(LLApp.get().getIconsTypeface());
            }

            TextView label = convertView.findViewById(R.id.label);
            TextView value = convertView.findViewById(R.id.value);
            label.setText(slot.titleRes);
            value.setText(valueText(slot));
            net.pierrox.lightning_launcher.configuration.UiTheme.applyTo(label,
                    net.pierrox.lightning_launcher.configuration.UiSlot.DIALOG_TEXT);
            value.setTextColor(net.pierrox.lightning_launcher.configuration.UiTheme.color(
                    net.pierrox.lightning_launcher.configuration.UiSlot.DIALOG_TEXT));
            ((TextView) convertView.findViewById(R.id.delete)).setTextColor(
                    net.pierrox.lightning_launcher.configuration.UiTheme.color(
                            net.pierrox.lightning_launcher.configuration.UiSlot.DIALOG_TEXT));

            View delete = convertView.findViewById(R.id.delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback != null) {
                        mCallback.onClearSlot(slot);
                    }
                }
            });

            return convertView;
        }
    }
}
