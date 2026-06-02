package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.Menu;

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

    private static void tint(Drawable d, int color) {
        if (d != null) {
            d.mutate().setTint(color);
        }
    }
}
