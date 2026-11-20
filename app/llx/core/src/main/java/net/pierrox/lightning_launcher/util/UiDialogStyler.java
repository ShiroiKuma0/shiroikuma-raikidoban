package net.pierrox.lightning_launcher.util;

import android.graphics.drawable.ColorDrawable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.app.AlertDialog;
import java.util.WeakHashMap;
import java.util.List;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

/**
 * Applies the DIALOG_* {@link UiSlot}s to an already-shown alert dialog: title, message and button
 * colours/fonts, plus the rounded {@link UiSlot#DIALOG_BG} panel with its {@link UiSlot#DIALOG_BORDER}
 * frame. Works on the framework {@link AlertDialog} and on AppCompat's — every view is looked up by id
 * (both use {@code android.R.id.message} / {@code button1..3}), so nothing here needs the concrete type.
 * Any other text in the dialog — list rows, an EditText, a custom {@code setView} layout — is caught by
 * {@link UiTheme#paintUnstyledText}, so no dialog is left with white platform text.
 */
public final class UiDialogStyler {

    private UiDialogStyler() {
    }

    public static void style(Dialog dialog) {
        if (dialog == null) {
            return;
        }

        int textColor = UiTheme.color(UiSlot.DIALOG_TEXT);
        int titleColor = UiTheme.color(UiSlot.DIALOG_TITLE);
        int buttonColor = UiTheme.color(UiSlot.DIALOG_BUTTON);

        // Everything the slots below do not own — the rows of a setItems() / setSingleChoiceItems()
        // list, an EditText, a custom setView layout — keeps the platform's white text on our black
        // panel. Paint it DIALOG_TEXT first, so the specific slots still win on the views they own.
        Window w = dialog.getWindow();
        if (w != null) {
            UiTheme.paintUnstyledText(w.getDecorView(), UiSlot.DIALOG_TEXT);
        }

        TextView title = findTitle(dialog);
        if (title != null) {
            title.setTextColor(titleColor);
            UiTheme.applyFont(title, UiSlot.DIALOG_TITLE);
        }

        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) {
            message.setTextColor(textColor);
            UiTheme.applyFont(message, UiSlot.DIALOG_TEXT);
        }

        // The button bar: every button is a pill. The positive one is the main pill (3 dp, bold) and
        // takes the delete red on a dialog marked destructive; the others are regular pills.
        boolean destructive;
        synchronized (sDestructive) {
            destructive = sDestructive.containsKey(dialog);
        }
        UiPills.dialogButton((Button) dialog.findViewById(android.R.id.button1), true,
                destructive ? UiTheme.color(UiSlot.DELETE) : buttonColor);
        UiPills.dialogButton((Button) dialog.findViewById(android.R.id.button2), false, buttonColor);
        UiPills.dialogButton((Button) dialog.findViewById(android.R.id.button3), false, buttonColor);

        // The dialog's own layout: checkboxes become the outlined switch, text buttons pills.
        if (w != null) {
            UiPills.styleTree(w.getDecorView());
        }

        stylePanel(dialog);
    }

    /**
     * Style {@code dialog} the moment it is shown, and every time it is shown again — the title, message
     * and buttons only exist once the dialog is laid out, so a {@link #style(Dialog)} call made right
     * after {@code create()} would find nothing to paint. This is THE way to theme a framework dialog:
     * {@code return UiDialogStyler.styleOnShow(builder.create());}. Returns the dialog for chaining.
     * <p>
     * It installs an {@link DialogInterface.OnShowListener}, so do not use it on a dialog that needs one
     * of its own — call {@link #style(Dialog)} from that listener instead. A dialog class of our own
     * styles itself in {@code onStart()} rather than using this.
     */
    public static <T extends Dialog> T styleOnShow(T dialog) {
        if (dialog != null) {
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface d) {
                    style((Dialog) d);
                }
            });
        }
        return dialog;
    }

    // Dialogs that delete, remove or reset something: their positive button is a red pill.
    private static final WeakHashMap<Dialog, Boolean> sDestructive = new WeakHashMap<>();

    /**
     * Mark a dialog whose positive button deletes, removes or resets something, so that button is drawn
     * as a red pill. Call it before the dialog is shown; returns the dialog for chaining, e.g.
     * {@code return UiDialogStyler.styleOnShow(UiDialogStyler.destructive(builder.create()));}.
     */
    public static <T extends Dialog> T destructive(T dialog) {
        if (dialog != null) {
            synchronized (sDestructive) {
                sDestructive.put(dialog, Boolean.TRUE);
            }
        }
        return dialog;
    }

    /**
     * The rows of an action list as an adapter, for a dialog that has to stay a {@code setAdapter} /
     * {@code setItems} list (its click and cancel handling is the builder's): every row is the same
     * full-width icon pill {@link #actions} draws. Use it as
     * {@code builder.setAdapter(UiDialogStyler.actionAdapter(context, rows), listener)}.
     */
    public static ListAdapter actionAdapter(final Context context, final List<Action> rows) {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return rows.size();
            }

            @Override
            public Object getItem(int position) {
                return rows.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (parent instanceof ListView) {
                    // the pill carries the press ripple; no rectangular highlight, no hairlines
                    ListView list = (ListView) parent;
                    list.setSelector(new ColorDrawable(0));
                    list.setDivider(null);
                }
                Action row = rows.get(position);
                FrameLayout cell = new FrameLayout(context);
                cell.setPadding(UiPills.dp(context, 8), UiPills.dp(context, 3), UiPills.dp(context, 8), UiPills.dp(context, 3));
                LinearLayout pill = pill(context, row);
                pill.setClickable(false);
                pill.setFocusable(false);
                pill.setDuplicateParentStateEnabled(true);
                cell.addView(pill, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return cell;
            }
        };
    }

    private static LinearLayout pill(Context context, Action row) {
        boolean deletes = row.does == UiIcons.Does.DELETE;
        int color = deletes ? UiTheme.color(UiSlot.DELETE) : UiTheme.color(UiSlot.DIALOG_BUTTON);
        Drawable icon = row.ownIcon != null ? row.ownIcon
                : UiIcons.drawable(context, row.icon == null ? UiIcons.Icon.LIST : row.icon, row.does);
        return UiPills.iconPill(context, row.label, icon, color, false, row.opens, true);
    }

    /** {@link #show(AlertDialog.Builder)} for a dialog that deletes: the positive button is red. */
    public static AlertDialog showDestructive(AlertDialog.Builder builder) {
        AlertDialog dialog = styleOnShow(destructive(builder.create()));
        dialog.show();
        return dialog;
    }

    /** One row of {@link #actions}: an action with its icon, coloured by what it does. */
    public static final class Action {
        final CharSequence label;
        final UiIcons.Icon icon;
        final UiIcons.Does does;
        final boolean opens;
        final Drawable ownIcon;

        /** @param opens the row opens a screen, dialog or picker — it gets a › */
        public Action(CharSequence label, UiIcons.Icon icon, UiIcons.Does does, boolean opens) {
            this(label, icon, does, opens, null);
        }

        /** A row that brings its own icon (an app's), shown as it is. */
        public Action(CharSequence label, Drawable ownIcon, boolean opens) {
            this(label, null, UiIcons.Does.VIEW, opens, ownIcon);
        }

        private Action(CharSequence label, UiIcons.Icon icon, UiIcons.Does does, boolean opens, Drawable ownIcon) {
            this.label = label;
            this.icon = icon;
            this.does = does;
            this.opens = opens;
            this.ownIcon = ownIcon;
        }
    }

    public interface OnAction {
        void onAction(int which);
    }

    /**
     * The themed replacement for {@code builder.setItems(...)} on a list of ACTIONS: every row is a
     * full-width outlined pill with its icon, a › when it opens something, and red when it deletes. (A
     * list of pure choices — a language, a font weight — stays a plain list.) Shows the dialog with a
     * Cancel button and returns it.
     */
    public static AlertDialog actions(Context context, CharSequence title, List<Action> rows, final OnAction onPick) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(UiPills.dp(context, 8), UiPills.dp(context, 6), UiPills.dp(context, 8), UiPills.dp(context, 2));
        ScrollView scroll = new ScrollView(context);
        scroll.addView(column);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setView(scroll);
        builder.setNegativeButton(android.R.string.cancel, null);
        final AlertDialog dialog = styleOnShow(builder.create());

        for (int i = 0; i < rows.size(); i++) {
            final int which = i;
            Action row = rows.get(i);
            LinearLayout pill = pill(context, row);
            pill.setOnClickListener(v -> {
                dialog.dismiss();
                onPick.onAction(which);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = UiPills.dp(context, 3);
            lp.bottomMargin = UiPills.dp(context, 3);
            column.addView(pill, lp);
        }
        dialog.show();
        return dialog;
    }

    /** {@code create()} + {@link #styleOnShow} + {@code show()}: the themed replacement for builder.show(). */
    public static AlertDialog show(AlertDialog.Builder builder) {
        AlertDialog dialog = styleOnShow(builder.create());
        dialog.show();
        return dialog;
    }

    /**
     * The panel only — all a plain {@link Dialog} (no title/message/button views) needs. Safe to call
     * before or after {@code show()}.
     */
    public static void stylePanel(Dialog dialog) {
        if (dialog == null) {
            return;
        }
        Drawable panel = panelBackground(dialog.getContext());
        Window window = dialog.getWindow();
        if (panel != null && window != null) {
            window.setBackgroundDrawable(panel);
        }
    }

    /**
     * The dialog panel background: the {@link UiSlot#DIALOG_BG} fill, rounded, with a
     * {@link UiSlot#DIALOG_BORDER} stroke. Returns {@code null} to keep the platform default panel —
     * i.e. when the border width is 0 AND the user has not overridden the dialog background — so an
     * untouched install still shows the theme's plain rounded black panel. Out of the box the border is
     * {@link UiTheme#DEFAULT_BORDER_DP}dp yellow, so this normally returns a bordered panel.
     */
    public static Drawable panelBackground(Context ctx) {
        int borderDp = UiTheme.borderWidthDp(UiSlot.DIALOG_BORDER);
        boolean bgOverridden = UiConfig.get().hasOverride(UiSlot.DIALOG_BG.key);
        if (borderDp <= 0 && !bgOverridden) {
            return null;
        }
        float d = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(UiTheme.color(UiSlot.DIALOG_BG));
        panel.setCornerRadius(UiTheme.cornerRadiusDp(UiSlot.DIALOG_BORDER) * d);
        if (borderDp > 0) {
            panel.setStroke(Math.round(borderDp * d), UiTheme.color(UiSlot.DIALOG_BORDER));
        }
        return panel;
    }

    // The title view has no public id: the framework layout calls it "android:id/alertTitle", AppCompat's
    // calls it "<app package>:id/alertTitle" (library ids are merged into the app's R). Try both.
    private static TextView findTitle(Dialog dialog) {
        Context ctx = dialog.getContext();
        String[] packages = {"android", ctx.getPackageName()};
        for (String pkg : packages) {
            int id = ctx.getResources().getIdentifier("alertTitle", "id", pkg);
            if (id != 0) {
                View v = dialog.findViewById(id);
                if (v instanceof TextView) {
                    return (TextView) v;
                }
            }
        }
        return null;
    }
}
