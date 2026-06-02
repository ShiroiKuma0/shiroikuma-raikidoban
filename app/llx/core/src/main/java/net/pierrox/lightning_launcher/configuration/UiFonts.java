package net.pierrox.lightning_launcher.configuration;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.data.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Font support for 「白い熊 雷起動盤 UI」: the app-wide imported-fonts directory, a cached
 * {@link Typeface} loader, weight-aware typeface construction, and the option list for the picker.
 * Built-in option "" = system default, {@link #MONOSPACE} = monospace; anything else is a file in the
 * fonts dir. Mirrors the sister repos' Fonts.kt.
 */
public final class UiFonts {

    public static final String MONOSPACE = "@monospace";

    private static final int SEMIBOLD_WEIGHT = 600;
    private static final int BOLD_WEIGHT = 700;
    private static final String[] FONT_EXTENSIONS = {"ttf", "otf"};

    private static final HashMap<String, Typeface> sCache = new HashMap<>();

    private UiFonts() {
    }

    /** A selectable font weight; value 0 keeps the family's own default weight. */
    public enum Weight {
        DEFAULT(0, R.string.font_weight_default),
        THIN(100, R.string.font_weight_thin),
        LIGHT(300, R.string.font_weight_light),
        REGULAR(400, R.string.font_weight_regular),
        MEDIUM(500, R.string.font_weight_medium),
        SEMIBOLD(600, R.string.font_weight_semibold),
        BOLD(700, R.string.font_weight_bold),
        BLACK(900, R.string.font_weight_black);

        public final int value;
        public final int labelRes;

        Weight(int value, int labelRes) {
            this.value = value;
            this.labelRes = labelRes;
        }

        public static Weight fromValue(int value) {
            for (Weight w : values()) {
                if (w.value == value) {
                    return w;
                }
            }
            return DEFAULT;
        }
    }

    /** One pickable font; an empty fileName means "system / global default". */
    public static final class Option {
        public final String displayName;
        public final String fileName;

        public Option(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }
    }

    public static File getFontsDir() {
        File dir = FileUtils.getFontsDir(LLApp.get().getAppEngine().getBaseDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** Typeface for a stored family value ("" = system default, sentinel = monospace, else file), cached. */
    public static Typeface fontTypeface(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return Typeface.DEFAULT;
        }
        if (fileName.equals(MONOSPACE)) {
            return Typeface.MONOSPACE;
        }
        Typeface t = sCache.get(fileName);
        if (t == null) {
            try {
                t = Typeface.createFromFile(new File(getFontsDir(), fileName));
            } catch (Exception e) {
                t = Typeface.DEFAULT;
            }
            sCache.put(fileName, t);
        }
        return t;
    }

    /** Combine a family + weight with a base text style. */
    public static Typeface typeface(String family, int weight, int baseStyle) {
        Typeface base = fontTypeface(family);
        if (weight <= 0) {
            return Typeface.create(base, baseStyle);
        }
        boolean italic = baseStyle == Typeface.ITALIC || baseStyle == Typeface.BOLD_ITALIC;
        boolean bold = baseStyle == Typeface.BOLD || baseStyle == Typeface.BOLD_ITALIC;
        int effective = bold ? Math.max(weight, BOLD_WEIGHT) : weight;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Typeface.create(base, effective, italic);
        }
        return Typeface.create(base, effective >= SEMIBOLD_WEIGHT ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** Built-in families + every font the user has imported (shared across all elements). */
    public static List<Option> availableFontOptions(Context ctx) {
        List<Option> options = new ArrayList<>();
        options.add(new Option(ctx.getString(R.string.font_system_default), ""));
        options.add(new Option(ctx.getString(R.string.font_monospace), MONOSPACE));

        File[] files = getFontsDir().listFiles();
        if (files != null) {
            List<File> fonts = new ArrayList<>();
            for (File f : files) {
                if (f.isFile() && hasFontExtension(f.getName())) {
                    fonts.add(f);
                }
            }
            Collections.sort(fonts, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().toLowerCase(Locale.ROOT).compareTo(b.getName().toLowerCase(Locale.ROOT));
                }
            });
            for (File f : fonts) {
                options.add(new Option(nameWithoutExtension(f.getName()), f.getName()));
            }
        }
        return options;
    }

    public static String displayName(Context ctx, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ctx.getString(R.string.font_system_default);
        }
        if (fileName.equals(MONOSPACE)) {
            return ctx.getString(R.string.font_monospace);
        }
        return nameWithoutExtension(fileName);
    }

    /** Copy a picked font file into the app fonts dir; returns its filename, or null on failure. */
    public static String importFont(Context ctx, Uri uri) {
        String name = fontFileName(ctx, uri);
        if (name == null || !hasFontExtension(name)) {
            return null;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            File dest = new File(getFontsDir(), name);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
        sCache.remove(name);
        return name;
    }

    private static String fontFileName(Context ctx, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            // fall through to the path-based fallback
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String last = uri.getLastPathSegment();
        if (last != null) {
            int slash = last.lastIndexOf('/');
            return slash >= 0 ? last.substring(slash + 1) : last;
        }
        return null;
    }

    private static boolean hasFontExtension(String name) {
        String ext = extension(name).toLowerCase(Locale.ROOT);
        for (String e : FONT_EXTENSIONS) {
            if (e.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    private static String nameWithoutExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
