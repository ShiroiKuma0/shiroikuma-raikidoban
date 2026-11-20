package net.pierrox.lightning_launcher.util;

import net.pierrox.lightning_launcher.configuration.UiConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 白い熊: where the 「白い熊 雷起動盤 UI」 page keeps its section order and which sections are unfolded.
 * Two comma-separated strings in the "llui" preferences — the file the export writes whole — so a
 * restore brings the arrangement back with the colours and fonts. Every section is folded until its key
 * is in the open list.
 */
public final class UiPageState {

    private static final String KEY_ORDER = "ui_section_order";
    private static final String KEY_OPEN = "ui_section_open";

    private UiPageState() {
    }

    /**
     * The sections in display order: the stored order as far as it still names existing sections, with
     * every section added since slotted in right after the one it follows in {@code built}.
     */
    public static List<String> order(List<String> built) {
        List<String> saved = split(UiConfig.get().getString(KEY_ORDER));
        List<String> out = new ArrayList<>();
        for (String k : saved) {
            if (built.contains(k) && !out.contains(k)) {
                out.add(k);
            }
        }
        if (out.isEmpty()) {
            return new ArrayList<>(built);
        }
        for (int i = 0; i < built.size(); i++) {
            String k = built.get(i);
            if (out.contains(k)) {
                continue;
            }
            int at = 0;
            for (int j = i - 1; j >= 0; j--) {
                int p = out.indexOf(built.get(j));
                if (p >= 0) {
                    at = p + 1;
                    break;
                }
            }
            out.add(at, k);
        }
        return out;
    }

    public static void setOrder(List<String> order) {
        UiConfig.get().setString(KEY_ORDER, join(order));
    }

    public static void resetOrder() {
        UiConfig.get().setString(KEY_ORDER, null);
    }

    public static boolean isOpen(String key) {
        return split(UiConfig.get().getString(KEY_OPEN)).contains(key);
    }

    public static void setOpen(String key, boolean open) {
        Set<String> set = new LinkedHashSet<>(split(UiConfig.get().getString(KEY_OPEN)));
        if (open) {
            set.add(key);
        } else {
            set.remove(key);
        }
        UiConfig.get().setString(KEY_OPEN, join(new ArrayList<>(set)));
    }

    private static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static String join(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(k);
        }
        return sb.toString();
    }
}
