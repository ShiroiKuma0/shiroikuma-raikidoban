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

package net.pierrox.lightning_launcher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The category-ZIP export/import engine — the single backup of everything that is settable in
 * 白い熊 雷起動盤, and the 白い熊 family's shared backup shape (the same one Kōjiki / kxkb / 自由作業盤
 * write). Every caller — the Export/Import panel and the headless automation receiver
 * ({@link net.pierrox.lightning_launcher.util.StateExportReceiver}) — goes through
 * {@link #export} / {@link #importZip}; the logic lives here once.
 *
 * <p><b>ONE ZIP per export, always</b>, named {@code shiroikuma-raikidoban_<yyyy-MM-dd_HH-mm-ss>.zip}
 * (no version, no suffix) because 白い熊 keeps every sister app's backups in one directory, so they
 * must sort and read uniformly. Inside: {@code manifest.json} plus one entry (or entry tree) per
 * selected {@link Cat}. File-based categories store paths <i>relative to the engine base dir</i>
 * under their id, so the importer is a plain "strip the id, write under the base dir" copy.
 *
 * <p>The configured export directory is a persisted SAF tree kept in its OWN prefs file, which is
 * deliberately not part of any category — neither the directory nor the automation token ever
 * travels inside a backup.
 */
public final class RkbExport {

    public static final String FORMAT = "raikidoban-export";
    public static final int VERSION = 1;

    // --- backup file naming — the 白い熊 family convention (2026-07-25) ---
    // The legacy Backup/restore screen writes `.lla` archives, which this deliberately never matches.
    public static final String EXPORT_PREFIX = "shiroikuma-raikidoban_";

    private static final String MANIFEST_ENTRY = "manifest.json";
    private static final String UI_ENTRY = "ui.json";
    private static final String FOLD_ENTRY = "fold.json";
    private static final String FOLD_KEY = "foldGrid";

    // Device-local: the export directory lives here and is never exported.
    private static final String EXIMPORT_PREFS = "rkb_eximport";
    private static final String KEY_DIR_URI = "dir_uri";

    private static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

    private RkbExport() {
    }

    /**
     * A selectable part of the backup. {@code id} is also the entry (or entry-tree) name in the ZIP.
     *
     * <p>{@code defaultSelected} is this app <i>stating</i> whether the part starts ticked, rather
     * than every picker guessing — the in-app panel and the {@code LIST_CATEGORIES} fourth field
     * ({@code on|off}) both read it. Everything here is {@code on}: the rule for {@code off} is
     * "large, derived AND re-creatable", and nothing this app exports qualifies. In particular
     * {@code desktops.icons} and {@code desktops.wallpapers} are the two big ones and stay on — a
     * launcher restored without its icons and wallpapers is a broken desktop, and neither can be
     * rebuilt from the rest of the archive.
     */
    public enum Cat {
        UI("ui", R.string.rkb_eim_cat_ui, null),
        UI_FONTS("ui.fonts", R.string.rkb_eim_cat_ui_fonts, "ui"),
        SETTINGS("settings", R.string.rkb_eim_cat_settings, null),
        FOLD("fold", R.string.rkb_eim_cat_fold, null),
        DESKTOPS("desktops", R.string.rkb_eim_cat_desktops, null),
        DESKTOPS_ICONS("desktops.icons", R.string.rkb_eim_cat_desktops_icons, "desktops"),
        DESKTOPS_WALLPAPERS("desktops.wallpapers", R.string.rkb_eim_cat_desktops_wp, "desktops"),
        SCRIPTS("scripts", R.string.rkb_eim_cat_scripts, null),
        THEMES("themes", R.string.rkb_eim_cat_themes, null),
        VARIABLES("variables", R.string.rkb_eim_cat_variables, null);

        public final String id;
        public final int labelRes;
        /** The parent category's id for a sub-option, or null for a top-level category. */
        public final String parentId;
        /** Whether a freshly-opened picker starts with this part ticked. */
        public final boolean defaultSelected;

        Cat(String id, int labelRes, String parentId) {
            this(id, labelRes, parentId, true);
        }

        Cat(String id, int labelRes, String parentId, boolean defaultSelected) {
            this.id = id;
            this.labelRes = labelRes;
            this.parentId = parentId;
            this.defaultSelected = defaultSelected;
        }

        public static Cat byId(String id) {
            for (Cat c : values()) {
                if (c.id.equals(id)) {
                    return c;
                }
            }
            return null;
        }

        public static Set<Cat> all() {
            Set<Cat> set = new LinkedHashSet<>();
            for (Cat c : values()) {
                set.add(c);
            }
            return set;
        }

        /** The set a picker starts on — also what an EXPORT_STATE with no {@code items} extra means. */
        public static Set<Cat> defaults() {
            Set<Cat> set = new LinkedHashSet<>();
            for (Cat c : values()) {
                if (c.defaultSelected) {
                    set.add(c);
                }
            }
            return set;
        }
    }

    /** Called after each category is written, with real counts (never a percentage). */
    public interface Progress {
        void onProgress(int done, int total, String categoryLabel);
    }

    /**
     * Polled between ZIP entries so a running export can be stopped from outside (the
     * {@code CANCEL_EXPORT} action). Never interrupts a thread mid-write: the export unwinds at the
     * next entry boundary by throwing {@link CancelledException}, and the caller deletes the partial.
     */
    public interface Cancel {
        boolean isCancelled();
    }

    /** Thrown out of {@link #export} when the caller's {@link Cancel} goes up. */
    public static class CancelledException extends IOException {
        public CancelledException() {
            super("cancelled");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // file name + export directory
    // ---------------------------------------------------------------------------------------------

    /** The name of the ZIP to write now — identical for the UI panel and the automation receiver. */
    public static String exportFileName() {
        return EXPORT_PREFIX
                + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date())
                + ".zip";
    }

    /** True if this is one of our category backups (the shared directory also holds sister apps'). */
    public static boolean isExportFileName(String name) {
        return name != null && name.endsWith(".zip") && name.startsWith(EXPORT_PREFIX);
    }

    public static Uri exportDirUri(Context context) {
        String stored = context.getApplicationContext()
                .getSharedPreferences(EXIMPORT_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_DIR_URI, null);
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            return Uri.parse(stored);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setExportDirUri(Context context, Uri uri) {
        context.getApplicationContext()
                .getSharedPreferences(EXIMPORT_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DIR_URI, uri == null ? null : uri.toString())
                .apply();
    }

    /** The configured export directory, or null when unset / no longer reachable. */
    public static DocumentFile exportDir(Context context) {
        Uri uri = exportDirUri(context);
        if (uri == null) {
            return null;
        }
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
            return (dir != null && dir.isDirectory()) ? dir : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Best-effort real filesystem path of a SAF tree/document, so the panel and the automation reply
     * can show an absolute path rather than a bare folder label. Only the primary-storage tree can be
     * resolved this way; anything else returns null.
     */
    public static String absolutePathOf(DocumentFile dir, String fileName) {
        if (dir == null) {
            return null;
        }
        Uri treeUri = dir.getUri();
        if (!EXTERNAL_STORAGE_AUTHORITY.equals(treeUri.getAuthority())) {
            return null;
        }
        String docId;
        try {
            docId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            return null;
        }
        if (docId == null || !docId.startsWith("primary:")) {
            return null; // sd-card / usb volumes have no stable mount path
        }
        String rel = docId.substring("primary:".length());
        while (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        while (rel.endsWith("/")) {
            rel = rel.substring(0, rel.length() - 1);
        }
        String base = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = rel.isEmpty() ? base : base + "/" + rel;
        return fileName == null ? path : path + "/" + fileName;
    }

    /** Our newest backup in the configured directory, or null. */
    public static DocumentFile newestExport(Context context) {
        DocumentFile dir = exportDir(context);
        if (dir == null) {
            return null;
        }
        DocumentFile newest = null;
        try {
            for (DocumentFile f : dir.listFiles()) {
                if (f.isFile() && isExportFileName(f.getName())
                        && (newest == null || f.lastModified() > newest.lastModified())) {
                    newest = f;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return newest;
    }

    /** Our backups in the configured directory, newest first. */
    public static List<DocumentFile> listExports(Context context) {
        List<DocumentFile> out = new ArrayList<>();
        DocumentFile dir = exportDir(context);
        if (dir == null) {
            return out;
        }
        try {
            for (DocumentFile f : dir.listFiles()) {
                if (f.isFile() && isExportFileName(f.getName())) {
                    out.add(f);
                }
            }
        } catch (Exception e) {
            return out;
        }
        out.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    public static String humanSize(long bytes) {
        if (bytes >= (1L << 30)) {
            return String.format(Locale.ROOT, "%.2f GB", bytes / (double) (1L << 30));
        }
        if (bytes >= (1L << 20)) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (double) (1L << 20));
        }
        if (bytes >= (1L << 10)) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / (double) (1L << 10));
        }
        return bytes + " B";
    }

    // ---------------------------------------------------------------------------------------------
    // EXPORT
    // ---------------------------------------------------------------------------------------------

    /**
     * Write a ZIP of the selected categories to {@code out} (which the caller closes). Returns a
     * short human summary. This is the headless core: the panel and the automation receiver are two
     * thin callers, so no export logic is duplicated anywhere.
     */
    public static String export(Context context, Set<Cat> cats, OutputStream out, Progress progress)
            throws IOException {
        return export(context, cats, out, progress, null);
    }

    /**
     * As above, but pollable: {@code cancel} is checked at every entry boundary, and a raised flag
     * unwinds the whole export with a {@link CancelledException} instead of finishing the archive.
     */
    public static String export(Context context, Set<Cat> cats, OutputStream out, Progress progress,
                                Cancel cancel) throws IOException {
        LightningEngine engine = LLApp.get().getAppEngine();
        // Flush anything still only in memory, so the archive matches what the launcher shows.
        engine.saveData();
        File base = engine.getBaseDir();

        List<Cat> ordered = new ArrayList<>();
        for (Cat c : Cat.values()) {
            if (cats.contains(c)) {
                ordered.add(c);
            }
        }
        int total = ordered.size();

        ZipOutputStream zip = new ZipOutputStream(out);
        try {
            JSONObject manifest = new JSONObject();
            JSONArray ids = new JSONArray();
            for (Cat c : ordered) {
                ids.put(c.id);
            }
            try {
                manifest.put("format", FORMAT);
                manifest.put("version", VERSION);
                manifest.put("app", context.getPackageName());
                manifest.put("appVersion", Utils.getMyPackageVersion(context));
                manifest.put("createdTs", System.currentTimeMillis());
                manifest.put("categories", ids);
            } catch (JSONException e) {
                // pass — a manifest field is never worth failing an export over
            }
            writeEntry(zip, MANIFEST_ENTRY, manifest.toString().getBytes(StandardCharsets.UTF_8));

            int done = 0;
            for (Cat cat : ordered) {
                throwIfCancelled(cancel);
                exportCategory(context, engine, base, cat, zip, cancel);
                done++;
                if (progress != null) {
                    progress.onProgress(done, total, context.getString(cat.labelRes));
                }
            }
            throwIfCancelled(cancel);
            zip.finish();
            zip.flush();
        } finally {
            // Do not close `zip` here: that would close the caller's stream twice.
        }
        return total + " categories";
    }

    private static void exportCategory(Context context, LightningEngine engine, File base, Cat cat,
                                       ZipOutputStream zip, Cancel cancel) throws IOException {
        switch (cat) {
            case UI:
                writeEntry(zip, UI_ENTRY, dumpPrefs(uiPrefs(context)).getBytes(StandardCharsets.UTF_8));
                break;

            case UI_FONTS:
                zipTree(zip, cat.id, base, FileUtils.getFontsDir(base), cancel);
                break;

            case SETTINGS:
                zipFile(zip, cat.id, base, FileUtils.getGlobalConfigFile(base), cancel);
                zipFile(zip, cat.id, base, FileUtils.getSystemConfigFile(context), cancel);
                zipFile(zip, cat.id, base, FileUtils.getStateFile(base), cancel);
                zipFile(zip, cat.id, base, FileUtils.getStatisticsFile(base), cancel);
                break;

            case FOLD: {
                JSONObject o = new JSONObject();
                try {
                    String grid = engine.getGlobalConfig().foldGrid;
                    o.put(FOLD_KEY, grid == null ? "" : grid);
                } catch (JSONException e) {
                    // pass
                }
                writeEntry(zip, FOLD_ENTRY, o.toString().getBytes(StandardCharsets.UTF_8));
                break;
            }

            case DESKTOPS:
                zipFile(zip, cat.id, base, FileUtils.getManifestFile(base), cancel);
                zipFile(zip, cat.id, base, FileUtils.getPinnedAppShortcutsFile(base), cancel);
                for (File page : pageDirs(base)) {
                    zipFile(zip, cat.id, base, new File(page, "items"), cancel);
                    zipFile(zip, cat.id, base, new File(page, "conf"), cancel);
                    zipFile(zip, cat.id, base, new File(page, "i"), cancel);
                }
                break;

            case DESKTOPS_ICONS:
                for (File page : pageDirs(base)) {
                    zipTree(zip, cat.id, base, new File(page, "icon"), cancel);
                }
                break;

            case DESKTOPS_WALLPAPERS:
                for (File page : pageDirs(base)) {
                    zipFile(zip, cat.id, base, new File(page, "wp"), cancel);
                }
                break;

            case SCRIPTS:
                zipTree(zip, cat.id, base, engine.getScriptManager().getScriptsDir(), cancel);
                break;

            case THEMES:
                zipTree(zip, cat.id, base, FileUtils.getStylesDir(base), cancel);
                break;

            case VARIABLES:
                zipFile(zip, cat.id, base, FileUtils.getVariablesFile(base), cancel);
                break;
        }
    }

    private static void throwIfCancelled(Cancel cancel) throws CancelledException {
        if (cancel != null && cancel.isCancelled()) {
            throw new CancelledException();
        }
    }

    private static File[] pageDirs(File base) {
        File[] pages = FileUtils.getPagesDir(base).listFiles();
        return pages == null ? new File[0] : pages;
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    /** Store {@code file} as {@code <catId>/<path relative to base>}. Missing files are skipped. */
    private static void zipFile(ZipOutputStream zip, String catId, File base, File file, Cancel cancel)
            throws IOException {
        throwIfCancelled(cancel); // every entry is a cancel boundary — never mid-write
        if (file == null || !file.isFile()) {
            return;
        }
        String rel = relativeTo(base, file);
        if (rel == null) {
            return;
        }
        zip.putNextEntry(new ZipEntry(catId + "/" + rel));
        InputStream is = new FileInputStream(file);
        try {
            FileUtils.copyStream(is, zip);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // pass
            }
        }
        zip.closeEntry();
    }

    private static void zipTree(ZipOutputStream zip, String catId, File base, File dir, Cancel cancel)
            throws IOException {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) {
            return; // the directory may simply not exist yet (fonts, themes, scripts)
        }
        for (File f : files) {
            if (f.isDirectory()) {
                zipTree(zip, catId, base, f, cancel);
            } else {
                zipFile(zip, catId, base, f, cancel);
            }
        }
    }

    private static String relativeTo(File base, File file) {
        String b = base.getAbsolutePath();
        String f = file.getAbsolutePath();
        if (!f.startsWith(b + "/")) {
            return null;
        }
        return f.substring(b.length() + 1);
    }

    // ---------------------------------------------------------------------------------------------
    // IMPORT
    // ---------------------------------------------------------------------------------------------

    /** The category ids the ZIP's manifest declares (empty when it is not one of our archives). */
    public static List<String> categoriesIn(byte[] data) {
        List<String> out = new ArrayList<>();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (!MANIFEST_ENTRY.equals(ze.getName())) {
                    continue;
                }
                JSONObject manifest = new JSONObject(new String(readEntry(zis), StandardCharsets.UTF_8));
                JSONArray ids = manifest.optJSONArray("categories");
                if (ids != null) {
                    for (int i = 0; i < ids.length(); i++) {
                        out.add(ids.optString(i));
                    }
                }
                break;
            }
        } catch (IOException | JSONException e) {
            // not one of our archives
        } finally {
            try {
                zis.close();
            } catch (IOException e) {
                // pass
            }
        }
        return out;
    }

    /**
     * Restore the selected categories the ZIP contains — merged, never wiped: entries present are
     * written, everything else is left exactly as it is, and categories absent from the archive are
     * skipped. Returns one summary line per restored category.
     *
     * <p>A restore replaces files the running engine also holds in memory, so the caller must offer a
     * restart (the Export/Import panel does). To make sure nothing stale is written back over the
     * restored files in the meantime, pending data is flushed and the page/script caches are dropped
     * before anything is written.
     */
    public static String importZip(Context context, byte[] data, Set<Cat> cats) throws IOException {
        LightningEngine engine = LLApp.get().getAppEngine();
        File base = engine.getBaseDir();

        List<String> present = categoriesIn(data);
        if (present.isEmpty()) {
            throw new IOException(context.getString(R.string.rkb_eim_import_none));
        }

        boolean touchesFiles = false;
        for (Cat c : cats) {
            if (c != Cat.UI && c != Cat.FOLD && present.contains(c.id)) {
                touchesFiles = true;
                break;
            }
        }
        if (touchesFiles) {
            engine.saveData();
            engine.getPageManager().clear();
            engine.getScriptManager().clear();
        }

        int[] counts = new int[Cat.values().length];
        String uiJson = null;
        String foldJson = null;

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String name = ze.getName();
                if (ze.isDirectory() || MANIFEST_ENTRY.equals(name) || name.contains("..")) {
                    continue;
                }
                if (UI_ENTRY.equals(name)) {
                    if (cats.contains(Cat.UI)) {
                        uiJson = new String(readEntry(zis), StandardCharsets.UTF_8);
                    }
                    continue;
                }
                if (FOLD_ENTRY.equals(name)) {
                    if (cats.contains(Cat.FOLD)) {
                        foldJson = new String(readEntry(zis), StandardCharsets.UTF_8);
                    }
                    continue;
                }
                Cat cat = fileCatOf(name);
                if (cat == null || !cats.contains(cat)) {
                    continue;
                }
                String rel = name.substring(cat.id.length() + 1);
                File outFile = new File(base, rel);
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(outFile);
                try {
                    FileUtils.copyStream(zis, fos);
                } finally {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        // pass
                    }
                }
                counts[cat.ordinal()]++;
            }
        } finally {
            try {
                zis.close();
            } catch (IOException e) {
                // pass
            }
        }

        if (uiJson != null) {
            counts[Cat.UI.ordinal()] = mergePrefs(uiPrefs(context), uiJson);
        }
        if (foldJson != null) {
            counts[Cat.FOLD.ordinal()] = patchFoldGrid(base, foldJson) ? 1 : 0;
        }
        if (touchesFiles || foldJson != null) {
            // Bring the in-memory global config back in line with what is now on disk. Everything
            // else (pages, scripts) is re-read on the restart the caller offers.
            engine.reloadGlobalConfig();
        }

        StringBuilder summary = new StringBuilder();
        for (Cat c : Cat.values()) {
            if (!cats.contains(c) || !present.contains(c.id)) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append('\n');
            }
            summary.append(context.getString(c.labelRes)).append(": ").append(counts[c.ordinal()]);
        }
        if (summary.length() == 0) {
            summary.append(context.getString(R.string.rkb_eim_import_nothing));
        }
        return summary.toString();
    }

    /** The file-based category owning a ZIP entry, or null. Ids never prefix each other ambiguously. */
    private static Cat fileCatOf(String entryName) {
        for (Cat c : Cat.values()) {
            if (c != Cat.UI && c != Cat.FOLD && entryName.startsWith(c.id + "/")) {
                return c;
            }
        }
        return null;
    }

    private static byte[] readEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileUtils.copyStream(zis, bos);
        return bos.toByteArray();
    }

    /** Write the archived fold matrix into the on-disk global config (the engine reloads it after). */
    private static boolean patchFoldGrid(File base, String json) {
        try {
            String grid = new JSONObject(json).optString(FOLD_KEY, "");
            File configFile = FileUtils.getGlobalConfigFile(base);
            if (!configFile.isFile()) {
                return false;
            }
            FileInputStream fis = new FileInputStream(configFile);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                FileUtils.copyStream(fis, bos);
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    // pass
                }
            }
            JSONObject config = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            if (grid.isEmpty()) {
                config.remove(FOLD_KEY);
            } else {
                config.put(FOLD_KEY, grid);
            }
            FileOutputStream fos = new FileOutputStream(configFile);
            try {
                fos.write(config.toString().getBytes(StandardCharsets.UTF_8));
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    // pass
                }
            }
            return true;
        } catch (IOException | JSONException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // prefs (type-tagged, like every sister app)
    // ---------------------------------------------------------------------------------------------

    private static SharedPreferences uiPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(UiConfig.PREFS, Context.MODE_PRIVATE);
    }

    private static String dumpPrefs(SharedPreferences sp) {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            JSONObject entry = new JSONObject();
            try {
                if (v instanceof Boolean) {
                    entry.put("t", "b").put("v", v);
                } else if (v instanceof Integer) {
                    entry.put("t", "i").put("v", v);
                } else if (v instanceof Long) {
                    entry.put("t", "l").put("v", v);
                } else if (v instanceof Float) {
                    entry.put("t", "f").put("v", ((Float) v).doubleValue());
                } else if (v instanceof String) {
                    entry.put("t", "s").put("v", v);
                } else {
                    continue; // string sets are unused by this app's config
                }
                obj.put(e.getKey(), entry);
            } catch (JSONException ex) {
                // pass
            }
        }
        return obj.toString();
    }

    /** Merge a dump back in, per key (never a wipe). Returns the number of values written. */
    private static int mergePrefs(SharedPreferences sp, String json) {
        int n = 0;
        try {
            JSONObject obj = new JSONObject(json);
            SharedPreferences.Editor editor = sp.edit();
            for (java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject entry = obj.optJSONObject(key);
                if (entry == null) {
                    continue;
                }
                String type = entry.optString("t");
                switch (type) {
                    case "b":
                        editor.putBoolean(key, entry.optBoolean("v"));
                        break;
                    case "i":
                        editor.putInt(key, entry.optInt("v"));
                        break;
                    case "l":
                        editor.putLong(key, entry.optLong("v"));
                        break;
                    case "f":
                        editor.putFloat(key, (float) entry.optDouble("v"));
                        break;
                    case "s":
                        editor.putString(key, entry.optString("v"));
                        break;
                    default:
                        continue;
                }
                n++;
            }
            // commit(): the import is normally followed by a process restart.
            editor.commit();
        } catch (JSONException e) {
            return n;
        }
        return n;
    }
}
