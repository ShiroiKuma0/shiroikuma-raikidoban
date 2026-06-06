package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher_extreme.R;

/**
 * Paints AND sizes the geometry box ({@code @layout/geometry_box}) from 「白い熊 雷起動盤 UI」, split per
 * region: the panel, the top value tiles ({@code gb_e1/gb_e2}), the +/- cross
 * ({@code gb_vm/gb_hm/gb_m/gb_hp/gb_vp}) and the bottom z-order buttons ({@code move_*}). Each region
 * has its own background / border (width + corner) / content colour, plus a dp size. Used both for the
 * live editor box (Dashboard) and the inline preview in {@code UiSettingsActivity} so they always match.
 *
 * Colours/borders/corners come from the per-region {@link UiSlot}s; sizes from {@link UiConfig#getSize}
 * under the keys below. Everything has a default that reproduces today's yellow-on-black look, so an
 * untouched config leaves the box exactly as shipped.
 */
public final class GeometryBoxStyler {

    // dp-size keys (UiConfig.getSize) and their unset defaults.
    public static final String KEY_CROSS = "geom_cross";
    public static final String KEY_ZORDER = "geom_zorder";
    public static final String KEY_TILE = "geom_tile";
    public static final String KEY_PANEL_WIDTH = "geom_panel_width";

    public static final int DEFAULT_CROSS_DP = 96;
    public static final int DEFAULT_ZORDER_DP = 80;
    public static final int DEFAULT_TILE_DP = 72;
    public static final int DEFAULT_PANEL_WIDTH_DP = 0; // 0 = auto (wrap to content)

    private GeometryBoxStyler() {
    }

    public static void style(View gb) {
        if (gb == null) {
            return;
        }
        Context ctx = gb.getContext();
        float d = ctx.getResources().getDisplayMetrics().density;
        UiConfig cfg = UiConfig.get();

        // --- panel: background + border + (optional explicit) width ---
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(UiTheme.color(UiSlot.GEOM_PANEL_BG));
        panel.setCornerRadius(UiTheme.cornerRadiusDp(UiSlot.GEOM_PANEL_BORDER) * d);
        int panelBorder = UiTheme.borderWidthDp(UiSlot.GEOM_PANEL_BORDER);
        if (panelBorder > 0) {
            panel.setStroke(Math.round(panelBorder * d), UiTheme.color(UiSlot.GEOM_PANEL_BORDER));
        }
        gb.setBackground(panel);
        int pad = Math.round(8 * d);
        gb.setPadding(pad, pad, pad, pad);

        int panelWidthDp = resolve(cfg.getSize(KEY_PANEL_WIDTH), DEFAULT_PANEL_WIDTH_DP);
        ViewGroup.LayoutParams glp = gb.getLayoutParams();
        if (glp != null) {
            glp.width = panelWidthDp > 0 ? Math.round(panelWidthDp * d) : ViewGroup.LayoutParams.WRAP_CONTENT;
            gb.setLayoutParams(glp);
        }

        // --- top value tiles: fill + border + text colour/font/size + size (min height) ---
        int tileH = Math.round(resolve(cfg.getSize(KEY_TILE), DEFAULT_TILE_DP) * d);
        for (int id : new int[]{R.id.gb_e1, R.id.gb_e2}) {
            TextView t = gb.findViewById(id);
            if (t == null) {
                continue;
            }
            t.setBackground(tile(d, UiSlot.GEOM_TILE_BG, UiSlot.GEOM_TILE_BORDER));
            UiTheme.applyTo(t, UiSlot.GEOM_TILE_TEXT);
            t.setMinHeight(tileH);
        }

        // --- +/- cross: fill + border + glyph colour + size (square) ---
        int crossDp = resolve(cfg.getSize(KEY_CROSS), DEFAULT_CROSS_DP);
        int crossPx = Math.round(crossDp * d);
        int glyphColor = UiTheme.color(UiSlot.GEOM_CROSS_GLYPH);
        int glyphSp = Math.round(crossDp * 0.45f);
        for (int id : new int[]{R.id.gb_vm, R.id.gb_hm, R.id.gb_m, R.id.gb_hp, R.id.gb_vp}) {
            TextView b = gb.findViewById(id);
            if (b == null) {
                continue;
            }
            b.setBackground(tile(d, UiSlot.GEOM_CROSS_BG, UiSlot.GEOM_CROSS_BORDER));
            b.setTextColor(glyphColor);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, glyphSp);
            setSize(b, crossPx, crossPx);
        }

        // --- bottom z-order buttons: fill + border + icon tint + row height. MULTIPLY keeps the
        // highlighted-layer bar and the icon outlines readable instead of flat-filling them. ---
        int iconColor = UiTheme.color(UiSlot.GEOM_ZORDER_ICON);
        for (int id : new int[]{R.id.move_bottom, R.id.move_down, R.id.move_up, R.id.move_top}) {
            ImageButton ib = gb.findViewById(id);
            if (ib == null) {
                continue;
            }
            ib.setBackground(tile(d, UiSlot.GEOM_ZORDER_BG, UiSlot.GEOM_ZORDER_BORDER));
            ib.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
        }
        View row = gb.findViewById(R.id.gb_zorder_row);
        if (row != null) {
            ViewGroup.LayoutParams rlp = row.getLayoutParams();
            if (rlp != null) {
                rlp.height = Math.round(resolve(cfg.getSize(KEY_ZORDER), DEFAULT_ZORDER_DP) * d);
                row.setLayoutParams(rlp);
            }
        }
    }

    // Flat fill + (optional) border stroke with the slot's configurable width and corner radius.
    private static GradientDrawable tile(float d, UiSlot bg, UiSlot border) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiTheme.color(bg));
        g.setCornerRadius(UiTheme.cornerRadiusDp(border) * d);
        int w = UiTheme.borderWidthDp(border);
        if (w > 0) {
            g.setStroke(Math.round(w * d), UiTheme.color(border));
        }
        return g;
    }

    private static void setSize(View v, int w, int h) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            lp.width = w;
            lp.height = h;
            v.setLayoutParams(lp);
        }
    }

    private static int resolve(int stored, int def) {
        return stored < 0 ? def : stored;
    }
}
