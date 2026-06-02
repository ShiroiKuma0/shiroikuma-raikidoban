package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Menu;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

/**
 * Applies the TOOLBARS slots of 「白い熊 雷起動盤 UI」 to an AppCompat toolbar, its menu icons and the
 * status bar. Used by the Settings / Customize chrome so toolbar colours follow the config.
 */
public final class UiChrome {

    private UiChrome() {
    }

    public static void applyToolbar(Toolbar toolbar) {
        if (toolbar == null) {
            return;
        }
        int icon = UiTheme.color(UiSlot.TOOLBAR_ICON);
        toolbar.setBackgroundColor(UiTheme.color(UiSlot.TOOLBAR_BG));
        toolbar.setTitleTextColor(UiTheme.color(UiSlot.TOOLBAR_TEXT));
        tint(toolbar.getNavigationIcon(), icon);
        tint(toolbar.getOverflowIcon(), icon);
    }

    public static void tintMenu(Menu menu) {
        if (menu == null) {
            return;
        }
        int icon = UiTheme.color(UiSlot.TOOLBAR_ICON);
        for (int i = 0; i < menu.size(); i++) {
            tint(menu.getItem(i).getIcon(), icon);
        }
    }

    public static void applyStatusBar(Activity activity) {
        if (activity != null) {
            activity.getWindow().setStatusBarColor(UiTheme.color(UiSlot.STATUSBAR_BG));
        }
    }

    /**
     * Style a push button with the BUTTONS slots: BUTTON_BG fill, BUTTON_BORDER stroke (width 0 = none)
     * with the configurable corner radius, and BUTTON_TEXT colour + font. Replaces the platform's grey
     * button background, so re-add some padding for the text.
     */
    public static void applyButton(TextView button) {
        if (button == null) {
            return;
        }
        float d = button.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiTheme.color(UiSlot.BUTTON_BG));
        bg.setCornerRadius(UiTheme.cornerRadiusDp(UiSlot.BUTTON_BORDER) * d);
        int width = UiTheme.borderWidthDp(UiSlot.BUTTON_BORDER);
        if (width > 0) {
            bg.setStroke(Math.round(width * d), UiTheme.color(UiSlot.BUTTON_BORDER));
        }
        button.setBackground(bg);
        button.setTextColor(UiTheme.color(UiSlot.BUTTON_TEXT));
        UiTheme.applyFont(button, UiSlot.BUTTON_TEXT);
        int padH = Math.round(16 * d);
        int padV = Math.round(10 * d);
        button.setPadding(padH, padV, padH, padV);
    }

    private static void tint(Drawable d, int color) {
        if (d != null) {
            d.mutate().setTint(color);
        }
    }
}
