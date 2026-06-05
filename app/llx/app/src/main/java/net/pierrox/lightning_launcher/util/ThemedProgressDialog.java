package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
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

    private ThemedProgressDialog() {
    }

    /** Build and show a non-cancelable themed progress dialog. */
    public static AlertDialog show(Context ctx, CharSequence message) {
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

        dialog.show();
        return dialog;
    }
}
