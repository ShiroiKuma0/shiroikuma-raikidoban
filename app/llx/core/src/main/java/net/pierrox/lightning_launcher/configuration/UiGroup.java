package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.R;

/** A logical section in the 「白い熊 雷起動盤 UI」 screen: foundation + one group per chrome surface. */
public enum UiGroup {
    FOUNDATION(R.string.llui_group_foundation),
    MENUS(R.string.llui_group_menus),
    DIALOGS(R.string.llui_group_dialogs),
    SETTINGS(R.string.llui_group_settings),
    TOOLBARS(R.string.llui_group_toolbars);

    public final int labelRes;

    UiGroup(int labelRes) {
        this.labelRes = labelRes;
    }
}
