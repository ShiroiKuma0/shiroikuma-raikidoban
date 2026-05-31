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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import net.pierrox.lightning_launcher.data.BackupRestoreTool;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.util.BackupFolder;
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
    private static final int DIALOG_BACKUP_IN_PROGRESS = 1;
    private static final int DIALOG_RESTORE_IN_PROGRESS = 2;
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

        mListView = findViewById(R.id.archives);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        mEmptyView = findViewById(R.id.empty);

        loadArchivesList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PICK_FOLDER, 0, R.string.backup_folder_select)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_MAIN);
            confirmRestore(intent.getData(), null);
        }
    }

    private void loadArchivesList() {
        mArchives.clear();
        mArchiveDir = BackupFolder.getDir(this);

        if (mArchiveDir == null) {
            mEmptyView.setText(R.string.backup_folder_none);
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            return;
        }

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
            String[] names = new String[mArchives.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = mArchives.get(i).getName();
            }
            mListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
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
        }
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
        startActivityForResult(intent, REQUEST_PICK_FOLDER);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;
        ProgressDialog progress;

        switch (id) {
            case DIALOG_BACKUP_IN_PROGRESS:
                progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.backup_in_progress));
                progress.setCancelable(false);
                return progress;

            case DIALOG_RESTORE_IN_PROGRESS:
                progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.restore_in_progress));
                progress.setCancelable(false);
                return progress;

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
                                Toast.makeText(BackupRestore.this, R.string.backup_folder_lost, Toast.LENGTH_LONG).show();
                            } else {
                                new BackupTask(doc).execute();
                            }
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return builder.create();

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
                    return builder.create();
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
                    return builder.create();
                }
                break;

            case DIALOG_SELECT_BACKUP_ACTION:
                if (mSelected != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_a);
                    builder.setItems(new String[]{getString(R.string.br_ob), getString(R.string.br_ot), getString(R.string.br_r), getString(R.string.br_s), getString(R.string.br_d)}, new DialogInterface.OnClickListener() {
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
                                        Toast.makeText(BackupRestore.this, R.string.backup_folder_lost, Toast.LENGTH_LONG).show();
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
                    return builder.create();
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
                    Toast.makeText(this, R.string.backup_folder_lost, Toast.LENGTH_LONG).show();
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
            private ProgressDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = new ProgressDialog(BackupRestore.this);
                mDialog.setMessage(getString(R.string.tmpl_e_m));
                mDialog.setCancelable(false);
                mDialog.show();
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
                Toast.makeText(BackupRestore.this, ok ? R.string.tmpl_e_d : R.string.tmpl_e_e, Toast.LENGTH_SHORT).show();
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
            private ProgressDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = new ProgressDialog(BackupRestore.this);
                mDialog.setMessage(getString(R.string.importing));
                mDialog.setCancelable(false);
                mDialog.show();
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
                    Toast.makeText(BackupRestore.this, R.string.import_e, Toast.LENGTH_SHORT).show();
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

        private BackupTask(DocumentFile target) {
            mTarget = target;
        }

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_BACKUP_IN_PROGRESS);
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
            removeDialog(DIALOG_BACKUP_IN_PROGRESS);

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
            Toast.makeText(BackupRestore.this, msg, Toast.LENGTH_LONG).show();

            loadArchivesList();
        }
    }

    private class RestoreTask extends AsyncTask<Void, Void, Integer> {
        private final Uri mUri;

        private RestoreTask(Uri uri) {
            mUri = uri;
        }

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_RESTORE_IN_PROGRESS);
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
            removeDialog(DIALOG_RESTORE_IN_PROGRESS);
            if (result == 1) {
                startActivity(new Intent(BackupRestore.this, Dashboard.class));
                System.exit(0);
            } else if (result == 2) {
                Intent intent = new Intent(BackupRestore.this, ApplyTemplate.class);
                intent.putExtra(ApplyTemplate.INTENT_EXTRA_URI, mUri);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(BackupRestore.this, R.string.restore_error, Toast.LENGTH_LONG).show();
            }
        }
    }
}
