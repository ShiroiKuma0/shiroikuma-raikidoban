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

package net.pierrox.lightning_launcher.configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The fold-aware desktop matrix: rows = fold states (rowKey 1/2/3, smallest→largest fold), each with a
 * physical fold-width signature in pixels (the smaller dimension of the real display, stable per fold)
 * and a sparse map of horizontal offset → pageId
 * (offset 0 = the row's home). Derived from desktop names like "1-2", "1-1", "1", "2+1", "3" (leading
 * digit = rowKey, suffix -N/+N = offset). Stored as a JSON string in GlobalConfig.foldGrid so it rides
 * along in backups; this is NOT a JsonLoader subclass (JsonLoader can't serialize the nested maps).
 */
public class FoldGrid {

    public static final int SCHEMA_VERSION = 1;

    public static class Row {
        public int rowKey;
        public Integer widthPx;                                  // null until learned/captured
        public final TreeMap<Integer, Integer> cells = new TreeMap<>(); // offset -> pageId
    }

    public final List<Row> rows = new ArrayList<>(); // kept sorted by rowKey ascending
    public boolean migrated = false;                  // set once migration has run (idempotency marker)

    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\d)([+-]\\d+)?$");

    private static final Comparator<Row> ROW_ORDER = new Comparator<Row>() {
        @Override
        public int compare(Row a, Row b) {
            return Integer.compare(a.rowKey, b.rowKey);
        }
    };

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public Row row(int rowKey) {
        for (Row r : rows) {
            if (r.rowKey == rowKey) {
                return r;
            }
        }
        return null;
    }

    private Row rowOrCreate(int rowKey) {
        Row r = row(rowKey);
        if (r == null) {
            r = new Row();
            r.rowKey = rowKey;
            rows.add(r);
            Collections.sort(rows, ROW_ORDER);
        }
        return r;
    }

    /** The pageId at (rowKey, offset), or null if there is no such cell. */
    public Integer cell(int rowKey, int offset) {
        Row r = row(rowKey);
        return r == null ? null : r.cells.get(offset);
    }

    /** Locate a page in the grid: returns {rowKey, offset}, or null if not present. */
    public int[] findCell(int pageId) {
        for (Row r : rows) {
            for (Map.Entry<Integer, Integer> e : r.cells.entrySet()) {
                if (e.getValue() == pageId) {
                    return new int[]{r.rowKey, e.getKey()};
                }
            }
        }
        return null;
    }

    /** Ascending list of the offsets present in a row (empty if the row is unknown). */
    public List<Integer> offsetsSorted(int rowKey) {
        Row r = row(rowKey);
        List<Integer> out = new ArrayList<>();
        if (r != null) {
            out.addAll(r.cells.keySet()); // TreeMap keys are already ascending
        }
        return out;
    }

    public void setWidth(int rowKey, int widthPx) {
        rowOrCreate(rowKey).widthPx = widthPx;
    }

    public void setCell(int rowKey, int offset, int pageId) {
        rowOrCreate(rowKey).cells.put(offset, pageId);
    }

    public void removeCell(int rowKey, int offset) {
        Row r = row(rowKey);
        if (r != null) {
            r.cells.remove(offset);
        }
    }

    /**
     * The rowKey whose learned widthPx is nearest the given width, among rows that have a signature.
     * Returns -1 when no row has been learned yet (caller should treat that as "don't move").
     */
    public int rowForWidth(int widthPx) {
        int best = -1;
        int bestDelta = Integer.MAX_VALUE;
        for (Row r : rows) {
            if (r.widthPx != null) {
                int d = Math.abs(widthPx - r.widthPx);
                if (d < bestDelta) {
                    bestDelta = d;
                    best = r.rowKey;
                }
            }
        }
        return best;
    }

    /**
     * Replace the cells from the parallel screensNames / screensOrder arrays (name → rowKey+offset →
     * pageId). Learned widthPx values are preserved per rowKey. Names that don't match are skipped.
     */
    public void deriveFromNames(String[] names, int[] order) {
        if (names == null || order == null) {
            return;
        }
        for (Row r : rows) {
            r.cells.clear();
        }
        int n = Math.min(names.length, order.length);
        for (int i = 0; i < n; i++) {
            String name = names[i];
            if (name == null) {
                continue;
            }
            Matcher m = NAME_PATTERN.matcher(name.trim());
            if (!m.matches()) {
                continue;
            }
            int rowKey = Integer.parseInt(m.group(1));
            int offset = 0;
            String suffix = m.group(2);
            if (suffix != null) {
                // "+N" -> Integer.parseInt drops the leading '+', "-N" parses negative
                offset = Integer.parseInt(suffix.charAt(0) == '+' ? suffix.substring(1) : suffix);
            }
            rowOrCreate(rowKey).cells.put(offset, order[i]);
        }
        // drop rows that ended up with no cells
        for (Iterator<Row> it = rows.iterator(); it.hasNext(); ) {
            if (it.next().cells.isEmpty()) {
                it.remove();
            }
        }
        Collections.sort(rows, ROW_ORDER);
    }

    public static FoldGrid parse(String json) {
        FoldGrid g = new FoldGrid();
        if (json == null || json.length() == 0) {
            return g;
        }
        try {
            JSONObject o = new JSONObject(json);
            g.migrated = o.optBoolean("migrated", false);
            JSONArray rowsArr = o.optJSONArray("rows");
            if (rowsArr != null) {
                for (int i = 0; i < rowsArr.length(); i++) {
                    JSONObject ro = rowsArr.getJSONObject(i);
                    Row r = g.rowOrCreate(ro.getInt("rowKey"));
                    if (!ro.isNull("widthPx")) {
                        r.widthPx = ro.getInt("widthPx");
                    }
                    JSONObject cellsObj = ro.optJSONObject("cells");
                    if (cellsObj != null) {
                        for (Iterator<String> it = cellsObj.keys(); it.hasNext(); ) {
                            String key = it.next();
                            r.cells.put(Integer.parseInt(key), cellsObj.getInt(key));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            // malformed -> behave as empty
        }
        Collections.sort(g.rows, ROW_ORDER);
        return g;
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("v", SCHEMA_VERSION);
            o.put("migrated", migrated);
            JSONArray rowsArr = new JSONArray();
            for (Row r : rows) {
                JSONObject ro = new JSONObject();
                ro.put("rowKey", r.rowKey);
                ro.put("widthPx", r.widthPx == null ? JSONObject.NULL : r.widthPx.intValue());
                JSONObject cellsObj = new JSONObject();
                for (Map.Entry<Integer, Integer> e : r.cells.entrySet()) {
                    cellsObj.put(String.valueOf(e.getKey()), e.getValue().intValue());
                }
                ro.put("cells", cellsObj);
                rowsArr.put(ro);
            }
            o.put("rows", rowsArr);
            return o.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    /** Compact human-readable form for diagnostics, e.g. "r1(w=?)[-2:9,-1:7,0:4] r2(w=320)[...]". */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        for (Row r : rows) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append('r').append(r.rowKey).append("(w=").append(r.widthPx == null ? "?" : r.widthPx).append(")[");
            boolean first = true;
            for (Map.Entry<Integer, Integer> e : r.cells.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(e.getKey()).append(':').append(e.getValue());
                first = false;
            }
            sb.append(']');
        }
        return sb.toString();
    }
}
