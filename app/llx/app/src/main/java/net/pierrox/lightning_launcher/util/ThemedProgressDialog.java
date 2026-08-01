package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

/**
 * Black-yellow replacement for the framework {@link android.app.ProgressDialog} (deprecated, and its
 * internal message TextView isn't reliably reachable on this device). Builds a small themed panel — an
 * indeterminate spinner next to a message — styled from the DIALOG_* {@link UiSlot}s so the
 * "Backing up…" / "Restoring…" dialogs obey the same 「白い熊 雷起動盤 UI」 controls as every other dialog:
 * the message takes DIALOG_TEXT (font family / weight / size + colour), the panel is DIALOG_BG with the
 * DIALOG_BORDER stroke (colour + width) and corner radius, and the spinner is tinted with DIALOG_TEXT.
 */
public final class ThemedProgressDialog {

    /** Tag on the message TextView, so {@link #setMessage} can find it again inside the custom view. */
    private static final String MESSAGE_TAG = "rkb.progress.message";

    private ThemedProgressDialog() {
    }

    /** Build and show a non-cancelable themed progress dialog. */
    public static AlertDialog show(Context ctx, CharSequence message) {
        AlertDialog dialog = create(ctx, message);
        dialog.show();
        return dialog;
    }

    /**
     * Build the dialog without showing it — for {@code onCreateDialog(int)}, which must return a dialog
     * the framework then shows itself.
     */
    public static AlertDialog create(Context ctx, CharSequence message) {
        float d = ctx.getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Math.round(20 * d);
        row.setPadding(pad, pad, pad, pad);

        ProgressBar spinner = new ProgressBar(ctx);
        spinner.setIndeterminate(true);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(UiTheme.color(UiSlot.DIALOG_TEXT)));
        int spinnerSize = Math.round(36 * d);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(spinnerSize, spinnerSize);
        sp.rightMargin = Math.round(16 * d);
        row.addView(spinner, sp);

        TextView text = new TextView(ctx);
        text.setText(message);
        text.setTag(MESSAGE_TAG);
        UiTheme.applyTo(text, UiSlot.DIALOG_TEXT);
        row.addView(text, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(row)
                .setCancelable(false)
                .create();

        // Same bordered/rounded panel the other themed dialogs use (DIALOG_BG + DIALOG_BORDER).
        Drawable panel = UiDialogStyler.panelBackground(ctx);
        Window window = dialog.getWindow();
        if (window != null && panel != null) {
            window.setBackgroundDrawable(panel);
        }

        return dialog;
    }

    /**
     * Replace the message of a dialog built here — the stand-in for {@code ProgressDialog.setMessage},
     * which does nothing on a dialog whose content is a custom view. No-op for any other dialog.
     */
    public static void setMessage(Dialog dialog, CharSequence message) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        View v = dialog.getWindow().getDecorView().findViewWithTag(MESSAGE_TAG);
        if (v instanceof TextView) {
            ((TextView) v).setText(message);
        }
    }
}
