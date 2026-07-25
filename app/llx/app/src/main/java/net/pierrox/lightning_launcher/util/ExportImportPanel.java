package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.data.RkbExport;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Export / Import window — one black-yellow page that backs up and restores everything settable in
 * 白い熊 雷起動盤, in the family's shared visual format (the Kōjiki export sheet / the kxkb Export-import
 * page): one bordered rounded box carrying a centred title, a dim description, a bordered tappable
 * directory box (red when unset), the last-backup line, a divider, 全選択 + the category checkboxes
 * (sub-options indented under their parent, following its toggle), a divider, and the ArcaneChat button
 * bar — round pills, Cancel alone on the left, Import + Export grouped on the right.
 *
 * <p>All work goes through {@link RkbExport}, the same core the headless automation receiver uses.
 * A successful export or import ends with a bordered info dialog whose acknowledgement closes the whole
 * chain (info dialog → this panel → the UI settings page, via {@link Host#onChainFinished()}); failures
 * leave the panel open so 白い熊 can fix the cause and retry.
 *
 * <p>The SAF pickers must be registered by the hosting activity, which forwards their results back
 * through {@link #onDirPicked} / {@link #onImportFilePicked}.
 */
public class ExportImportPanel {

    /** What the hosting activity must provide: the two SAF pickers and the "close everything" hook. */
    public interface Host {
        void pickExportDir(Uri initial);

        void pickImportFile();

        void onChainFinished();
    }

    private static final int WARN_COLOR = 0xFFFF5252;

    private final Activity mActivity;
    private final Host mHost;
    private final float mDensity;

    private final Set<RkbExport.Cat> mSelected = new LinkedHashSet<>(RkbExport.Cat.all());

    private AlertDialog mDialog;
    private LinearLayout mBox;

    public ExportImportPanel(Activity activity, Host host) {
        mActivity = activity;
        mHost = host;
        mDensity = activity.getResources().getDisplayMetrics().density;
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    public void show() {
        mBox = new LinearLayout(mActivity);
        mBox.setOrientation(LinearLayout.VERTICAL);
        mBox.setPadding(dp(20), dp(16), dp(20), dp(20));
        mBox.setBackground(panelBackground());

        ScrollView scroll = new ScrollView(mActivity);
        int m = dp(10);
        scroll.setPadding(m, m, m, m);
        scroll.setClipToPadding(false);
        scroll.addView(mBox, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mDialog = new AlertDialog.Builder(mActivity).setView(scroll).create();
        mDialog.show();
        Window window = mDialog.getWindow();
        if (window != null) {
            // The box owns the whole surface (fill + border), so the dialog window itself is transparent.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        rebuild();
    }

    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    // ---- content -------------------------------------------------------------------------------

    public void rebuild() {
        if (mBox == null) {
            return;
        }
        mBox.removeAllViews();

        TextView title = text(mActivity.getString(R.string.rkb_eim_title), 18, UiTheme.color(UiSlot.DIALOG_TITLE), true);
        UiTheme.applyFont(title, UiSlot.DIALOG_TITLE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(2), 0, dp(6));
        mBox.addView(title);

        TextView desc = text(mActivity.getString(R.string.rkb_eim_desc), 13, UiTheme.color(UiSlot.DIALOG_TEXT), false);
        desc.setAlpha(0.85f);
        desc.setPadding(0, 0, 0, dp(10));
        mBox.addView(desc);

        mBox.addView(dirBox());
        mBox.addView(statusLine());

        mBox.addView(divider(0));

        final CheckBox selectAll = checkbox(mActivity.getString(R.string.rkb_eim_select_all), true, 0);
        selectAll.setChecked(mSelected.size() == RkbExport.Cat.values().length);
        selectAll.setOnClickListener(v -> {
            if (selectAll.isChecked()) {
                mSelected.addAll(RkbExport.Cat.all());
            } else {
                mSelected.clear();
            }
            rebuild();
        });
        mBox.addView(selectAll);

        for (RkbExport.Cat cat : RkbExport.Cat.values()) {
            mBox.addView(categoryRow(cat));
        }

        mBox.addView(divider(8));
        mBox.addView(buttonRow());
    }

    /** The folder box: a bordered, clearly-tappable box — small label over the value, red when unset. */
    private View dirBox() {
        LinearLayout box = new LinearLayout(mActivity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClickable(true);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiTheme.color(UiSlot.DIALOG_BG));
        bg.setStroke(Math.max(1, dp(2)), UiTheme.color(UiSlot.DIALOG_BORDER));
        bg.setCornerRadius(dp(10));
        box.setBackground(bg);
        box.setOnClickListener(v -> mHost.pickExportDir(RkbExport.exportDirUri(mActivity)));

        box.addView(text(mActivity.getString(R.string.rkb_eim_dir), 12, UiTheme.color(UiSlot.DIALOG_TITLE), false));
        String label = dirLabel();
        TextView value = text(label != null ? label : mActivity.getString(R.string.rkb_eim_dir_unset), 15,
                label != null ? UiTheme.color(UiSlot.DIALOG_TEXT) : WARN_COLOR, true);
        box.addView(value);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        box.setLayoutParams(lp);
        return box;
    }

    /** The configured directory as an absolute path when resolvable, its name otherwise, or null. */
    private String dirLabel() {
        return dirLabel(mActivity);
    }

    /** Shared with the UI settings page, so both show the directory exactly the same way. */
    public static String dirLabel(Context context) {
        DocumentFile dir = RkbExport.exportDir(context);
        if (dir == null) {
            return null;
        }
        String abs = RkbExport.absolutePathOf(dir, null);
        if (abs != null) {
            return abs;
        }
        return dir.getName();
    }

    private View statusLine() {
        String msg;
        boolean warn;
        if (RkbExport.exportDir(mActivity) == null) {
            msg = mActivity.getString(R.string.rkb_eim_warn_nodir);
            warn = true;
        } else {
            DocumentFile newest = RkbExport.newestExport(mActivity);
            if (newest == null) {
                msg = mActivity.getString(R.string.rkb_eim_warn_none);
                warn = true;
            } else {
                msg = mActivity.getString(R.string.rkb_eim_last, formatTs(newest.lastModified()));
                warn = false;
            }
        }
        TextView tv = text(msg, 14, warn ? WARN_COLOR : UiTheme.color(UiSlot.DIALOG_TEXT), false);
        tv.setAlpha(warn ? 1f : 0.8f);
        tv.setPadding(dp(2), 0, 0, dp(8));
        return tv;
    }

    private String formatTs(long ts) {
        return DateFormat.getDateFormat(mActivity).format(ts) + " " + DateFormat.getTimeFormat(mActivity).format(ts);
    }

    private View categoryRow(final RkbExport.Cat cat) {
        // Sub-options sit one step in, under their parent, and follow the parent's toggle.
        boolean isChild = cat.parentId != null;
        CheckBox cb = checkbox(mActivity.getString(cat.labelRes), false, isChild ? dp(28) : 0);
        boolean parentOn = !isChild || mSelected.contains(RkbExport.Cat.byId(cat.parentId));
        cb.setChecked(mSelected.contains(cat) && parentOn);
        cb.setEnabled(parentOn);
        cb.setAlpha(parentOn ? 1f : 0.5f);
        cb.setOnClickListener(v -> {
            boolean checked = cb.isChecked();
            if (checked) {
                mSelected.add(cat);
            } else {
                mSelected.remove(cat);
            }
            // A parent drags its children along, so turning a group off never leaves orphaned parts on.
            boolean hasChildren = false;
            for (RkbExport.Cat other : RkbExport.Cat.values()) {
                if (cat.id.equals(other.parentId)) {
                    hasChildren = true;
                    if (checked) {
                        mSelected.add(other);
                    } else {
                        mSelected.remove(other);
                    }
                }
            }
            if (hasChildren) {
                rebuild();
            }
        });
        return cb;
    }

    /** The ArcaneChat button bar: Cancel alone on the left, Import + Export grouped on the right. */
    private View buttonRow() {
        LinearLayout row = new LinearLayout(mActivity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, 0);

        row.addView(pill(mActivity.getString(android.R.string.cancel), v -> dismiss()));
        View spacer = new View(mActivity);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
        Button importButton = pill(mActivity.getString(R.string.rkb_eim_import), v -> onImportClicked());
        ((LinearLayout.LayoutParams) importButton.getLayoutParams()).rightMargin = dp(8);
        row.addView(importButton);
        row.addView(pill(mActivity.getString(R.string.rkb_eim_export), v -> onExportClicked()));
        return row;
    }

    // ---- export --------------------------------------------------------------------------------

    private void onExportClicked() {
        if (mSelected.isEmpty()) {
            showInfo(mActivity.getString(R.string.rkb_eim_export_fail_title),
                    mActivity.getString(R.string.rkb_eim_none_selected), false);
            return;
        }
        DocumentFile dir = RkbExport.exportDir(mActivity);
        if (dir == null) {
            mHost.pickExportDir(null); // no folder yet: ask for one instead of failing
            return;
        }
        Flash.show(mActivity, R.string.rkb_eim_exporting);
        final Set<RkbExport.Cat> cats = new LinkedHashSet<>(mSelected);
        final String name = RkbExport.exportFileName();
        new Thread(() -> {
            String path;
            long bytes;
            try {
                DocumentFile doc = dir.createFile("application/zip", name);
                if (doc == null) {
                    throw new Exception("cannot create " + name);
                }
                OutputStream os = mActivity.getContentResolver().openOutputStream(doc.getUri());
                if (os == null) {
                    throw new Exception("cannot open " + name);
                }
                try {
                    RkbExport.export(mActivity, cats, os, null);
                } finally {
                    os.close();
                }
                bytes = doc.length();
                String abs = RkbExport.absolutePathOf(dir, doc.getName() == null ? name : doc.getName());
                path = abs != null ? abs : name;
            } catch (Throwable t) {
                final String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                mActivity.runOnUiThread(() -> showInfo(
                        mActivity.getString(R.string.rkb_eim_export_fail_title),
                        mActivity.getString(R.string.rkb_eim_export_fail, message), false));
                return;
            }
            final String shownPath = path;
            final long size = bytes;
            mActivity.runOnUiThread(() -> showInfo(
                    mActivity.getString(R.string.rkb_eim_export_done_title),
                    mActivity.getString(R.string.rkb_eim_export_done_body,
                            shownPath, RkbExport.humanSize(size), cats.size()),
                    true));
        }, "rkb-export").start();
    }

    // ---- import --------------------------------------------------------------------------------

    private void onImportClicked() {
        if (mSelected.isEmpty()) {
            showInfo(mActivity.getString(R.string.rkb_eim_import_fail_title),
                    mActivity.getString(R.string.rkb_eim_none_selected), false);
            return;
        }
        final List<DocumentFile> backups = RkbExport.listExports(mActivity);
        final List<CharSequence> labels = new ArrayList<>();
        for (DocumentFile f : backups) {
            labels.add(f.getName());
        }
        labels.add(mActivity.getString(R.string.rkb_eim_browse));
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.rkb_eim_pick_backup)
                .setItems(labels.toArray(new CharSequence[0]), (d, which) -> {
                    if (which >= backups.size()) {
                        mHost.pickImportFile();
                    } else {
                        runImport(backups.get(which).getUri());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        UiDialogStyler.style(dialog);
    }

    /** Called by the host after the SAF file picker returns. */
    public void onImportFilePicked(Uri uri) {
        if (uri != null) {
            runImport(uri);
        }
    }

    private void runImport(final Uri uri) {
        Flash.show(mActivity, R.string.rkb_eim_importing);
        final Set<RkbExport.Cat> cats = new LinkedHashSet<>(mSelected);
        new Thread(() -> {
            String summary;
            try {
                InputStream is = mActivity.getContentResolver().openInputStream(uri);
                if (is == null) {
                    throw new Exception("no input stream");
                }
                byte[] data;
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = is.read(buffer)) > 0) {
                        bos.write(buffer, 0, n);
                    }
                    data = bos.toByteArray();
                } finally {
                    is.close();
                }
                summary = RkbExport.importZip(mActivity, data, cats);
            } catch (Throwable t) {
                final String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                mActivity.runOnUiThread(() -> showInfo(
                        mActivity.getString(R.string.rkb_eim_import_fail_title),
                        mActivity.getString(R.string.rkb_eim_import_fail, message), false));
                return;
            }
            final String body = summary;
            mActivity.runOnUiThread(() -> showImportResult(body));
        }, "rkb-import").start();
    }

    /**
     * The import result: a persistent bordered dialog (study it) with an explicit restart button —
     * never a toast that flies by. Both buttons close the whole chain; "Restart now" additionally
     * relaunches the process so the restored pages/settings are re-read from disk.
     */
    private void showImportResult(String summary) {
        String body = summary + "\n\n" + mActivity.getString(R.string.rkb_eim_restart_hint);
        LinearLayout box = infoBox(mActivity.getString(R.string.rkb_eim_import_done_title), body);
        final AlertDialog dialog = infoDialog(box, false);

        LinearLayout buttons = new LinearLayout(mActivity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(16), 0, 0);
        Button later = pill(mActivity.getString(R.string.rkb_eim_restart_later), v -> {
            dialog.dismiss();
            dismiss();
            mHost.onChainFinished();
        });
        ((LinearLayout.LayoutParams) later.getLayoutParams()).rightMargin = dp(10);
        buttons.addView(later);
        buttons.addView(pill(mActivity.getString(R.string.rkb_eim_restart_now), v -> restartApp()));
        box.addView(buttons);
        dialog.show();
        transparentWindow(dialog);
    }

    private void restartApp() {
        Intent launch = mActivity.getPackageManager().getLaunchIntentForPackage(mActivity.getPackageName());
        if (launch == null) {
            launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(mActivity.getPackageName());
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mActivity.startActivity(launch);
        Runtime.getRuntime().exit(0);
    }

    // ---- the export directory ------------------------------------------------------------------

    /** Called by the host after the SAF folder picker returns. */
    public void onDirPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        storeDir(mActivity, uri);
        rebuild();
    }

    /**
     * Persist a folder picked with ACTION_OPEN_DOCUMENT_TREE as the export directory, taking a
     * persistable read/write grant so it survives reboots. Shared with the UI settings page, whose
     * directory row opens the same picker.
     */
    public static void storeDir(Context context, Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            context.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            // pass — some providers do not offer a persistable grant; the session grant still works
        }
        RkbExport.setExportDirUri(context, uri);
    }

    // ---- info dialogs --------------------------------------------------------------------------

    /**
     * A bordered black-yellow info dialog with a single OK. When {@code closeChain} is set (a
     * successful export), acknowledging it closes this panel and the UI settings page too; failures
     * only dismiss the dialog, leaving the panel open to retry.
     */
    private void showInfo(String title, String body, final boolean closeChain) {
        LinearLayout box = infoBox(title, body);
        final AlertDialog dialog = infoDialog(box, !closeChain);

        LinearLayout buttons = new LinearLayout(mActivity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(16), 0, 0);
        buttons.addView(pill(mActivity.getString(android.R.string.ok), v -> {
            dialog.dismiss();
            if (closeChain) {
                dismiss();
                mHost.onChainFinished();
            }
        }));
        box.addView(buttons);
        dialog.show();
        transparentWindow(dialog);
    }

    private LinearLayout infoBox(String title, String body) {
        LinearLayout box = new LinearLayout(mActivity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(20), dp(22), dp(16));
        box.setBackground(panelBackground());
        TextView titleView = text(title, 19, UiTheme.color(UiSlot.DIALOG_TITLE), true);
        UiTheme.applyFont(titleView, UiSlot.DIALOG_TITLE);
        box.addView(titleView);
        TextView bodyView = text(body, 14, UiTheme.color(UiSlot.DIALOG_TEXT), false);
        bodyView.setPadding(0, dp(10), 0, 0);
        box.addView(bodyView);
        return box;
    }

    private AlertDialog infoDialog(View content, boolean cancelable) {
        ScrollView scroll = new ScrollView(mActivity);
        int m = dp(10);
        scroll.setPadding(m, m, m, m);
        scroll.setClipToPadding(false);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(mActivity).setView(scroll).create();
        dialog.setCancelable(cancelable);
        return dialog;
    }

    private void transparentWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    // ---- view builders -------------------------------------------------------------------------

    /** The bordered rounded panel every surface of this window is drawn on. */
    private GradientDrawable panelBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiTheme.color(UiSlot.DIALOG_BG));
        bg.setStroke(Math.max(1, dp(UiTheme.borderWidthDp(UiSlot.DIALOG_BORDER) > 0
                        ? UiTheme.borderWidthDp(UiSlot.DIALOG_BORDER) : 2)),
                UiTheme.color(UiSlot.DIALOG_BORDER));
        bg.setCornerRadius(dp(16));
        return bg;
    }

    private TextView text(CharSequence s, int sizeSp, int color, boolean bold) {
        TextView tv = new TextView(mActivity);
        tv.setText(s);
        tv.setTextColor(color);
        // Size first, font second: a size the user set on the slot then wins over our default.
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        UiTheme.applyFont(tv, UiSlot.DIALOG_TEXT);
        if (bold) {
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        }
        return tv;
    }

    private CheckBox checkbox(String label, boolean bold, int indent) {
        CheckBox cb = new CheckBox(mActivity);
        cb.setText(label);
        cb.setTextColor(UiTheme.color(UiSlot.DIALOG_TEXT));
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        UiTheme.applyFont(cb, UiSlot.DIALOG_TEXT);
        if (bold) {
            cb.setTypeface(cb.getTypeface(), Typeface.BOLD);
        }
        cb.setButtonTintList(ColorStateList.valueOf(UiTheme.color(UiSlot.DIALOG_BUTTON)));
        cb.setPadding(dp(8) + indent, dp(7), 0, dp(7));
        return cb;
    }

    private View divider(int topGap) {
        View v = new View(mActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        lp.topMargin = dp(topGap);
        v.setLayoutParams(lp);
        v.setBackgroundColor(UiTheme.color(UiSlot.DIALOG_BORDER));
        v.setAlpha(0.4f);
        return v;
    }

    /** An ArcaneChat-style round pill: panel fill, thin accent stroke, accent text, accent ripple. */
    private Button pill(String label, View.OnClickListener onClick) {
        Button b = new Button(mActivity);
        b.setText(label);
        b.setAllCaps(false);
        int accent = UiTheme.color(UiSlot.DIALOG_BUTTON);
        b.setTextColor(accent);
        UiTheme.applyFont(b, UiSlot.DIALOG_BUTTON);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiTheme.color(UiSlot.DIALOG_BG));
        bg.setStroke(Math.max(1, Math.round(1.5f * mDensity)), accent);
        bg.setCornerRadius(dp(50));
        b.setBackground(new RippleDrawable(
                ColorStateList.valueOf((accent & 0x00FFFFFF) | 0x33000000), bg, null));
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(20), dp(8), dp(20), dp(8));
        b.setOnClickListener(onClick);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    private int dp(float v) {
        return Math.round(v * mDensity);
    }
}
