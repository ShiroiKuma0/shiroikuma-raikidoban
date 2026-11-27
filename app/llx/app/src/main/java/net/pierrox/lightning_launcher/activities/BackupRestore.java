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

import net.pierrox.lightning_launcher.util.UiIcons;
import net.pierrox.lightning_launcher.util.UiPills;
import net.pierrox.lightning_launcher.util.UiSectionIndex;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.data.BackupRestoreTool;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.RkbExport;
import net.pierrox.lightning_launcher.util.BackupFolder;
import net.pierrox.lightning_launcher.util.Flash;
import net.pierrox.lightning_launcher.util.ThemedProgressDialog;
import net.pierrox.lightning_launcher.util.UiDialogStyler;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class BackupRestore extends ResourceWrapperActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, OnLongClickListener {
    private static final int DIALOG_SELECT_ARCHIVE_NAME = 3;
    private static final int DIALOG_SELECT_BACKUP_ACTION = 4;
    private static final int DIALOG_CONFIRM_RESTORE = 5;
    private static final int DIALOG_CONFIRM_DELETE = 6;

    private static final int REQUEST_SELECT_PAGES_FOR_EXPORT = 1;
    private static final int REQUEST_SELECT_FILE_TO_IMPORT = 2;
    private static final int REQUEST_SELECT_FILE_TO_LOAD = 3;
    private static final int REQUEST_PICK_FOLDER = 4;

    private static final int MENU_PICK_FOLDER = 100;

    // name dialog modes
    private static final int MODE_BACKUP = 0;
    private static final int MODE_TEMPLATE = 1;
    private static final int MODE_RENAME = 2;

    // value of mPendingExport when no export is waiting for a folder to be picked
    private static final int PENDING_NONE = -1;

    private ListView mListView;
    private TextView mEmptyView;

    // The configured backup folder (SAF tree) and its current content. Single source for the list.
    private DocumentFile mArchiveDir;
    private final ArrayList<DocumentFile> mArchives = new ArrayList<>();

    private int mNameDialogMode = MODE_BACKUP;
    private int mPendingExport = PENDING_NONE;
    private String mPendingExportName;

    // currently selected in-folder archive (long press / tap / rename / delete target)
    private DocumentFile mSelected;

    // ad-hoc restore (file picked outside the configured folder, or opened via ACTION_VIEW)
    private Uri mArchiveUri;
    private String mArchiveName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.backup_restore);

        Button backup = findViewById(R.id.backup);
        backup.setText(R.string.backup_t);
        backup.setOnClickListener(this);

        Button import_ = findViewById(R.id.import_);
        import_.setText(R.string.import_t);
        import_.setOnClickListener(this);
        import_.setOnLongClickListener(this);

        Button export = findViewById(R.id.export);
        export.setText(R.string.tmpl_e_t);
        export.setOnClickListener(this);
        export.setOnLongClickListener(this);

        Button prune = findViewById(R.id.prune);
        prune.setText(R.string.br_prune);
        prune.setOnClickListener(this);

        mListView = findViewById(R.id.archives);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        mEmptyView = findViewById(R.id.empty);

        // The archive list rows come from the platform's simple_list_item_1 and the empty label from
        // ?textAppearanceLarge — both white until painted.
        UiTheme.paintUnstyledText(mListView, UiSlot.PREF_TITLE);
        UiTheme.applyTo(mEmptyView, UiSlot.PREF_TITLE);

        styleButtons();
        decorate();

        loadArchivesList();
    }

    // 「白い熊 雷起動盤 UI」: paint the three top buttons with the BUTTONS slots (black bg + yellow border
    // by default) instead of the platform's grey button background.
    private void styleButtons() {
        // Backup is what this screen is for: the thick, bold pill. Prune deletes: the red one.
        net.pierrox.lightning_launcher.util.UiChrome.applyMainButton((Button) findViewById(R.id.backup));
        net.pierrox.lightning_launcher.util.UiChrome.applyButton((Button) findViewById(R.id.export));
        net.pierrox.lightning_launcher.util.UiChrome.applyButton((Button) findViewById(R.id.import_));
        net.pierrox.lightning_launcher.util.UiChrome.applyDestructiveButton((Button) findViewById(R.id.prune));
        // Regular pills: the page's text font at 15 sp, not the Buttons font and size of the UI page
        // (白い熊, 2026-11-27) — only the main one is bold. Done HERE because onResume styles the buttons
        // again: set anywhere else, the Buttons font came straight back.
        int[] ids = {R.id.backup, R.id.export, R.id.import_, R.id.prune};
        for (int i = 0; i < ids.length; i++) {
            Button b = findViewById(ids[i]);
            int style = i == 0 ? Typeface.BOLD : Typeface.NORMAL;
            b.setTypeface(UiTheme.typeface(UiSlot.TEXT, style), style);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        }
    }

    /**
     * 白い熊: the look of the screen — a heading, the four actions as pills that hug their label (icon
     * inside, coloured by what the action does) in a wrapping row, the explanation with its left rule,
     * and the heading of the list. Colours come from the UI roles; the layout carries none.
     */
    private void decorate() {
        int accent = UiTheme.color(UiSlot.ACCENT);
        int note = UiTheme.color(UiSlot.NOTE);
        float density = getResources().getDisplayMetrics().density;

        TextView title = findViewById(R.id.br_title);
        title.setText(R.string.br_title);
        title.setTextColor(accent);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(UiTheme.typeface(UiSlot.PREF_TITLE, Typeface.BOLD), Typeface.BOLD);
        title.setCompoundDrawablesRelative(
                UiIcons.drawable(this, UiIcons.Icon.ARCHIVE, UiIcons.color(UiIcons.Does.SHARE), 22f), null, null, null);
        title.setCompoundDrawablePadding(Math.round(8 * density));
        findViewById(R.id.br_title_line).setBackgroundColor(accent);

        FrameLayout host = findViewById(R.id.br_actions);
        UiSectionIndex.Flow flow = new UiSectionIndex.Flow(this, Math.round(8 * density));
        int[] ids = {R.id.backup, R.id.export, R.id.import_, R.id.prune};
        UiIcons.Icon[] icons = {UiIcons.Icon.EXPORT, UiIcons.Icon.STORE, UiIcons.Icon.IMPORT, UiIcons.Icon.DELETE};
        UiIcons.Does[] does = {UiIcons.Does.SHARE, UiIcons.Does.SHARE, UiIcons.Does.CONNECT, UiIcons.Does.DELETE};
        boolean[] opens = {false, true, true, false};
        for (int i = 0; i < ids.length; i++) {
            Button b = findViewById(ids[i]);
            host.removeView(b);
            b.setMaxLines(2);
            if (opens[i]) {
                b.setText(b.getText() + "  \u203A");
            }
            b.setCompoundDrawablesRelative(
                    UiIcons.drawable(this, icons[i], UiIcons.color(does[i]), 20f), null, null, null);
            b.setCompoundDrawablePadding(Math.round(8 * density));
            flow.addView(b, new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        host.addView(flow);

        TextView hint = findViewById(R.id.br_hint);
        hint.setText(R.string.br_hint);
        hint.setTextColor(note);
        findViewById(R.id.br_hint_rule).setBackgroundColor(note);

        // The screen is a dialog: a framed panel almost as large as the screen over the dimmed
        // launcher, with the list of backups in a framed box of its own — the only part that scrolls.
        android.graphics.drawable.Drawable panel = net.pierrox.lightning_launcher.util.UiDialogStyler.panelBackground(this);
        if (panel == null) {
            panel = UiPills.background(this, UiTheme.color(UiSlot.DIALOG_BG), 0f,
                    UiTheme.cornerRadiusDp(UiSlot.DIALOG_BORDER), UiTheme.color(UiSlot.DIALOG_BG));
        }
        findViewById(R.id.br_panel).setBackground(panel);
        findViewById(R.id.br_outside).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.br_box).setBackground(UiPills.background(this, UiTheme.color(UiSlot.DIALOG_BORDER),
                UiPills.REGULAR_DP, Math.max(4, UiTheme.cornerRadiusDp(UiSlot.DIALOG_BORDER) - 4),
                android.graphics.Color.TRANSPARENT));

        Button close = findViewById(R.id.br_close);
        close.setText(R.string.br_close);
        net.pierrox.lightning_launcher.util.UiChrome.applyButton(close);
        close.setTypeface(UiTheme.typeface(UiSlot.TEXT, Typeface.NORMAL), Typeface.NORMAL);
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        close.setOnClickListener(v -> onBackPressed());

        TextView listTitle = findViewById(R.id.br_list_title);
        listTitle.setText(R.string.br_list);
        listTitle.setTextColor(accent);
        listTitle.setTypeface(UiTheme.typeface(UiSlot.PREF_TITLE, Typeface.BOLD), Typeface.BOLD);
        findViewById(R.id.br_list_title_line).setBackgroundColor(accent);
        ((TextView) findViewById(R.id.br_list_count)).setTextColor(note);

        mListView.setDivider(new android.graphics.drawable.ColorDrawable(
                UiPills.blend(UiTheme.color(UiSlot.BACKGROUND), accent, 0.35f)));
        mListView.setDividerHeight(1);
    }

    private static final java.util.regex.Pattern STAMPED_NAME =
            java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})[ _](\\d{2})-(\\d{2})(?:-\\d{2})?\\.lla$");

    /**
     * One row of the list: the archive icon, the date and time in bold with the file name beneath in
     * dim gold (a file whose name carries no stamp shows its name in bold and its date beneath), a
     * "newest" mark on the first row, and a › at the right.
     */
    private View archiveRow(int position, String[] names, long[] times) {
        float density = getResources().getDisplayMetrics().density;
        int text = UiTheme.color(UiSlot.PREF_TITLE);
        int note = UiTheme.color(UiSlot.NOTE);

        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(Math.round(4 * density), Math.round(7 * density), Math.round(4 * density), Math.round(7 * density));

        View icon = new View(this);
        icon.setBackground(UiIcons.drawable(this, UiIcons.Icon.ARCHIVE, UiIcons.color(UiIcons.Does.SHARE), 22f));
        android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(
                Math.round(22 * density), Math.round(22 * density));
        ilp.setMarginEnd(Math.round(14 * density));
        row.addView(icon, ilp);

        String name = names[position] == null ? "" : names[position];
        java.util.regex.Matcher m = STAMPED_NAME.matcher(name);
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(new Date(times[position]));
        boolean stamped = m.find();
        String main = stamped ? m.group(1) + " " + m.group(2) + ":" + m.group(3) : name;
        String sub = stamped ? name : stamp;

        android.widget.LinearLayout col = new android.widget.LinearLayout(this);
        col.setOrientation(android.widget.LinearLayout.VERTICAL);

        android.widget.LinearLayout top = new android.widget.LinearLayout(this);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText(main);
        label.setTextColor(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setTypeface(stamped ? Typeface.MONOSPACE : UiTheme.typeface(UiSlot.PREF_TITLE, Typeface.BOLD), Typeface.BOLD);
        label.setMaxLines(2);
        top.addView(label);
        if (position == 0 && names.length > 1) {
            TextView newest = new TextView(this);
            newest.setText(R.string.br_newest);
            newest.setTextColor(note);
            newest.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            newest.setBackground(UiPills.background(this, note, 1f, 100f, android.graphics.Color.TRANSPARENT));
            newest.setPadding(Math.round(8 * density), 0, Math.round(8 * density), 0);
            android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.setMarginStart(Math.round(10 * density));
            top.addView(newest, nlp);
        }
        col.addView(top);

        TextView below = new TextView(this);
        below.setText(sub);
        below.setTextColor(note);
        below.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        below.setMaxLines(2);
        col.addView(below);
        row.addView(col, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView chevron = new TextView(this);
        chevron.setText("\u203A");
        chevron.setTextColor(text);
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        chevron.setPadding(Math.round(8 * density), 0, Math.round(4 * density), 0);
        row.addView(chevron);
        return row;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PICK_FOLDER, 0, R.string.backup_folder_select)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        net.pierrox.lightning_launcher.util.MenuPills.decorate(this, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_PICK_FOLDER) {
            pickFolder(PENDING_NONE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Launched as a launcher action (the gesture binding carries the shortcut label): the screen
        // underneath may be the app drawer, not the desktop. Go home explicitly instead of finishing —
        // Dashboard is singleTask, so this also pops the drawer above it.
        if (getIntent().hasExtra(LightningIntent.INTENT_EXTRA_SHORTCUT_LABEL)) {
            startActivity(new Intent(this, Dashboard.class));
            finish();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-apply so 「白い熊 雷起動盤 UI」 button edits show on return from the settings screen.
        styleButtons();

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_MAIN);
            confirmRestore(intent.getData(), null);
        }
    }

    private void loadArchivesList() {
        mArchives.clear();
        mArchiveDir = BackupFolder.getDir(this);
        findViewById(R.id.br_list_head).setVisibility(View.GONE);

        if (mArchiveDir == null) {
            // Two different states, and telling them apart is the whole point: never chosen, or
            // chosen on a phone that is gone — a backup carries the folder's Uri but no platform can
            // carry its grant (see SafFolder). The remembered one is named and tappable, and the
            // picker opens right there.
            Uri remembered = BackupFolder.getRememberedUri(this);
            String path = RkbExport.absolutePathOf(remembered, null);
            if (remembered != null) {
                mEmptyView.setText(getString(R.string.backup_folder_regrant,
                        path != null ? path : remembered.toString()));
                mEmptyView.setOnClickListener(v -> pickFolder(PENDING_NONE));
            } else {
                mEmptyView.setText(R.string.backup_folder_none);
                mEmptyView.setOnClickListener(null);
                mEmptyView.setClickable(false);
            }
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            return;
        }
        mEmptyView.setOnClickListener(null);
        mEmptyView.setClickable(false);

        for (DocumentFile f : mArchiveDir.listFiles()) {
            if (f.isFile()) {
                mArchives.add(f);
            }
        }
        Collections.sort(mArchives, new Comparator<DocumentFile>() {
            @Override
            public int compare(DocumentFile a, DocumentFile b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });

        if (mArchives.isEmpty()) {
            mEmptyView.setText(R.string.no_backup_archive);
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        } else {
            final String[] names = new String[mArchives.size()];
            final long[] times = new long[names.length];
            for (int i = 0; i < names.length; i++) {
                names[i] = mArchives.get(i).getName();
                times[i] = mArchives.get(i).lastModified();
            }
            mListView.setAdapter(new ArrayAdapter<String>(this, 0, names) {
                @Override
                public View getView(int position, View convertView, android.view.ViewGroup parent) {
                    return archiveRow(position, names, times);
                }
            });
            ((TextView) findViewById(R.id.br_list_count)).setText(getString(R.string.br_count, names.length));
            findViewById(R.id.br_list_head).setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backup:
                exportArchive(MODE_BACKUP);
                break;

            case R.id.import_:
                selectFileToLoadOrImport(true);
                break;

            case R.id.export:
                exportArchive(MODE_TEMPLATE);
                break;

            case R.id.prune:
                pruneOldBackups();
                break;
        }
    }

    /**
     * Prune: delete every backup in the backup directory except the newest one. Only full backups
     * count — a template (an archive with a manifest) and any file that is not an ".lla" archive are
     * never touched, so the newest backup is kept even when a template is newer. Nothing is deleted
     * before the confirmation, which names the count and the backup that stays.
     */
    @SuppressLint("StaticFieldLeak")
    private void pruneOldBackups() {
        if (mArchiveDir == null) {
            pickFolder(PENDING_NONE);
            return;
        }
        // mArchives is sorted newest first, so the first backup found is the one to keep
        final ArrayList<DocumentFile> candidates = new ArrayList<>(mArchives);
        new AsyncTask<Void, Void, ArrayList<DocumentFile>>() {
            private AlertDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = ThemedProgressDialog.show(BackupRestore.this, getString(R.string.br_prune_p));
            }

            @Override
            protected ArrayList<DocumentFile> doInBackground(Void... voids) {
                ArrayList<DocumentFile> backups = new ArrayList<>();
                for (DocumentFile f : candidates) {
                    String name = f.getName();
                    if (name == null || !name.toLowerCase().endsWith(".lla")) {
                        continue;
                    }
                    InputStream is = null;
                    try {
                        is = getContentResolver().openInputStream(f.getUri());
                        if (is == null || BackupRestoreTool.readManifest(is) != null) {
                            // unreadable, or a template
                            continue;
                        }
                    } catch (Exception e) {
                        // cannot tell what it is: leave it alone
                        continue;
                    } finally {
                        if (is != null) try {
                            is.close();
                        } catch (IOException e) {
                        }
                    }
                    backups.add(f);
                }
                return backups;
            }

            @Override
            protected void onPostExecute(final ArrayList<DocumentFile> backups) {
                mDialog.dismiss();
                if (isFinishing()) {
                    return;
                }
                if (backups.size() < 2) {
                    Flash.show(BackupRestore.this, getString(R.string.br_prune_none));
                    return;
                }
                final DocumentFile kept = backups.remove(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(BackupRestore.this);
                builder.setTitle(R.string.br_prune_c);
                builder.setMessage(getString(R.string.br_prune_m, backups.size(), kept.getName()));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteBackups(backups);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                UiDialogStyler.showDestructive(builder);
            }
        }.execute((Void) null);
    }

    @SuppressLint("StaticFieldLeak")
    private void deleteBackups(final ArrayList<DocumentFile> backups) {
        new AsyncTask<Void, Void, Integer>() {
            private AlertDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = ThemedProgressDialog.show(BackupRestore.this, getString(R.string.br_prune_p));
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                int deleted = 0;
                for (DocumentFile f : backups) {
                    try {
                        if (f.delete()) {
                            deleted++;
                        }
                    } catch (Exception e) {
                        // counted as not deleted
                    }
                }
                return deleted;
            }

            @Override
            protected void onPostExecute(Integer deleted) {
                mDialog.dismiss();
                Flash.show(BackupRestore.this, getString(R.string.br_prune_done, deleted, backups.size()), Toast.LENGTH_LONG);
                loadArchivesList();
            }
        }.execute((Void) null);
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.export:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pierrox.net/cmsms/applications/lightning-launcher/templates.html")));
                return true;

            case R.id.import_:
                selectFileToLoadOrImport(false);
                return true;
        }
        return false;
    }

    private void pickFolder(int pendingExport) {
        mPendingExport = pendingExport;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // Open at the folder we remember, so re-granting a restored one is a single tap rather than
        // a walk back down the tree.
        Uri remembered = BackupFolder.getRememberedUri(this);
        if (remembered != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, remembered);
        }
        startActivityForResult(intent, REQUEST_PICK_FOLDER);
    }

    // 「白い熊 雷起動盤 UI」: give these framework dialogs the themed bordered panel (the yellow
    // DIALOG_BORDER frame + rounded DIALOG_BG), matching the rest of the app's dialogs. Styled on show
    // so the title / buttons exist by the time UiDialogStyler runs.
    private AlertDialog withChrome(AlertDialog dialog) {
        return UiDialogStyler.styleOnShow(dialog);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;

        switch (id) {
            case DIALOG_SELECT_ARCHIVE_NAME:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.br_n);
                final String archive_name;
                if (mArchiveName == null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm");
                    archive_name = getString(mNameDialogMode == MODE_TEMPLATE ? R.string.tmpl_fn : R.string.backup_d) + "-" + sdf.format(new Date()) + ".lla";
                } else {
                    archive_name = mArchiveName;
                }
                final EditText edit_text = new EditText(this);
                edit_text.setText(archive_name);
                edit_text.setSelection(archive_name.length());
                FrameLayout l = new FrameLayout(this);
                l.setPadding(10, 10, 10, 10);
                l.addView(edit_text);
                builder.setView(l);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = edit_text.getText().toString().trim();
                        if (mNameDialogMode == MODE_RENAME) {
                            if (mSelected != null && mSelected.renameTo(name)) {
                                loadArchivesList();
                            }
                        } else if (mNameDialogMode == MODE_TEMPLATE) {
                            // defer document creation until the desktops have been picked
                            mPendingExportName = name;
                            selectDesktopsToExport();
                        } else {
                            DocumentFile doc = BackupFolder.createDoc(BackupRestore.this, name);
                            if (doc == null) {
                                Flash.show(BackupRestore.this, getString(R.string.backup_folder_lost), Toast.LENGTH_LONG);
                            } else {
                                new BackupTask(doc).execute();
                            }
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return withChrome(builder.create());

            case DIALOG_CONFIRM_RESTORE:
                if (mArchiveUri != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_rc);
                    builder.setMessage(mArchiveName == null ? getNameForUri(mArchiveUri) : mArchiveName);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new RestoreTask(mArchiveUri).execute();
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return withChrome(builder.create());
                }
                break;

            case DIALOG_CONFIRM_DELETE:
                if (mSelected != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_dc);
                    builder.setMessage(mSelected.getName());
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mSelected != null) {
                                mSelected.delete();
                            }
                            loadArchivesList();
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return withChrome(UiDialogStyler.destructive(builder.create()));
                }
                break;

            case DIALOG_SELECT_BACKUP_ACTION:
                if (mSelected != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_a);
                    ArrayList<UiDialogStyler.Action> actions = new ArrayList<>();
                    actions.add(new UiDialogStyler.Action(getString(R.string.br_ob), UiIcons.Icon.ARCHIVE, UiIcons.Does.SHARE, false));
                    actions.add(new UiDialogStyler.Action(getString(R.string.br_ot), UiIcons.Icon.LAYERS, UiIcons.Does.SHARE, true));
                    actions.add(new UiDialogStyler.Action(getString(R.string.br_r), UiIcons.Icon.EDIT, UiIcons.Does.MESSAGE, true));
                    actions.add(new UiDialogStyler.Action(getString(R.string.br_s), UiIcons.Icon.SHARE, UiIcons.Does.SHARE, true));
                    actions.add(new UiDialogStyler.Action(getString(R.string.br_d), UiIcons.Icon.DELETE, UiIcons.Does.DELETE, true));
                    builder.setAdapter(UiDialogStyler.actionAdapter(this, actions), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mSelected == null) {
                                return;
                            }
                            switch (i) {
                                case 0:
                                    // overwrite backup: re-create the document with the same name
                                    DocumentFile doc = BackupFolder.createDoc(BackupRestore.this, mSelected.getName());
                                    if (doc == null) {
                                        Flash.show(BackupRestore.this, getString(R.string.backup_folder_lost), Toast.LENGTH_LONG);
                                    } else {
                                        new BackupTask(doc).execute();
                                    }
                                    break;

                                case 1:
                                    // overwrite template: the document is (re)created after the desktops are picked
                                    mPendingExportName = mSelected.getName();
                                    selectDesktopsToExport();
                                    break;

                                case 2:
                                    mArchiveName = mSelected.getName();
                                    mNameDialogMode = MODE_RENAME;
                                    try {
                                        removeDialog(DIALOG_SELECT_ARCHIVE_NAME);
                                    } catch (Exception e) {
                                    }
                                    showDialog(DIALOG_SELECT_ARCHIVE_NAME);
                                    break;

                                case 3:
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, mSelected.getUri());
                                    shareIntent.setType("application/zip");
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.br_s)));
                                    break;

                                case 4:
                                    try {
                                        removeDialog(DIALOG_CONFIRM_DELETE);
                                    } catch (Exception e) {
                                    }
                                    showDialog(DIALOG_CONFIRM_DELETE);
                                    break;
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return withChrome(builder.create());
                }
                break;
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i >= 0 && i < mArchives.size()) {
            DocumentFile f = mArchives.get(i);
            confirmRestore(f.getUri(), f.getName());
        }
    }

    private void confirmRestore(Uri archiveUri, String archiveName) {
        mArchiveUri = archiveUri;
        mArchiveName = archiveName;
        try {
            removeDialog(DIALOG_CONFIRM_RESTORE);
        } catch (Exception e) {
        }
        showDialog(DIALOG_CONFIRM_RESTORE);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i >= 0 && i < mArchives.size()) {
            mSelected = mArchives.get(i);
            try {
                removeDialog(DIALOG_SELECT_BACKUP_ACTION);
            } catch (Exception e) {
            }
            showDialog(DIALOG_SELECT_BACKUP_ACTION);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_PAGES_FOR_EXPORT) {
            if (resultCode == RESULT_OK) {
                int[] selected_pages = data.getIntArrayExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS);

                ArrayList<Integer> all_pages = new ArrayList<Integer>();
                for (int p : selected_pages) {
                    all_pages.add(Integer.valueOf(p));
                    addSubPages(all_pages, p);
                }
                all_pages.add(Integer.valueOf(Page.APP_DRAWER_PAGE));
                all_pages.add(Integer.valueOf(Page.USER_MENU_PAGE));

                DocumentFile target = mPendingExportName == null ? null : BackupFolder.createDoc(this, mPendingExportName);
                mPendingExportName = null;
                if (target == null) {
                    Flash.show(this, getString(R.string.backup_folder_lost), Toast.LENGTH_LONG);
                } else {
                    doExportTemplate(target, all_pages);
                }
            } else {
                mPendingExportName = null;
            }
        } else if (requestCode == REQUEST_SELECT_FILE_TO_IMPORT) {
            if (resultCode == RESULT_OK) {
                importFile(data.getData());
            }
        } else if (requestCode == REQUEST_SELECT_FILE_TO_LOAD) {
            if (resultCode == RESULT_OK) {
                confirmRestore(data.getData(), null);
            }
        } else if (requestCode == REQUEST_PICK_FOLDER) {
            int pending = mPendingExport;
            mPendingExport = PENDING_NONE;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                BackupFolder.setTreeUri(this, data.getData());
                loadArchivesList();
                if (pending != PENDING_NONE) {
                    exportArchive(pending);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addSubPages(ArrayList<Integer> all_pages, int p) {
        Page page = LLApp.get().getAppEngine().getOrLoadPage(p);
        for (Item i : page.items) {
            if (i instanceof Folder) {
                int folder_page_id = ((Folder) i).getFolderPageId();
                all_pages.add(Integer.valueOf(folder_page_id));
                addSubPages(all_pages, folder_page_id);
            }
        }
    }

    /**
     * Start a backup ({@link #MODE_BACKUP}) or a template export ({@link #MODE_TEMPLATE}). If no
     * backup folder is configured yet, prompt for one first and resume once it has been picked.
     */
    private void exportArchive(int mode) {
        if (BackupFolder.getDir(this) == null) {
            pickFolder(mode);
            return;
        }
        mNameDialogMode = mode;
        mArchiveName = null;
        try {
            removeDialog(DIALOG_SELECT_ARCHIVE_NAME);
        } catch (Exception e) {
        }
        showDialog(DIALOG_SELECT_ARCHIVE_NAME);
    }

    private void selectDesktopsToExport() {
        Intent intent = new Intent(this, ScreenManager.class);
        intent.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS, LLApp.get().getAppEngine().getGlobalConfig().screensOrder);
        intent.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_TITLE, getString(R.string.tmpl_s_p));
        startActivityForResult(intent, REQUEST_SELECT_PAGES_FOR_EXPORT);
    }

    @SuppressLint("StaticFieldLeak")
    private void doExportTemplate(final DocumentFile target, final ArrayList<Integer> included_pages) {
        Rect r = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        final int sb_height = r.top;

        new AsyncTask<Void, Void, Boolean>() {
            private AlertDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = ThemedProgressDialog.show(BackupRestore.this, getString(R.string.tmpl_e_m));
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                BackupRestoreTool.BackupConfig backup_config = new BackupRestoreTool.BackupConfig();

                backup_config.context = BackupRestore.this;
                PackageManager pm = getPackageManager();
                try {
                    PackageInfo pi = pm.getPackageInfo(BackupRestore.this.getPackageName(), 0);
                    final String data_dir = pi.applicationInfo.dataDir + "/files";
                    backup_config.pathFrom = data_dir;
                } catch (PackageManager.NameNotFoundException e) {
                    // pass
                }
                backup_config.uriTo = target.getUri();
                backup_config.includeWidgetsData = true;
                backup_config.includeWallpaper = true;
                backup_config.includeFonts = true;
                backup_config.forTemplate = true;
                backup_config.statusBarHeight = sb_height;
                backup_config.pagesToInclude = included_pages;

                Exception exception = BackupRestoreTool.backup(backup_config);
                if (exception != null) {
                    target.delete();
                }
                return exception == null;
            }

            @Override
            protected void onPostExecute(Boolean ok) {
                mDialog.dismiss();
                Flash.show(BackupRestore.this, getString(ok ? R.string.tmpl_e_d : R.string.tmpl_e_e));
                loadArchivesList();
            }
        }.execute((Void) null);
    }

    /**
     * Request the user to pick an external file.
     *
     * @param only_load true to directly restore the file, false to first copy it into the
     *                  configured backup folder (so it is kept alongside the other archives).
     */
    private void selectFileToLoadOrImport(boolean only_load) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.import_t)), only_load ? REQUEST_SELECT_FILE_TO_LOAD : REQUEST_SELECT_FILE_TO_IMPORT);
    }

    @SuppressLint("StaticFieldLeak")
    private void importFile(final Uri uri) {
        if (BackupFolder.getDir(this) == null) {
            // no folder to copy into: just restore the picked file directly
            confirmRestore(uri, null);
            return;
        }
        new AsyncTask<Void, Void, DocumentFile>() {
            private AlertDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = ThemedProgressDialog.show(BackupRestore.this, getString(R.string.importing));
            }

            @Override
            protected DocumentFile doInBackground(Void... voids) {
                InputStream is = null;
                OutputStream os = null;
                DocumentFile target = null;
                try {
                    String name = getNameForUri(uri);
                    target = BackupFolder.createDoc(BackupRestore.this, name);
                    if (target == null) {
                        return null;
                    }
                    is = getContentResolver().openInputStream(uri);
                    os = getContentResolver().openOutputStream(target.getUri());
                    FileUtils.copyStream(is, os);
                    return target;
                } catch (IOException e) {
                    if (target != null) {
                        target.delete();
                    }
                    return null;
                } finally {
                    try {
                        if (is != null) is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (os != null) os.close();
                    } catch (IOException e) {
                    }
                }
            }

            @Override
            protected void onPostExecute(DocumentFile target) {
                mDialog.dismiss();
                if (target != null) {
                    loadArchivesList();
                    confirmRestore(target.getUri(), target.getName());
                } else {
                    Flash.show(BackupRestore.this, R.string.import_e);
                }
            }
        }.execute((Void) null);
    }

    private String getNameForUri(Uri uri) {
        String name = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return name;
    }

    @SuppressLint("StaticFieldLeak")
    private class BackupTask extends AsyncTask<Void, Void, Exception> {
        private final DocumentFile mTarget;
        private AlertDialog mDialog;

        private BackupTask(DocumentFile target) {
            mTarget = target;
        }

        @Override
        protected void onPreExecute() {
            mDialog = ThemedProgressDialog.show(BackupRestore.this, getString(R.string.backup_in_progress));
        }

        @Override
        protected Exception doInBackground(Void... params) {
            BackupRestoreTool.BackupConfig backup_config = new BackupRestoreTool.BackupConfig();

            backup_config.context = BackupRestore.this;
            PackageManager pm = getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(BackupRestore.this.getPackageName(), 0);
                final String data_dir = pi.applicationInfo.dataDir + "/files";
                backup_config.pathFrom = data_dir;
            } catch (PackageManager.NameNotFoundException e) {
                // pass
            }
            backup_config.uriTo = mTarget.getUri();
            backup_config.includeWidgetsData = true;
            backup_config.includeWallpaper = true;
            backup_config.includeFonts = true;

            return BackupRestoreTool.backup(backup_config);
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (mDialog != null) {
                mDialog.dismiss();
            }

            String name = mTarget.getName();

            if (result != null) {
                mTarget.delete();

                Writer out = new StringWriter(1000);

                PrintWriter printWriter = new PrintWriter(out);
                result.printStackTrace(printWriter);

                Intent email_intent = new Intent(android.content.Intent.ACTION_SEND, Uri.parse("mailto:"));
                email_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                email_intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"pierrox@pierrox.net"});
                email_intent.putExtra(Intent.EXTRA_SUBJECT, "Backup error");
                email_intent.putExtra(Intent.EXTRA_TEXT, out.toString());
                email_intent.setType("message/rfc822");
                startActivity(Intent.createChooser(email_intent, "Backup error, please send a bug report by email"));
            }
            String msg = (result == null ? getString(R.string.backup_done, name) : getString(R.string.backup_error));
            Flash.show(BackupRestore.this, msg, Toast.LENGTH_LONG);

            loadArchivesList();
        }
    }

    private class RestoreTask extends AsyncTask<Void, Void, Integer> {
        private final Uri mUri;
        private AlertDialog mDialog;

        private RestoreTask(Uri uri) {
            mUri = uri;
        }

        @Override
        protected void onPreExecute() {
            mDialog = ThemedProgressDialog.show(BackupRestore.this, getString(R.string.restore_in_progress));
        }

        @Override
        protected Integer doInBackground(Void... params) {
            BackupRestoreTool.RestoreConfig restore_config = new BackupRestoreTool.RestoreConfig();

            InputStream is = null;
            try {
                ContentResolver cr = getContentResolver();
                is = cr.openInputStream(mUri);
                JSONObject manifest = BackupRestoreTool.readManifest(is);
                if (manifest != null) {
                    // it looks like a template
                    return 2;
                }
            } catch (Exception e) {
                // not a template, continue with normal restore
            } finally {
                if (is != null) try {
                    is.close();
                } catch (IOException e) {
                }
            }

            restore_config.context = BackupRestore.this;
            PackageManager pm = getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(BackupRestore.this.getPackageName(), 0);
                final String data_dir = pi.applicationInfo.dataDir + "/files";
                restore_config.pathTo = data_dir;
            } catch (PackageManager.NameNotFoundException e) {
                // pass
            }
            restore_config.uriFrom = mUri;
            restore_config.restoreWidgetsData = true;
            restore_config.restoreWallpaper = true;
            restore_config.restoreFonts = true;

            // ensure this directory at least is created with right permissions
            try {
                restore_config.context.createPackageContext(BackupRestore.this.getPackageName(), 0).getDir("files", Context.MODE_PRIVATE);
            } catch (PackageManager.NameNotFoundException e1) {
                return 0;
            }

            return BackupRestoreTool.restore(restore_config) ? 1 : 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            if (result == 1) {
                startActivity(new Intent(BackupRestore.this, Dashboard.class));
                System.exit(0);
            } else if (result == 2) {
                Intent intent = new Intent(BackupRestore.this, ApplyTemplate.class);
                intent.putExtra(ApplyTemplate.INTENT_EXTRA_URI, mUri);
                startActivity(intent);
                finish();
            } else {
                Flash.show(BackupRestore.this, getString(R.string.restore_error), Toast.LENGTH_LONG);
            }
        }
    }
}
