package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiFonts;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.List;

/**
 * Lists every available font with its name drawn in its own typeface, plus an "Add font…" action.
 * Mirrors the sister repos' FontPickerDialog. Black-yellow themed via {@link UiTheme} (DIALOG slots).
 */
public final class UiFontPickerDialog {

    public interface Callback {
        void onPick(String fileName);

        void onAddFont();
    }

    public UiFontPickerDialog(Activity activity, Callback callback) {
        float d = activity.getResources().getDisplayMetrics().density;
        int textColor = UiTheme.color(UiSlot.DIALOG_TEXT);
        int accent = UiTheme.color(UiSlot.DIALOG_BUTTON);
        int padH = Math.round(20 * d);
        int padV = Math.round(12 * d);

        LinearLayout holder = new LinearLayout(activity);
        holder.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(holder);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.theme_font)
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        List<UiFonts.Option> options = UiFonts.availableFontOptions(activity);
        for (final UiFonts.Option option : options) {
            TextView row = new TextView(activity);
            row.setText(option.displayName);
            row.setTextColor(textColor);
            row.setTextSize(18);
            row.setTypeface(UiFonts.fontTypeface(option.fileName));
            row.setPadding(padH, padV, padH, padV);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    callback.onPick(option.fileName);
                }
            });
            holder.addView(row);
        }

        TextView add = new TextView(activity);
        add.setText(R.string.theme_add_font);
        add.setTextColor(accent);
        add.setTextSize(18);
        add.setGravity(Gravity.START);
        add.setPadding(padH, padV, padH, padV);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onAddFont();
            }
        });
        holder.addView(add);

        dialog.show();
        UiDialogStyler.style(dialog);
    }
}
