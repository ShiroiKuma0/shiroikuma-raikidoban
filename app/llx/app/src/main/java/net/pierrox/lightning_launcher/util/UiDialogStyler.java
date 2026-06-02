package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.content.Context;
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
 * Applies the DIALOG_* {@link UiSlot}s to an already-shown framework {@link AlertDialog}: title, message
 * and button colours/fonts, and (only when the user set an explicit DIALOG background) a rounded panel
 * in that colour. With nothing customized the night theme's yellow-on-black is left untouched.
 */
public final class UiDialogStyler {

    private UiDialogStyler() {
    }

    public static void style(AlertDialog dialog) {
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

        styleButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), buttonColor);
        styleButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), buttonColor);
        styleButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), buttonColor);

        Drawable panel = panelBackground(dialog.getContext());
        if (panel != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(panel);
            }
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
        panel.setCornerRadius(12 * d);
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

    private static TextView findTitle(AlertDialog dialog) {
        int id = dialog.getContext().getResources().getIdentifier("alertTitle", "id", "android");
        if (id != 0) {
            View v = dialog.findViewById(id);
            if (v instanceof TextView) {
                return (TextView) v;
            }
        }
        return null;
    }
}
