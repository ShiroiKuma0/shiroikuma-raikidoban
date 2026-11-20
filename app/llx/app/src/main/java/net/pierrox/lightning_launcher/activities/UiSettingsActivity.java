/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.configuration.UiFonts;
import net.pierrox.lightning_launcher.configuration.UiGroup;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.data.RkbExport;
import net.pierrox.lightning_launcher.util.AutomationAuth;
import net.pierrox.lightning_launcher.util.ExportImportPanel;
import net.pierrox.lightning_launcher.util.Flash;
import net.pierrox.lightning_launcher.util.GeometryBoxStyler;
import net.pierrox.lightning_launcher.util.UiFontPickerDialog;
import net.pierrox.lightning_launcher.util.UiIcons;
import net.pierrox.lightning_launcher.util.UiPageState;
import net.pierrox.lightning_launcher.util.UiPills;
import net.pierrox.lightning_launcher.util.UiSectionIndex;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「白い熊 雷起動盤 UI」 — the launcher appearance screen. A section per chrome surface (foundation +
 * menus + dialogs + settings + toolbars); each section's slots are indented under it. A colour slot is a
 * label + swatch (tap → colour picker, long-press → reset); a text slot adds font / weight / size rows
 * and a live sample. The screen dogfoods the config, so editing a foundation value repaints it via
 * {@link #recreate()}. Reimplements the sister repos' ThemeActivity in Java/Views.
 */
public class UiSettingsActivity extends ResourceWrapperActivity {

    // Big visual indents: a section's slots sit one step in, a text slot's sub-rows one step deeper.
    private static final int INDENT_STEP_DP = 40;

    private float mDensity;
    private int mStepPx;
    private LinearLayout mRoot;
    private Toolbar mToolbar;
    private LinearLayout mHolder;
    private ScrollView mScroll;
    /** Where the row builders append: the folding body of the section being built. */
    private LinearLayout mBody;

    // The folding accordion: every top-level section by its stable key.
    private final Map<String, View> mSectionWrappers = new HashMap<>();
    private final Map<String, View> mSectionBodies = new HashMap<>();
    private final Map<String, TextView> mSectionChevrons = new HashMap<>();

    private final Map<UiSlot, View> mSwatches = new HashMap<>();
    private final Map<UiSlot, TextSlotViews> mTextSlots = new HashMap<>();

    /** One inline geometry-box preview per region, each above its sub-section, re-styled live. */
    private static final class GeomPreview {
        View view;
        int forceWidthDp; // 0 = let the styler decide the width (else a fixed preview width)
    }

    private final List<GeomPreview> mGeometryPreviews = new ArrayList<>();

    private UiSlot mPendingFontSlot;
    private final ActivityResultLauncher<String[]> mFontImport =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFontImported);

    /** The Export / Import window (see {@link ExportImportPanel}) and its two SAF pickers. */
    private ExportImportPanel mExportImport;
    private final ActivityResultLauncher<Uri> mPickExportDir =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::onExportDirPicked);
    private final ActivityResultLauncher<String[]> mPickImportFile =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onImportFilePicked);

    /** A missing backup destination is shown in the delete / warning red — a settable role like every other colour. */
    private static int warnColor() {
        return UiTheme.color(UiSlot.DELETE);
    }

    private static final class TextSlotViews {
        TextView fontValue, weightValue, sizeValue, sample;
        View swatch;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDensity = getResources().getDisplayMetrics().density;
        mStepPx = Math.round(INDENT_STEP_DP * mDensity);

        mRoot = new LinearLayout(this);
        mRoot.setOrientation(LinearLayout.VERTICAL);

        mToolbar = new Toolbar(this);
        mToolbar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mRoot.addView(mToolbar);

        ScrollView scroll = new ScrollView(this);
        mScroll = scroll;
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mHolder = new LinearLayout(this);
        mHolder.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        mHolder.setPadding(pad, pad, pad, dp(32));
        mBody = mHolder;
        scroll.addView(mHolder);
        mRoot.addView(scroll);

        setContentView(mRoot);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.llui_title);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyChrome();
        buildRows();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int dp(float v) {
        return Math.round(v * mDensity);
    }

    // --- chrome (this screen dogfoods the config) ---

    private void applyChrome() {
        mRoot.setBackgroundColor(UiTheme.color(UiSlot.BACKGROUND));
        mToolbar.setBackgroundColor(UiTheme.color(UiSlot.TOOLBAR_BG));
        mToolbar.setTitleTextColor(UiTheme.color(UiSlot.TOOLBAR_TEXT));
        Drawable nav = mToolbar.getNavigationIcon();
        if (nav != null) {
            nav.setTint(UiTheme.color(UiSlot.TOOLBAR_ICON));
        }
        getWindow().setStatusBarColor(UiTheme.color(UiSlot.STATUSBAR_BG));
        getWindow().setNavigationBarColor(UiTheme.color(UiSlot.BACKGROUND));
    }

    // --- row building ---

    /**
     * The page is a folding accordion with a chip index on top (the 白い熊 house rule for every UI
     * page): the link to the launcher's own settings, then one small chip per section — tap to unfold
     * and jump, long-press to drag the sections into another order — then the sections, every one
     * folded until opened. Order and fold state live in the "llui" preferences, so they travel in the
     * export.
     */
    private void buildRows() {
        mHolder.removeAllViews();
        mSwatches.clear();
        mTextSlots.clear();
        mGeometryPreviews.clear();
        mSectionWrappers.clear();
        mSectionBodies.clear();
        mSectionChevrons.clear();

        // The launcher's own settings: the first, thick pill. Not a section — it stays pinned here.
        mBody = mHolder;
        addPillRow(getString(R.string.llui_app_settings), getString(R.string.llui_app_settings_desc),
                UiIcons.Icon.GEAR, UiIcons.Does.VIEW, true, true, false, 0,
                v -> startActivity(new Intent(this, net.pierrox.lightning_launcher.feature.settings.RootSettings.class)));

        // The registry: key → label + what fills the section.
        final List<String> keys = new ArrayList<>();
        final Map<String, String> labels = new HashMap<>();
        final Map<String, Runnable> content = new HashMap<>();
        reg(keys, labels, content, "eim", getString(R.string.rkb_ui_section_eim), this::fillExportImportSection);
        reg(keys, labels, content, "lang", getString(R.string.llui_language), this::fillLanguageSection);
        for (final UiGroup group : UiGroup.values()) {
            reg(keys, labels, content, group.name().toLowerCase(java.util.Locale.ROOT), getString(group.labelRes),
                    () -> fillGroupSection(group));
        }
        reg(keys, labels, content, "maint", getString(R.string.llui_group_maint), this::fillMaintenanceSection);

        List<String> order = UiPageState.order(keys);

        // The chip index.
        List<UiSectionIndex.Item> items = new ArrayList<>();
        for (String k : order) {
            items.add(new UiSectionIndex.Item(k, labels.get(k)));
        }
        View index = UiSectionIndex.build(this, items, UiTheme.color(UiSlot.ACCENT), new UiSectionIndex.Listener() {
            @Override
            public void onJump(String key) {
                jumpToSection(key);
            }

            @Override
            public void onReordered(List<String> newOrder) {
                UiPageState.setOrder(newOrder);
                buildRows();
            }
        });
        LinearLayout indexWrap = new LinearLayout(this);
        indexWrap.setPadding(dp(8), dp(6), dp(8), dp(2));
        indexWrap.addView(index, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mHolder.addView(indexWrap);

        for (String k : order) {
            openSection(k, labels.get(k));
            content.get(k).run();
        }
        mBody = mHolder;
    }

    private static void reg(List<String> keys, Map<String, String> labels, Map<String, Runnable> content,
                            String key, String label, Runnable fill) {
        keys.add(key);
        labels.put(key, label);
        content.put(key, fill);
    }

    private void fillGroupSection(UiGroup group) {
        if (group == UiGroup.GEOMETRY) {
            // Bespoke layout: a live preview, per-region sub-headers, and dp size sliders.
            addGeometrySection();
            return;
        }
        for (UiSlot slot : UiSlot.forGroup(group)) {
            if (slot.isBorder()) {
                // Border slots = colour + width slider + corner-roundness slider.
                addBorderSlot(slot, mStepPx);
                addCornerRow(slot, mStepPx);
            } else if (slot.hasFont) {
                addTextSlot(slot, mStepPx);
            } else {
                addColorRow(slot, mStepPx);
            }
        }
    }

    /** Maintenance: put the sections back in their original order; how to reset a single value. */
    private void fillMaintenanceSection() {
        addPillRow(getString(R.string.llui_reset_order), getString(R.string.llui_reset_order_desc),
                UiIcons.Icon.RESET, UiIcons.Does.VIEW, false, false, false, mStepPx, v -> {
                    UiPageState.resetOrder();
                    buildRows();
                    Flash.show(this, R.string.llui_reset_order_done);
                });
        addNote(getString(R.string.llui_reset_hint), mStepPx);
    }

    /**
     * Start a top-level section: the 1 px darker-accent divider (kept whether the section is folded or
     * open), the heading — 20 sp bold accent, underlined exactly as wide as its text, with › when
     * folded and ⌄ when open — and the folding body the row builders then fill.
     */
    private void openSection(final String key, String labelText) {
        int accent = UiTheme.color(UiSlot.ACCENT);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, dp(10), 0, dp(2));

        View line = new View(this);
        line.setBackgroundColor(UiPills.divider());
        wrapper.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(8), dp(8), dp(2));

        LinearLayout labelCol = new LinearLayout(this);
        labelCol.setOrientation(LinearLayout.VERTICAL);
        TextView label = makeLabel(labelText, accent);
        label.setTextSize(20);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setMaxLines(1);
        labelCol.addView(label);
        labelCol.addView(underline(label, Math.max(1, dp(2.5f)), accent));
        header.addView(labelCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView chevron = new TextView(this);
        chevron.setTextSize(22);
        chevron.setTextColor(accent);
        chevron.setIncludeFontPadding(false);
        chevron.setPadding(dp(10), 0, dp(4), 0);
        header.addView(chevron);
        header.setOnClickListener(v -> setSectionOpen(key, !UiPageState.isOpen(key)));
        wrapper.addView(header);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(inner);

        boolean open = UiPageState.isOpen(key);
        inner.setVisibility(open ? View.VISIBLE : View.GONE);
        chevron.setText(open ? "\u2304" : "\u203A");

        mHolder.addView(wrapper);
        mSectionWrappers.put(key, wrapper);
        mSectionBodies.put(key, inner);
        mSectionChevrons.put(key, chevron);
        mBody = inner;
    }

    private void setSectionOpen(String key, boolean open) {
        UiPageState.setOpen(key, open);
        View inner = mSectionBodies.get(key);
        TextView chevron = mSectionChevrons.get(key);
        if (inner != null) {
            inner.setVisibility(open ? View.VISIBLE : View.GONE);
        }
        if (chevron != null) {
            chevron.setText(open ? "\u2304" : "\u203A");
        }
    }

    /** A chip was tapped: unfold its section and scroll to it. */
    private void jumpToSection(String key) {
        final View wrapper = mSectionWrappers.get(key);
        if (wrapper == null) {
            return;
        }
        setSectionOpen(key, true);
        mHolder.post(() -> mScroll.smoothScrollTo(0, wrapper.getTop()));
    }

    // --- the row kinds of the black-yellow settings page ---

    /** A 20 dp icon at the start of a row, coloured by what the row does. */
    private void addRowIcon(LinearLayout row, UiIcons.Icon icon, UiIcons.Does does) {
        View v = new View(this);
        v.setBackground(UiIcons.drawable(this, icon, does));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(20), dp(20));
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.setMarginEnd(dp(10));
        row.addView(v, 0, lp);
    }

    /**
     * An action / navigation row: an outlined pill that hugs its label, the icon inside it, and the
     * explanation beneath in dim gold, aligned with the pill's content. Only the pill is the touch
     * target.
     *
     * @param main        the main action of its section: 3 dp frame, bold
     * @param opens       it opens a screen, picker or panel: a › at the right end of the pill
     * @param destructive it deletes or revokes something: frame, icon and label in the delete red
     */
    private void addPillRow(String title, @Nullable String description, UiIcons.Icon icon, UiIcons.Does does,
                            boolean main, boolean opens, boolean destructive, int indent,
                            View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(indent + dp(8), dp(3), dp(14), dp(3));

        int color = destructive ? UiTheme.color(UiSlot.DELETE) : UiTheme.color(UiSlot.ACCENT);
        LinearLayout pill = UiPills.iconPill(this, title, UiIcons.drawable(this, icon,
                destructive ? UiIcons.Does.DELETE : does), color, main, opens, false);
        pill.setPadding(dp(16), dp(9), dp(16), dp(9));
        pill.setOnClickListener(onClick);
        row.addView(pill, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (description != null) {
            TextView desc = makeLabel(description, UiTheme.color(UiSlot.NOTE));
            desc.setTextSize(13);
            desc.setPadding(dp(16), dp(2), 0, 0);
            row.addView(desc);
        }
        mBody.addView(row);
    }

    /** A loose note that belongs to no single row: dim gold, no rule. */
    private TextView addNote(CharSequence text, int indent) {
        TextView note = makeLabel(text, UiTheme.color(UiSlot.NOTE));
        note.setTextSize(12);
        note.setPadding(indent + dp(8), dp(6), dp(14), dp(2));
        mBody.addView(note);
        return note;
    }

    /** The thin dim-gold bar down the left edge of an explained row. */
    private void addLeftRule(LinearLayout row) {
        View rule = new View(this);
        rule.setBackgroundColor(UiTheme.color(UiSlot.NOTE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(2), ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMarginEnd(dp(10));
        row.addView(rule, 0, lp);
        row.setPadding(Math.max(0, row.getPaddingLeft() - dp(12)), row.getPaddingTop(),
                row.getPaddingRight(), row.getPaddingBottom());
    }

    /** An underline measured to the label's text width, so it never stretches to the whole row. */
    private View underline(TextView label, int height, int color) {
        label.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        View rule = new View(this);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(label.getMeasuredWidth(), height);
        rlp.topMargin = dp(3);
        rule.setLayoutParams(rlp);
        rule.setBackgroundColor(color);
        return rule;
    }

    // --- geometry box (split per region: panel / top tiles / cross / layer buttons) + live preview ---

    private void addGeometrySection() {
        // Sub-sections are indented inside the group, with a rule-less heading (the group already drew
        // the only separator). Each region shows its own preview just above its controls, so it visibly
        // changes as the sliders move. Rows sit one step deeper than their sub-heading.
        int sub = mStepPx / 2;

        // Box (panel): background, border (colour + width + corner), and panel pixel width (0 = auto).
        // Preview = the whole box, honouring the configured width so the Box-width slider moves it.
        addRegionPreview(true, true, true, 0);
        addSubHeader(getString(R.string.llui_geom_sub_panel), sub);
        addColorRow(UiSlot.GEOM_PANEL_BG, mStepPx);
        addBorderSlot(UiSlot.GEOM_PANEL_BORDER, mStepPx);
        addCornerRow(UiSlot.GEOM_PANEL_BORDER, mStepPx);
        addSizeRow(getString(R.string.llui_geom_panel_width), GeometryBoxStyler.KEY_PANEL_WIDTH,
                GeometryBoxStyler.DEFAULT_PANEL_WIDTH_DP, UiConfig.MAX_GEOM_PANEL_WIDTH_DP, mStepPx, true);

        // Top value tiles: background, border, text (colour + font + size), and tile size.
        addRegionPreview(true, false, false, GEOM_PREVIEW_WIDTH_DP);
        addSubHeader(getString(R.string.llui_geom_sub_tiles), sub);
        addColorRow(UiSlot.GEOM_TILE_BG, mStepPx);
        addBorderSlot(UiSlot.GEOM_TILE_BORDER, mStepPx);
        addCornerRow(UiSlot.GEOM_TILE_BORDER, mStepPx);
        addTextSlot(UiSlot.GEOM_TILE_TEXT, mStepPx);
        addSizeRow(getString(R.string.llui_geom_tile_size), GeometryBoxStyler.KEY_TILE,
                GeometryBoxStyler.DEFAULT_TILE_DP, UiConfig.MAX_GEOM_ELEMENT_DP, mStepPx, false);

        // +/- cross: background, border, glyph colour, and button size.
        addRegionPreview(false, true, false, 0);
        addSubHeader(getString(R.string.llui_geom_sub_cross), sub);
        addColorRow(UiSlot.GEOM_CROSS_BG, mStepPx);
        addBorderSlot(UiSlot.GEOM_CROSS_BORDER, mStepPx);
        addCornerRow(UiSlot.GEOM_CROSS_BORDER, mStepPx);
        addColorRow(UiSlot.GEOM_CROSS_GLYPH, mStepPx);
        addSizeRow(getString(R.string.llui_geom_cross_size), GeometryBoxStyler.KEY_CROSS,
                GeometryBoxStyler.DEFAULT_CROSS_DP, UiConfig.MAX_GEOM_ELEMENT_DP, mStepPx, false);

        // Bottom layer-ordering buttons: background, border, icon colour, and button size.
        addRegionPreview(false, false, true, GEOM_PREVIEW_WIDTH_DP);
        addSubHeader(getString(R.string.llui_geom_sub_zorder), sub);
        addColorRow(UiSlot.GEOM_ZORDER_BG, mStepPx);
        addBorderSlot(UiSlot.GEOM_ZORDER_BORDER, mStepPx);
        addCornerRow(UiSlot.GEOM_ZORDER_BORDER, mStepPx);
        addColorRow(UiSlot.GEOM_ZORDER_ICON, mStepPx);
        addSizeRow(getString(R.string.llui_geom_zorder_size), GeometryBoxStyler.KEY_ZORDER,
                GeometryBoxStyler.DEFAULT_ZORDER_DP, UiConfig.MAX_GEOM_ELEMENT_DP, mStepPx, false);
    }

    // An indented sub-heading inside a group (no full-width spacer — that marks top-level sections
    // only), text-width underlined like every other heading on the page.
    private void addSubHeader(String text, int indent) {
        int accent = UiTheme.color(UiSlot.ACCENT);
        TextView label = makeLabel(text, accent);
        label.setTextSize(16);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setMaxLines(1);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(indent + dp(8), dp(10), dp(8), dp(2));
        box.addView(label);
        box.addView(underline(label, Math.max(1, dp(1.5f)), accent));
        mBody.addView(box);
    }

    // Fixed preview width for regions whose buttons use layout weights (tiles, z-order row) so they have
    // a width to distribute; the cross uses fixed-width buttons and the box honours the configured width.
    private static final int GEOM_PREVIEW_WIDTH_DP = 240;

    // Inflate a geometry box showing only the requested region(s) and style it; this is the live preview
    // shown above that region's controls. forceWidthDp > 0 pins the width (re-applied after each restyle).
    private void addRegionPreview(boolean tiles, boolean cross, boolean zorder, int forceWidthDp) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(10);
        hlp.bottomMargin = dp(2);
        scroll.setLayoutParams(hlp);

        View gb = getLayoutInflater().inflate(R.layout.geometry_box, scroll, false);
        gb.setVisibility(View.VISIBLE);
        // Centre the box when it is narrower than the screen; scroll horizontally when wider.
        if (gb.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) gb.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
        }
        setRegionVisible(gb, R.id.gb_tile_row, tiles);
        setRegionVisible(gb, R.id.gb_vm, cross);
        setRegionVisible(gb, R.id.gb_cross_row, cross);
        setRegionVisible(gb, R.id.gb_vp, cross);
        setRegionVisible(gb, R.id.gb_zorder_row, zorder);
        TextView e1 = gb.findViewById(R.id.gb_e1);
        TextView e2 = gb.findViewById(R.id.gb_e2);
        if (e1 != null) {
            e1.setText(getString(R.string.gb_w) + "\n6");
        }
        if (e2 != null) {
            e2.setText(getString(R.string.gb_h) + "\n3");
        }

        GeomPreview p = new GeomPreview();
        p.view = gb;
        p.forceWidthDp = forceWidthDp;
        styleGeomPreview(p);

        scroll.addView(gb);
        mGeometryPreviews.add(p);
        mBody.addView(scroll);
    }

    private void setRegionVisible(View root, int id, boolean visible) {
        View v = root.findViewById(id);
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void styleGeomPreview(GeomPreview p) {
        GeometryBoxStyler.style(p.view);
        if (p.forceWidthDp > 0) {
            ViewGroup.LayoutParams lp = p.view.getLayoutParams();
            if (lp != null) {
                lp.width = dp(p.forceWidthDp);
                p.view.setLayoutParams(lp);
            }
        }
    }

    private void restyleGeometryPreview() {
        for (GeomPreview p : mGeometryPreviews) {
            styleGeomPreview(p);
        }
    }

    // A dp-size slider (geometry element size / panel width). 0 shows "Auto" when autoAtZero; long-press
    // resets to the default. Writes UiConfig.setSize and live-restyles the preview.
    private void addSizeRow(String title, final String sizeKey, final int defaultDp, final int maxDp,
                            int indent, final boolean autoAtZero) {
        int textColor = UiTheme.color(UiSlot.TEXT);
        LinearLayout row = makeRowContainer(indent);
        TextView label = makeLabel(title, textColor);
        row.addView(label);
        addRowIcon(row, UiIcons.Icon.SIZE, UiIcons.Does.VIEW);
        final SeekBar seek = new SeekBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = dp(8);
        slp.rightMargin = dp(8);
        seek.setLayoutParams(slp);
        seek.setMax(maxDp);
        int cur = UiConfig.get().getSize(sizeKey);
        if (cur < 0) {
            cur = defaultDp;
        }
        seek.setProgress(Math.max(0, Math.min(maxDp, cur)));
        row.addView(seek);
        final TextView value = makeLabel(sizeValueLabel(cur, autoAtZero), textColor);
        value.setMinWidth(dp(64));
        value.setGravity(Gravity.END);
        row.addView(value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UiConfig.get().setSize(sizeKey, progress);
                value.setText(sizeValueLabel(progress, autoAtZero));
                restyleGeometryPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        row.setOnLongClickListener(v -> {
            UiConfig.get().clearSize(sizeKey);
            seek.setProgress(Math.max(0, Math.min(maxDp, defaultDp)));
            return true;
        });
        mBody.addView(row);
    }

    private String sizeValueLabel(int dp, boolean autoAtZero) {
        if (autoAtZero && dp <= 0) {
            return getString(R.string.llui_geom_auto);
        }
        return getString(R.string.theme_border_width_value, dp);
    }

    // --- Export / Import: the first section of the page, exactly as in the sister apps. It holds the
    // Export / Import window, the backup directory, and — directly below those, never as a section of
    // its own — the 保存復元 automation switch + token, because that is a backup feature too. ---

    private void fillExportImportSection() {
        // The one panel of this section: its main pill.
        addPillRow(getString(R.string.rkb_eim_entry), getString(R.string.rkb_eim_entry_desc),
                UiIcons.Icon.EXPORT, UiIcons.Does.SHARE, true, true, false, mStepPx, v -> openExportImport());
        addDirRow();
        addLastExportRow();
        addAutomationRows();
    }

    /** The directory is queried when this page opens, so the newest backup's date is shown right here. */
    private void addLastExportRow() {
        String msg;
        boolean warn;
        if (RkbExport.exportDir(this) == null) {
            msg = getString(R.string.rkb_eim_warn_nodir);
            warn = true;
        } else {
            androidx.documentfile.provider.DocumentFile newest = RkbExport.newestExport(this);
            if (newest == null) {
                msg = getString(R.string.rkb_eim_warn_none);
                warn = true;
            } else {
                msg = getString(R.string.rkb_eim_last,
                        android.text.format.DateFormat.getDateFormat(this).format(newest.lastModified())
                                + " " + android.text.format.DateFormat.getTimeFormat(this).format(newest.lastModified()));
                warn = false;
            }
        }
        TextView line = addNote(msg, mStepPx);
        line.setTextSize(13);
        if (warn) {
            line.setTextColor(warnColor());
        }
    }

    /**
     * The backup directory — red while unset, so a launcher with nowhere to back up to says so
     * loudly. A folder a restore remembered but the platform no longer grants is named too, with the
     * re-grant line under it: the picker below opens straight at it, so it is one tap away from
     * working again.
     */
    private void addDirRow() {
        String label = ExportImportPanel.dirLabel(this);
        String remembered = label == null ? ExportImportPanel.rememberedDirLabel(this) : null;
        CharSequence value = label != null ? label
                : remembered != null ? remembered + "\n" + getString(R.string.rkb_eim_dir_regrant)
                : getString(R.string.rkb_eim_dir_unset);
        addTwoLineRow(getString(R.string.rkb_eim_dir), value,
                label != null ? UiTheme.color(UiSlot.NOTE) : warnColor(),
                mStepPx, UiIcons.Icon.DIRECTORY, v -> mPickExportDir.launch(RkbExport.exportDirUri(this)));
    }

    private void openExportImport() {
        mExportImport = new ExportImportPanel(this, new ExportImportPanel.Host() {
            @Override
            public void pickExportDir(Uri initial) {
                mPickExportDir.launch(initial);
            }

            @Override
            public void pickImportFile() {
                mPickImportFile.launch(new String[]{"application/zip", "application/octet-stream", "*/*"});
            }

            @Override
            public void onChainFinished() {
                // A finished export/import closes the whole chain: info dialog → panel → this page.
                finish();
            }
        });
        mExportImport.show();
    }

    private void onExportDirPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        if (mExportImport != null && mExportImport.isShowing()) {
            mExportImport.onDirPicked(uri);
        } else {
            ExportImportPanel.storeDir(this, uri);
        }
        buildRows();
    }

    private void onImportFilePicked(Uri uri) {
        if (uri != null && mExportImport != null && mExportImport.isShowing()) {
            mExportImport.onImportFilePicked(uri);
        }
    }

    // --- 保存復元 automation (the sister-app state-export contract, v2) ---

    /**
     * Three rows, in the order every sister app shows them, and inside the Export/Import section
     * rather than a section of their own — this is a backup feature, so 白い熊 finds it where backup
     * lives.
     *
     * <p>The master switch ships <b>ON</b> and the token is <b>opt-in</b>: a clean phone being
     * restored by 応用管理 has nobody to turn anything on and nothing pasted anywhere. The token row
     * is therefore drawn only while it is actually being asked for — a 48-character secret sitting
     * under an off switch invites 白い熊 to paste it somewhere it will do nothing.
     */
    private void addAutomationRows() {
        addSwitchRow(getString(R.string.rkb_auto_switch), getString(R.string.rkb_auto_switch_desc),
                AutomationAuth.isEnabled(this), mStepPx, checked -> AutomationAuth.setEnabled(this, checked));
        addSwitchRow(getString(R.string.rkb_auto_require_token),
                getString(R.string.rkb_auto_require_token_desc),
                AutomationAuth.isTokenRequired(this), mStepPx, checked -> {
                    AutomationAuth.setTokenRequired(this, checked);
                    // The token row appears/disappears with this switch. Posted rather than called
                    // straight out of the listener, so the Switch that fired it finishes drawing
                    // itself before the whole holder is rebuilt underneath it.
                    mHolder.post(this::buildRows);
                });
        if (AutomationAuth.isTokenRequired(this)) {
            addTokenRow();
        }
    }

    /**
     * The token (abbreviated, monospace) and its two actions as pills: Copy, and Regenerate — a fresh
     * secret that revokes every pasted copy, so it wears the delete red.
     */
    private void addTokenRow() {
        LinearLayout row = makeRowContainer(mStepPx);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = makeLabel(getString(R.string.rkb_auto_token), UiTheme.color(UiSlot.TEXT));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        labels.addView(title);
        TextView value = makeLabel(AutomationAuth.abbreviate(AutomationAuth.token(this)), UiTheme.color(UiSlot.NOTE));
        value.setTypeface(Typeface.MONOSPACE);
        value.setTextSize(13);
        labels.addView(value);
        row.addView(labels);
        addRowIcon(row, UiIcons.Icon.LOCK, UiIcons.Does.VIEW);
        addLeftRule(row);
        mBody.addView(row);

        addPillRow(getString(R.string.rkb_auto_token_copy), getString(R.string.rkb_auto_token_copy_desc),
                UiIcons.Icon.COPY, UiIcons.Does.SHARE, false, false, false, mStepPx, v -> {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("automation_token", AutomationAuth.token(this)));
                    }
                    Flash.show(this, R.string.rkb_auto_token_copied);
                });
        addPillRow(getString(R.string.rkb_auto_regenerate), getString(R.string.rkb_auto_regenerate_desc),
                UiIcons.Icon.RESET, UiIcons.Does.DELETE, false, false, true, mStepPx,
                v -> confirmRegenerateToken());
    }

    private void confirmRegenerateToken() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rkb_auto_token_regen_title)
                .setMessage(R.string.rkb_auto_token_regen_msg)
                .setPositiveButton(R.string.rkb_auto_regenerate, (d, which) -> {
                    AutomationAuth.regenerateToken(this);
                    buildRows();
                    Flash.show(this, R.string.rkb_auto_token_regenerated);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        // it revokes every copy pasted elsewhere: the red pill
        net.pierrox.lightning_launcher.util.UiDialogStyler.destructive(dialog);
        dialog.show();
        net.pierrox.lightning_launcher.util.UiDialogStyler.style(dialog);
    }

    // --- generic rows shared by the section above ---

    /** A control row: a title over its current value; a tap opens the picker for it. */
    private LinearLayout addTwoLineRow(String title, CharSequence value, int valueColor, int indent,
                                       UiIcons.Icon icon, View.OnClickListener onClick) {
        LinearLayout row = makeRowContainer(indent);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        labels.addView(makeLabel(title, UiTheme.color(UiSlot.TEXT)));
        TextView valueView = makeLabel(value, valueColor);
        valueView.setTextSize(13);
        labels.addView(valueView);
        row.addView(labels);
        addRowIcon(row, icon, UiIcons.Does.VIEW);
        if (onClick != null) {
            row.setOnClickListener(onClick);
        }
        mBody.addView(row);
        return row;
    }

    private interface OnToggle {
        void onToggle(boolean checked);
    }

    /**
     * An explained switch row: bold label, dim-gold description, the thin dim-gold rule down its left
     * edge, and the outlined switch at the right — a round outline with a solid dot, never filled.
     */
    private void addSwitchRow(String title, String description, boolean checked, int indent,
                              final OnToggle onToggle) {
        LinearLayout row = makeRowContainer(indent);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView titleView = makeLabel(title, UiTheme.color(UiSlot.TEXT));
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        labels.addView(titleView);
        TextView desc = makeLabel(description, UiTheme.color(UiSlot.NOTE));
        desc.setTextSize(13);
        desc.setPadding(0, 0, dp(8), 0);
        labels.addView(desc);
        row.addView(labels);

        final CheckBox toggle = new CheckBox(this);
        toggle.setChecked(checked);
        UiPills.applySwitch(toggle, false);
        toggle.setOnCheckedChangeListener((button, isChecked) -> onToggle.onToggle(isChecked));
        row.addView(toggle);

        addRowIcon(row, UiIcons.Icon.TOGGLE, UiIcons.Does.VIEW);
        addLeftRule(row);
        row.setOnClickListener(v -> toggle.toggle());
        mBody.addView(row);
    }

    // --- language (in-app, independent of the system locale) ---

    private static final String[] LANG_TAGS = {"", "en", "ja", "cs", "ru"};

    private void fillLanguageSection() {
        TextView value = addValueRow(getString(R.string.llui_language), mStepPx, UiIcons.Icon.GLOBE, v -> openLanguagePicker());
        value.setText(currentLanguageLabel());
    }

    private CharSequence[] languageLabels() {
        return new CharSequence[]{getString(R.string.llui_lang_system), "English", "日本語", "Čeština", "Русский"};
    }

    private void restartApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(getPackageName());
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launch);
        Runtime.getRuntime().exit(0);
    }

    private String currentLanguageLabel() {
        String tag = UiConfig.get().getLocaleTag();
        if (tag == null || tag.isEmpty()) {
            return getString(R.string.llui_lang_system);
        }
        if (tag.startsWith("ja")) {
            return "日本語";
        }
        if (tag.startsWith("en")) {
            return "English";
        }
        if (tag.startsWith("cs")) {
            return "Čeština";
        }
        if (tag.startsWith("ru")) {
            return "Русский";
        }
        return tag;
    }

    private void openLanguagePicker() {
        String tag = UiConfig.get().getLocaleTag();
        int checked = 0;
        for (int i = 0; i < LANG_TAGS.length; i++) {
            if (LANG_TAGS[i].equals(tag)) {
                checked = i;
                break;
            }
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.llui_language)
                .setSingleChoiceItems(languageLabels(), checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        d.dismiss();
                        UiConfig.get().setLocaleTag(LANG_TAGS[which]);
                        // A full process restart is required: recreate()/swipe-from-Recents leaves stale
                        // resources behind, so parts of the UI keep the old language. Relaunch the home in
                        // a fresh process so the forced locale (attachBaseContext) applies everywhere.
                        restartApp();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        net.pierrox.lightning_launcher.util.UiDialogStyler.style(dialog);
    }

    private TextView makeLabel(CharSequence text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        UiTheme.applyFont(tv, UiSlot.TEXT);
        return tv;
    }

    private View makeSwatch(UiSlot slot) {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
        lp.gravity = Gravity.CENTER_VERTICAL;
        v.setLayoutParams(lp);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(UiTheme.color(slot));
        g.setStroke(dp(1), UiTheme.color(UiSlot.ACCENT));
        v.setBackground(g);
        return v;
    }

    private void refreshSwatch(UiSlot slot, View swatch) {
        Drawable d = swatch.getBackground();
        if (d instanceof GradientDrawable) {
            ((GradientDrawable) d).setColor(UiTheme.color(slot));
        }
    }

    private LinearLayout makeRowContainer(int indent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // every row is only as tall as its content: no minimum height, 2 dp above and below
        row.setPadding(indent + dp(8), dp(2), dp(14), dp(2));
        return row;
    }

    private void addColorRow(final UiSlot slot, int indent) {
        LinearLayout row = makeRowContainer(indent);

        TextView label = makeLabel(getString(slot.labelRes), UiTheme.color(UiSlot.TEXT));
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);

        View swatch = makeSwatch(slot);
        row.addView(swatch);
        mSwatches.put(slot, swatch);
        addRowIcon(row, UiIcons.Icon.PALETTE, UiIcons.Does.VIEW);

        row.setOnClickListener(v -> openColorPicker(slot));
        row.setOnLongClickListener(v -> {
            resetColor(slot);
            return true;
        });
        mBody.addView(row);
    }

    // A border slot = a colour row (the line/stroke colour) plus a width SeekBar (0 = no border).
    private void addBorderSlot(final UiSlot slot, int indent) {
        addColorRow(slot, indent);

        int sub = indent + mStepPx;
        int textColor = UiTheme.color(UiSlot.TEXT);

        LinearLayout widthRow = makeRowContainer(sub);
        TextView widthLabel = makeLabel(getString(R.string.theme_border_width), textColor);
        widthRow.addView(widthLabel);
        addRowIcon(widthRow, UiIcons.Icon.SLIDERS, UiIcons.Does.VIEW);
        final SeekBar seek = new SeekBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = dp(8);
        slp.rightMargin = dp(8);
        seek.setLayoutParams(slp);
        seek.setMax(UiConfig.MAX_BORDER_WIDTH_DP);
        // A push button is a pill by rule (3 dp main / 1.25 dp others); its sliders only override that.
        // Until one is moved they read "pill". A long-press on the row gives the pill back.
        final boolean pillWidth = slot == UiSlot.BUTTON_BORDER && !UiConfig.get().hasBorderWidth(slot.key);
        seek.setProgress(pillWidth ? 1 : Math.max(0, Math.min(UiConfig.MAX_BORDER_WIDTH_DP, UiTheme.borderWidthDp(slot))));
        widthRow.addView(seek);
        final TextView widthValue = makeLabel(pillWidth ? getString(R.string.llui_auto_pill)
                : borderLabel(UiTheme.borderWidthDp(slot)), textColor);
        widthRow.setOnLongClickListener(v -> {
            UiConfig.get().clearBorderWidth(slot.key);
            buildRows();
            return true;
        });
        widthValue.setMinWidth(dp(56));
        widthValue.setGravity(Gravity.END);
        widthRow.addView(widthValue);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UiConfig.get().setBorderWidth(slot.key, progress);
                widthValue.setText(borderLabel(progress));
                restyleGeometryPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mBody.addView(widthRow);
    }

    private String borderLabel(int dp) {
        return dp > 0 ? getString(R.string.theme_border_width_value, dp) : getString(R.string.theme_border_none);
    }

    // Corner-roundness slider for a button slot (0 = square). Shares the border slot's indent.
    private void addCornerRow(final UiSlot slot, int indent) {
        int sub = indent + mStepPx;
        int textColor = UiTheme.color(UiSlot.TEXT);

        LinearLayout row = makeRowContainer(sub);
        TextView label = makeLabel(getString(R.string.theme_corner), textColor);
        row.addView(label);
        addRowIcon(row, UiIcons.Icon.FRAME, UiIcons.Does.VIEW);
        final SeekBar seek = new SeekBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = dp(8);
        slp.rightMargin = dp(8);
        seek.setLayoutParams(slp);
        seek.setMax(UiConfig.MAX_CORNER_RADIUS_DP);
        final boolean pillCorner = slot == UiSlot.BUTTON_BORDER && !UiConfig.get().hasCornerRadius(slot.key);
        seek.setProgress(pillCorner ? UiConfig.MAX_CORNER_RADIUS_DP
                : Math.max(0, Math.min(UiConfig.MAX_CORNER_RADIUS_DP, UiTheme.cornerRadiusDp(slot))));
        row.addView(seek);
        final TextView value = makeLabel(pillCorner ? getString(R.string.llui_auto_pill)
                : getString(R.string.theme_border_width_value, UiTheme.cornerRadiusDp(slot)), textColor);
        row.setOnLongClickListener(v -> {
            UiConfig.get().clearCornerRadius(slot.key);
            buildRows();
            return true;
        });
        value.setMinWidth(dp(56));
        value.setGravity(Gravity.END);
        row.addView(value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UiConfig.get().setCornerRadius(slot.key, progress);
                value.setText(getString(R.string.theme_border_width_value, progress));
                restyleGeometryPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mBody.addView(row);
    }

    private void addTextSlot(final UiSlot slot, int indent) {
        TextSlotViews views = new TextSlotViews();
        int textColor = UiTheme.color(UiSlot.TEXT);

        // colour row (label + swatch)
        LinearLayout colorRow = makeRowContainer(indent);
        TextView label = makeLabel(getString(slot.labelRes), textColor);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        colorRow.addView(label);
        View swatch = makeSwatch(slot);
        colorRow.addView(swatch);
        addRowIcon(colorRow, UiIcons.Icon.PALETTE, UiIcons.Does.VIEW);
        colorRow.setOnClickListener(v -> openColorPicker(slot));
        colorRow.setOnLongClickListener(v -> {
            resetColor(slot);
            return true;
        });
        mBody.addView(colorRow);
        views.swatch = swatch;

        int sub = indent + mStepPx;

        // font row
        views.fontValue = addValueRow(getString(R.string.theme_font), sub, UiIcons.Icon.TEXT, v -> openFontPicker(slot));
        // weight row
        views.weightValue = addValueRow(getString(R.string.theme_weight), sub, UiIcons.Icon.LAYERS, v -> openWeightPicker(slot));

        // size row
        LinearLayout sizeRow = makeRowContainer(sub);
        TextView sizeLabel = makeLabel(getString(R.string.theme_size), textColor);
        sizeRow.addView(sizeLabel);
        addRowIcon(sizeRow, UiIcons.Icon.SIZE, UiIcons.Does.VIEW);
        final SeekBar seek = new SeekBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = dp(8);
        slp.rightMargin = dp(8);
        seek.setLayoutParams(slp);
        seek.setMax(UiConfig.MAX_FONT_SIZE_SP);
        int curSize = Math.max(0, Math.min(UiConfig.MAX_FONT_SIZE_SP, UiTheme.size(slot)));
        seek.setProgress(curSize);
        sizeRow.addView(seek);
        final TextView sizeValue = makeLabel(sizeLabel(UiTheme.size(slot)), textColor);
        sizeValue.setMinWidth(dp(56));
        sizeValue.setGravity(Gravity.END);
        sizeRow.addView(sizeValue);
        views.sizeValue = sizeValue;
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UiConfig.get().setFontSize(slot.key, progress);
                sizeValue.setText(sizeLabel(progress));
                refreshSample(slot);
                restyleGeometryPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mBody.addView(sizeRow);

        // live sample
        TextView sample = new TextView(this);
        sample.setPadding(sub + dp(8) + dp(30), dp(2), dp(14), dp(6));
        sample.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        views.sample = sample;
        mBody.addView(sample);

        mTextSlots.put(slot, views);
        refreshTextSlot(slot);
    }

    private TextView addValueRow(String title, int indent, UiIcons.Icon icon, View.OnClickListener onClick) {
        LinearLayout row = makeRowContainer(indent);
        TextView titleView = makeLabel(title, UiTheme.color(UiSlot.TEXT));
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(titleView);
        addRowIcon(row, icon, UiIcons.Does.VIEW);
        TextView value = makeLabel("", UiTheme.color(UiSlot.TEXT));
        row.addView(value);
        row.setOnClickListener(onClick);
        mBody.addView(row);
        return value;
    }

    private String sizeLabel(int sp) {
        return sp > 0 ? getString(R.string.theme_size_value, sp) : getString(R.string.theme_size_default);
    }

    private void refreshTextSlot(UiSlot slot) {
        TextSlotViews v = mTextSlots.get(slot);
        if (v == null) {
            return;
        }
        if (v.swatch != null) {
            refreshSwatch(slot, v.swatch);
        }
        if (v.fontValue != null) {
            v.fontValue.setText(UiFonts.displayName(this, UiTheme.family(slot)));
        }
        if (v.weightValue != null) {
            v.weightValue.setText(getString(UiFonts.Weight.fromValue(UiTheme.weight(slot)).labelRes));
        }
        if (v.sizeValue != null) {
            v.sizeValue.setText(sizeLabel(UiTheme.size(slot)));
        }
        refreshSample(slot);
    }

    private void refreshSample(UiSlot slot) {
        TextSlotViews v = mTextSlots.get(slot);
        if (v == null || v.sample == null) {
            return;
        }
        int sp = UiTheme.size(slot);
        v.sample.setText(R.string.font_sample_text);
        v.sample.setTypeface(UiFonts.typeface(UiTheme.family(slot), UiTheme.weight(slot), Typeface.NORMAL));
        v.sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp > 0 ? sp : 16);
        v.sample.setTextColor(UiTheme.color(slot));
    }

    // --- pickers ---

    private void openColorPicker(final UiSlot slot) {
        ColorPickerDialog dialog = new ColorPickerDialog(this, UiTheme.color(slot));
        dialog.setAlphaSliderVisible(true);
        dialog.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                applyColor(slot, color);
            }

            @Override
            public void onColorDialogSelected(int color) {
                applyColor(slot, color);
            }

            @Override
            public void onColorDialogCanceled() {
            }
        });
        dialog.show();
    }

    private void applyColor(UiSlot slot, int color) {
        UiConfig.get().setOverride(slot.key, color);
        afterColorChange(slot);
    }

    private void resetColor(UiSlot slot) {
        UiConfig.get().clearOverride(slot.key);
        afterColorChange(slot);
    }

    private void afterColorChange(UiSlot slot) {
        if (slot.isFoundation) {
            // a foundation colour cascades into the chrome + every inheriting slot/preview
            recreate();
            return;
        }
        if (slot.parentKey == null) {
            // a role of its own (dim gold, switch off, delete red, an icon colour): the page itself
            // wears it, so redraw the rows
            buildRows();
            return;
        }
        View swatch = mSwatches.get(slot);
        if (swatch != null) {
            refreshSwatch(slot, swatch);
        }
        if (mTextSlots.containsKey(slot)) {
            refreshTextSlot(slot);
        }
        restyleGeometryPreview();
    }

    private void openFontPicker(final UiSlot slot) {
        new UiFontPickerDialog(this, new UiFontPickerDialog.Callback() {
            @Override
            public void onPick(String fileName) {
                UiConfig.get().setFontFamily(slot.key, fileName);
                afterFontChange(slot);
            }

            @Override
            public void onAddFont() {
                mPendingFontSlot = slot;
                mFontImport.launch(new String[]{"*/*"});
            }
        });
    }

    private void openWeightPicker(final UiSlot slot) {
        final UiFonts.Weight[] weights = UiFonts.Weight.values();
        CharSequence[] labels = new CharSequence[weights.length];
        int checked = 0;
        int current = UiTheme.weight(slot);
        for (int i = 0; i < weights.length; i++) {
            labels[i] = getString(weights[i].labelRes);
            if (weights[i].value == current) {
                checked = i;
            }
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.theme_weight)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        UiConfig.get().setFontWeight(slot.key, weights[which].value);
                        afterFontChange(slot);
                        d.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        net.pierrox.lightning_launcher.util.UiDialogStyler.style(dialog);
    }

    private void afterFontChange(UiSlot slot) {
        // the foundation TEXT font is the global default → repaint the whole screen when it changes
        if (slot == UiSlot.TEXT) {
            recreate();
        } else {
            refreshTextSlot(slot);
            restyleGeometryPreview();
        }
    }

    private void onFontImported(Uri uri) {
        UiSlot slot = mPendingFontSlot;
        mPendingFontSlot = null;
        if (uri == null || slot == null) {
            return;
        }
        String fileName = UiFonts.importFont(this, uri);
        if (fileName == null) {
            Flash.show(this, R.string.font_invalid);
            return;
        }
        UiConfig.get().setFontFamily(slot.key, fileName);
        afterFontChange(slot);
    }
}
