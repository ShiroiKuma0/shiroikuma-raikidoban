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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
import net.pierrox.lightning_launcher.util.Flash;
import net.pierrox.lightning_launcher.util.UiFontPickerDialog;
import net.pierrox.lightning_launcher_extreme.R;

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

    private final Map<UiSlot, View> mSwatches = new HashMap<>();
    private final Map<UiSlot, TextSlotViews> mTextSlots = new HashMap<>();

    private UiSlot mPendingFontSlot;
    private final ActivityResultLauncher<String[]> mFontImport =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFontImported);

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
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mHolder = new LinearLayout(this);
        mHolder.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        mHolder.setPadding(pad, pad, pad, dp(32));
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

    private void buildRows() {
        mHolder.removeAllViews();
        mSwatches.clear();
        mTextSlots.clear();

        addLanguageSection();

        for (UiGroup group : UiGroup.values()) {
            addSection(group);
            List<UiSlot> slots = UiSlot.forGroup(group);
            for (UiSlot slot : slots) {
                if (slot == UiSlot.DIALOG_BORDER || slot == UiSlot.BUTTON_BORDER) {
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

        TextView hint = makeLabel(getString(R.string.llui_reset_hint), UiTheme.color(UiSlot.TEXT));
        hint.setTextSize(12);
        hint.setPadding(dp(8), dp(16), dp(8), dp(8));
        mHolder.addView(hint);
    }

    private void addSection(UiGroup group) {
        addSection(getString(group.labelRes));
    }

    private void addSection(String labelText) {
        int accent = UiTheme.color(UiSlot.ACCENT);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(18), dp(8), dp(4));

        TextView label = makeLabel(labelText, accent);
        label.setTextSize(20);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        box.addView(label);

        View rule = new View(this);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        rlp.topMargin = dp(4);
        rule.setLayoutParams(rlp);
        rule.setBackgroundColor(accent);
        box.addView(rule);

        mHolder.addView(box);
    }

    // --- language (in-app, independent of the system locale) ---

    private static final String[] LANG_TAGS = {"", "en", "ja", "cs", "ru"};

    private void addLanguageSection() {
        addSection(getString(R.string.llui_language));
        TextView value = addValueRow(getString(R.string.llui_language), mStepPx, v -> openLanguagePicker());
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
        int pad = dp(10);
        row.setPadding(indent + dp(8), pad, dp(14), pad);
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

        row.setOnClickListener(v -> openColorPicker(slot));
        row.setOnLongClickListener(v -> {
            resetColor(slot);
            return true;
        });
        mHolder.addView(row);
    }

    // A border slot = a colour row (the line/stroke colour) plus a width SeekBar (0 = no border).
    private void addBorderSlot(final UiSlot slot, int indent) {
        addColorRow(slot, indent);

        int sub = indent + mStepPx;
        int textColor = UiTheme.color(UiSlot.TEXT);

        LinearLayout widthRow = makeRowContainer(sub);
        TextView widthLabel = makeLabel(getString(R.string.theme_border_width), textColor);
        widthRow.addView(widthLabel);
        final SeekBar seek = new SeekBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = dp(8);
        slp.rightMargin = dp(8);
        seek.setLayoutParams(slp);
        seek.setMax(UiConfig.MAX_BORDER_WIDTH_DP);
        seek.setProgress(Math.max(0, Math.min(UiConfig.MAX_BORDER_WIDTH_DP, UiTheme.borderWidthDp(slot))));
        widthRow.addView(seek);
        final TextView widthValue = makeLabel(borderLabel(UiTheme.borderWidthDp(slot)), textColor);
        widthValue.setMinWidth(dp(56));
        widthValue.setGravity(Gravity.END);
        widthRow.addView(widthValue);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UiConfig.get().setBorderWidth(slot.key, progress);
                widthValue.setText(borderLabel(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mHolder.addView(widthRow);
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
        final SeekBar seek = new SeekBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = dp(8);
        slp.rightMargin = dp(8);
        seek.setLayoutParams(slp);
        seek.setMax(UiConfig.MAX_CORNER_RADIUS_DP);
        seek.setProgress(Math.max(0, Math.min(UiConfig.MAX_CORNER_RADIUS_DP, UiTheme.cornerRadiusDp(slot))));
        row.addView(seek);
        final TextView value = makeLabel(getString(R.string.theme_border_width_value, UiTheme.cornerRadiusDp(slot)), textColor);
        value.setMinWidth(dp(56));
        value.setGravity(Gravity.END);
        row.addView(value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UiConfig.get().setCornerRadius(slot.key, progress);
                value.setText(getString(R.string.theme_border_width_value, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mHolder.addView(row);
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
        colorRow.setOnClickListener(v -> openColorPicker(slot));
        colorRow.setOnLongClickListener(v -> {
            resetColor(slot);
            return true;
        });
        mHolder.addView(colorRow);
        views.swatch = swatch;

        int sub = indent + mStepPx;

        // font row
        views.fontValue = addValueRow(getString(R.string.theme_font), sub, v -> openFontPicker(slot));
        // weight row
        views.weightValue = addValueRow(getString(R.string.theme_weight), sub, v -> openWeightPicker(slot));

        // size row
        LinearLayout sizeRow = makeRowContainer(sub);
        TextView sizeLabel = makeLabel(getString(R.string.theme_size), textColor);
        sizeRow.addView(sizeLabel);
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
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mHolder.addView(sizeRow);

        // live sample
        TextView sample = new TextView(this);
        sample.setPadding(sub + dp(8), dp(4), dp(14), dp(10));
        sample.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        views.sample = sample;
        mHolder.addView(sample);

        mTextSlots.put(slot, views);
        refreshTextSlot(slot);
    }

    private TextView addValueRow(String title, int indent, View.OnClickListener onClick) {
        LinearLayout row = makeRowContainer(indent);
        TextView titleView = makeLabel(title, UiTheme.color(UiSlot.TEXT));
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(titleView);
        TextView value = makeLabel("", UiTheme.color(UiSlot.TEXT));
        row.addView(value);
        row.setOnClickListener(onClick);
        mHolder.addView(row);
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
        View swatch = mSwatches.get(slot);
        if (swatch != null) {
            refreshSwatch(slot, swatch);
        }
        if (mTextSlots.containsKey(slot)) {
            refreshTextSlot(slot);
        }
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
