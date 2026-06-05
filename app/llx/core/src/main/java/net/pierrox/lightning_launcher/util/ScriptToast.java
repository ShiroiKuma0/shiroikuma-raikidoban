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

package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Script-facing drop-in for {@link android.widget.Toast}, bound into the JS script scope as the global
 * {@code Toast} (see {@code ScriptExecutor}). It <b>subclasses</b> {@link Toast} so existing scripts keep
 * working unchanged — {@code new Toast(ctx)}, the {@code LENGTH_SHORT}/{@code LENGTH_LONG} constants, and
 * every instance method ({@code show()}, {@code setGravity()}, …) are inherited as-is — but the static
 * {@code makeText()} factories are overridden to return a {@link Flash}-themed toast (black background,
 * yellow text, yellow border) instead of the grey system one, so script toasts match 「白い熊 雷起動盤 UI」.
 *
 * <p>Caveat: because the returned toast carries a custom themed view, calling {@code setText()} on it
 * throws (same as any {@code makeText}-with-custom-view toast); scripts that need to mutate the text
 * should call {@code makeText()} again. The common {@code Toast.makeText(ctx, msg, dur).show()} path is
 * fully supported.
 */
public class ScriptToast extends Toast {

    public ScriptToast(Context context) {
        super(context);
    }

    /** Themed replacement for {@link Toast#makeText(Context, CharSequence, int)}. */
    public static Toast makeText(Context context, CharSequence text, int duration) {
        return Flash.makeToast(context, text, duration);
    }

    /** Themed replacement for {@link Toast#makeText(Context, int, int)} (string resource). */
    public static Toast makeText(Context context, int resId, int duration) {
        return Flash.makeToast(context, context.getText(resId), duration);
    }
}
