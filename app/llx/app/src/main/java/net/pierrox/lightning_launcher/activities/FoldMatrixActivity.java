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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.FoldGrid;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.UiFonts;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fold-matrix editor: an aligned 2D grid of live desktop miniatures — one row per fold state, with
 * a shared column range so every row's offset-0 "home" lines up vertically (rows render empty slots where
 * they have no desktop at a given offset). Long-press a desktop to drag it (drop on the left/right half of
 * a target to place before/after; drop on an empty slot to take that offset); tap for set-home / remove.
 * Black-yellow theme.
 */
public class FoldMatrixActivity extends ResourceWrapperActivity
        implements View.OnLongClickListener, View.OnClickListener, View.OnDragListener {

    private static final float PREVIEW_RATIO = 0.3f;
    private static final int THUMB_W_DP = 144;
    private static final int CELL_EXTRA_DP = 16;
    // Colours follow the DIALOG slots of 「白い熊 雷起動盤 UI」 (initialised in onCreate).
    private int YELLOW;
    private int DIM_YELLOW;
    private int FAINT_YELLOW;
    private int BLACK;

    private LightningEngine mEngine;
    private GlobalConfig mGlobalConfig;
    private net.pierrox.lightning_launcher.engine.Screen mScreen;
    private FoldGrid mGrid;
    private LinearLayout mRoot;
    private int mDisplayW, mDisplayH;
    private final Map<Integer, Drawable> mPreviewCache = new HashMap<>();

    private int mDragRow;
    private int mDragPageId;

    private static final class Holder {
        final int rowKey, offset, pageId;
        Holder(int rowKey, int offset, int pageId) { this.rowKey = rowKey; this.offset = offset; this.pageId = pageId; }
    }

    private static final class Slot { // an empty column slot (drop target only)
        final int rowKey, offset;
        Slot(int rowKey, int offset) { this.rowKey = rowKey; this.offset = offset; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEngine = LLApp.get().getAppEngine();
        mGlobalConfig = mEngine.getGlobalConfig();
        mGrid = FoldGrid.parse(mGlobalConfig.foldGrid);

        YELLOW = UiTheme.color(UiSlot.DIALOG_TEXT);
        BLACK = UiTheme.color(UiSlot.DIALOG_BG);
        DIM_YELLOW = UiTheme.adjustAlpha(YELLOW, 0.53f);
        FAINT_YELLOW = UiTheme.adjustAlpha(YELLOW, 0.27f);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        mDisplayW = dm.widthPixels;
        mDisplayH = dm.heightPixels;

        getWindow().setStatusBarColor(UiTheme.color(UiSlot.STATUSBAR_BG));
        getWindow().setNavigationBarColor(BLACK);

        // Off-screen Screen used only to render desktop previews (mirrors ScreenManager).
        mScreen = new net.pierrox.lightning_launcher.engine.Screen(this, 0) {
            @Override
            public ScreenIdentity getIdentity() {
                return ScreenIdentity.DESKTOP_PREVIEW;
            }

            @Override
            protected Resources getRealResources() {
                return FoldMatrixActivity.this.getRealResources();
            }

            @Override
            public void launchItem(ItemView itemView) {
                // pass
            }
        };
        mScreen.setWindow(getWindow());

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BLACK);
        mRoot = new LinearLayout(this);
        mRoot.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        mRoot.setPadding(pad, pad, pad, pad);
        scroll.addView(mRoot);
        setContentView(scroll);

        buildUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScreen != null) {
            mScreen.destroy();
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(CharSequence s, float sizeSp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(UiFonts.typeface(UiTheme.family(UiSlot.DIALOG_TEXT),
                UiTheme.weight(UiSlot.DIALOG_TEXT), bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private void buildUI() {
        mRoot.removeAllViews();

        mRoot.addView(text(getString(R.string.fm_title), 20, true, YELLOW));
        TextView hint = text(getString(R.string.fm_drag_hint), 12, false, DIM_YELLOW);
        hint.setPadding(0, dp(2), 0, dp(8));
        mRoot.addView(hint);

        // Shared column range across all rows so offsets (and the home column) align vertically.
        boolean any = false;
        int minOff = 0, maxOff = 0;
        for (FoldGrid.Row r : mGrid.rows) {
            for (Integer off : mGrid.offsetsSorted(r.rowKey)) {
                if (!any) {
                    minOff = maxOff = off;
                    any = true;
                } else {
                    minOff = Math.min(minOff, off);
                    maxOff = Math.max(maxOff, off);
                }
            }
        }

        // All rows live in ONE horizontal scroller so columns stay aligned even when scrolled.
        HorizontalScrollView hgrid = new HorizontalScrollView(this);
        hgrid.setHorizontalScrollBarEnabled(false);
        LinearLayout gridInner = new LinearLayout(this);
        gridInner.setOrientation(LinearLayout.VERTICAL);

        for (FoldGrid.Row r : mGrid.rows) {
            String w = r.widthPx == null ? getString(R.string.fm_width_unset) : (r.widthPx + " px");
            TextView header = text(getString(R.string.fm_row, r.rowKey) + " · " + w, 14, true, YELLOW);
            header.setGravity(Gravity.START);
            header.setPadding(0, dp(12), 0, dp(4));
            gridInner.addView(header);

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setTag(Integer.valueOf(r.rowKey));
            rowLayout.setOnDragListener(this);
            if (any) {
                for (int off = minOff; off <= maxOff; off++) {
                    Integer pageId = mGrid.cell(r.rowKey, off);
                    rowLayout.addView(pageId != null ? makeThumb(r.rowKey, off, pageId) : makeSlot(r.rowKey, off));
                }
            }
            gridInner.addView(rowLayout);
        }
        hgrid.addView(gridInner);
        mRoot.addView(hgrid);

        Button rebuild = new Button(this);
        rebuild.setText(R.string.fm_rebuild);
        rebuild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGrid.deriveFromNames(mGlobalConfig.screensNames, mGlobalConfig.screensOrder);
                save();
                buildUI();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(16);
        rebuild.setLayoutParams(blp);
        mRoot.addView(rebuild);
    }

    private int cellW() {
        return dp(THUMB_W_DP + CELL_EXTRA_DP);
    }

    private int previewH() {
        int tw = dp(THUMB_W_DP);
        return mDisplayW > 0 ? Math.round(tw * (float) mDisplayH / mDisplayW) : tw;
    }

    private View makeThumb(int rowKey, int offset, int pageId) {
        boolean isHome = offset == 0;

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(cellW(), ViewGroup.LayoutParams.WRAP_CONTENT));
        int pad = dp(4);
        cell.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLACK);
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(isHome ? 3 : 1), isHome ? YELLOW : DIM_YELLOW);
        cell.setBackground(bg);

        String name = desktopName(pageId);
        cell.addView(text(isHome ? (name + " ★") : name, 12, isHome, YELLOW));

        ImageView preview = new ImageView(this);
        preview.setLayoutParams(new LinearLayout.LayoutParams(dp(THUMB_W_DP), previewH()));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setImageDrawable(previewFor(pageId));
        cell.addView(preview);

        cell.setTag(new Holder(rowKey, offset, pageId));
        cell.setOnLongClickListener(this);
        cell.setOnClickListener(this);
        cell.setOnDragListener(this);
        return cell;
    }

    private View makeSlot(int rowKey, int offset) {
        LinearLayout cell = new LinearLayout(this);
        cell.setLayoutParams(new LinearLayout.LayoutParams(cellW(), previewH() + dp(28)));
        int m = dp(4);
        cell.setPadding(m, m, m, m);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLACK);
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), FAINT_YELLOW);
        cell.setBackground(bg);
        cell.setTag(new Slot(rowKey, offset));
        cell.setOnDragListener(this);
        return cell;
    }

    private Drawable previewFor(int pageId) {
        Drawable d = mPreviewCache.get(pageId);
        if (d == null) {
            d = new PageDrawable(pageId);
            mPreviewCache.put(pageId, d);
        }
        return d;
    }

    private String desktopName(int pageId) {
        int[] order = mGlobalConfig.screensOrder;
        String[] names = mGlobalConfig.screensNames;
        if (order != null && names != null) {
            for (int i = 0; i < order.length && i < names.length; i++) {
                if (order[i] == pageId) {
                    return names[i];
                }
            }
        }
        return "#" + pageId;
    }

    @Override
    public boolean onLongClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof Holder) {
            Holder h = (Holder) tag;
            mDragRow = h.rowKey;
            mDragPageId = h.pageId;
            ClipData data = ClipData.newPlainText("page", String.valueOf(h.pageId));
            v.startDragAndDrop(data, new View.DragShadowBuilder(v), null, 0);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (!(tag instanceof Holder)) {
            return;
        }
        final Holder h = (Holder) tag;
        new AlertDialog.Builder(this)
                .setTitle(desktopName(h.pageId))
                .setItems(new CharSequence[]{getString(R.string.fm_set_home), getString(R.string.fm_remove)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) {
                                    setHome(h.rowKey, h.pageId);
                                } else {
                                    removeCell(h.rowKey, h.pageId);
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            Object tag = v.getTag();
            if (tag instanceof Holder) {
                // Drop onto another desktop: the dragged one takes that desktop's slot (others shift).
                Holder h = (Holder) tag;
                int iB = orderedPages(h.rowKey).indexOf(Integer.valueOf(h.pageId));
                if (iB >= 0) {
                    performMove(mDragRow, mDragPageId, h.rowKey, iB, false);
                }
            } else if (tag instanceof Slot) {
                // Drop onto an empty slot: take that offset.
                Slot s = (Slot) tag;
                int insert = 0;
                for (Integer off : mGrid.offsetsSorted(s.rowKey)) {
                    if (off < s.offset) {
                        insert++;
                    }
                }
                performMove(mDragRow, mDragPageId, s.rowKey, insert, true);
            } else if (tag instanceof Integer) {
                performMove(mDragRow, mDragPageId, (Integer) tag, Integer.MAX_VALUE, true);
            }
        }
        return true;
    }

    private List<Integer> orderedPages(int rowKey) {
        List<Integer> out = new ArrayList<>();
        for (Integer off : mGrid.offsetsSorted(rowKey)) {
            Integer p = mGrid.cell(rowKey, off);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private void performMove(int srcRow, int pageId, int dstRow, int dstIndex, boolean adjustForRemoval) {
        List<Integer> src = orderedPages(srcRow);
        Integer srcHome = mGrid.cell(srcRow, 0);
        int removeIdx = src.indexOf(Integer.valueOf(pageId));
        if (removeIdx < 0) {
            return;
        }
        src.remove(removeIdx);

        boolean sameRow = dstRow == srcRow;
        List<Integer> dst = sameRow ? src : orderedPages(dstRow);
        Integer dstHome = mGrid.cell(dstRow, 0);

        int insert = dstIndex;
        if (adjustForRemoval && sameRow && removeIdx < insert) {
            insert--;
        }
        if (insert > dst.size()) {
            insert = dst.size();
        }
        if (insert < 0) {
            insert = 0;
        }
        dst.add(insert, Integer.valueOf(pageId));

        Integer newSrcHome;
        Integer newDstHome;
        if (sameRow) {
            newSrcHome = srcHome;
            newDstHome = srcHome;
        } else {
            newDstHome = (dstHome == null) ? Integer.valueOf(pageId) : dstHome;
            if (srcHome != null && srcHome == pageId) {
                newSrcHome = src.isEmpty() ? null : src.get(Math.min(removeIdx, src.size() - 1));
            } else {
                newSrcHome = srcHome;
            }
        }

        commitRow(srcRow, src, newSrcHome);
        if (!sameRow) {
            commitRow(dstRow, dst, newDstHome);
        }
        save();
        buildUI();
    }

    private void setHome(int rowKey, int pageId) {
        commitRow(rowKey, orderedPages(rowKey), Integer.valueOf(pageId));
        save();
        buildUI();
    }

    private void removeCell(int rowKey, int pageId) {
        List<Integer> list = orderedPages(rowKey);
        Integer home = mGrid.cell(rowKey, 0);
        int idx = list.indexOf(Integer.valueOf(pageId));
        if (idx < 0) {
            return;
        }
        list.remove(idx);
        Integer newHome;
        if (home != null && home == pageId) {
            newHome = list.isEmpty() ? null : list.get(Math.min(idx, list.size() - 1));
        } else {
            newHome = home;
        }
        commitRow(rowKey, list, newHome);
        save();
        buildUI();
    }

    // Write an ordered list of pageIds back into the grid row as offset->pageId, with the home desktop
    // at offset 0 (others by position: left = negative, right = positive).
    private void commitRow(int rowKey, List<Integer> list, Integer homePageId) {
        FoldGrid.Row r = mGrid.row(rowKey);
        if (r == null) {
            return;
        }
        r.cells.clear();
        if (list.isEmpty()) {
            return;
        }
        int homeIdx = homePageId == null ? 0 : list.indexOf(homePageId);
        if (homeIdx < 0) {
            homeIdx = 0;
        }
        for (int i = 0; i < list.size(); i++) {
            r.cells.put(i - homeIdx, list.get(i));
        }
    }

    private void save() {
        mGlobalConfig.foldGrid = mGrid.toJson();
        mEngine.saveData();
    }

    private class PageDrawable extends Drawable {
        private final ItemLayout il;

        PageDrawable(int page) {
            Page p = mEngine.getOrLoadPage(page);
            il = new ItemLayout(FoldMatrixActivity.this, null);
            il.setAllowDelayedViewInit(false);
            il.setScreen(mScreen);
            mScreen.takeItemLayoutOwnership(il);
            il.setPage(p);
            il.measure(View.MeasureSpec.makeMeasureSpec(mDisplayW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mDisplayH, View.MeasureSpec.EXACTLY));
            il.layout(0, 0, mDisplayW, mDisplayH);
        }

        @Override
        public int getIntrinsicWidth() {
            return (int) (mDisplayW * PREVIEW_RATIO);
        }

        @Override
        public int getIntrinsicHeight() {
            return (int) (mDisplayH * PREVIEW_RATIO);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.scale(PREVIEW_RATIO, PREVIEW_RATIO);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            il.draw(canvas);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            // pass
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // pass
        }
    }
}
