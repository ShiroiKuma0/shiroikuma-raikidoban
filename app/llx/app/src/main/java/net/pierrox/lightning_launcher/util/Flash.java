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
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Toast;
import android.widget.TextView;

/**
 * A small toast / "info flash" in the app's black-yellow scheme: black background, yellow text, yellow
 * border. Use this instead of Toast.makeText() so every info flash matches the theme. The launcher is
 * the foreground app, so custom toast views still display on modern Android.
 */
public final class Flash {

    private static final int YELLOW = 0xFFFFFF00;
    private static final int BLACK = 0xFF000000;

    private Flash() {
    }

    public static void show(Context ctx, CharSequence text) {
        show(ctx, text, Toast.LENGTH_SHORT);
    }

    public static void show(Context ctx, int textResId) {
        show(ctx, ctx.getString(textResId), Toast.LENGTH_SHORT);
    }

    public static void show(Context ctx, CharSequence text, int duration) {
        float d = ctx.getResources().getDisplayMetrics().density;

        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(YELLOW);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setGravity(Gravity.CENTER);
        int padH = Math.round(18 * d);
        int padV = Math.round(12 * d);
        tv.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLACK);
        bg.setCornerRadius(8 * d);
        bg.setStroke(Math.round(2 * d), YELLOW);
        tv.setBackground(bg);

        Toast toast = new Toast(ctx);
        toast.setDuration(duration);
        toast.setView(tv);
        toast.show();
    }
}
