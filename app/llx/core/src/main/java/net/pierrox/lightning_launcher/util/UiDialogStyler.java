package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
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

        styleButton(dialog.findViewById(android.R.id.button1), buttonColor);
        styleButton(dialog.findViewById(android.R.id.button2), buttonColor);
        styleButton(dialog.findViewById(android.R.id.button3), buttonColor);

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

    private static void styleButton(Button button, int color) {
        if (button != null) {
            button.setTextColor(color);
            UiTheme.applyFont(button, UiSlot.DIALOG_BUTTON);
        }
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
