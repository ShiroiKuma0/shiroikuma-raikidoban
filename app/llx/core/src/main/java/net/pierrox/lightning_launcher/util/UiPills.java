package net.pierrox.lightning_launcher.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

/**
 * 白い熊: the black-yellow shapes — the PILL every action wears and the outlined SWITCH every on/off
 * setting wears. Nothing here is ever filled.
 * <ul>
 * <li>The main action of a block is a pill with a {@link #MAIN_DP} frame and bold text; every other
 * action a pill with a {@link #REGULAR_DP} frame and normal text. A destructive one lends the
 * {@link UiSlot#DELETE} red to frame and label. Disabled = 38 %, ripple = 30 % of the pill's colour.</li>
 * <li>A switch is a 2 dp round outline with a solid dot inside: right and accent when on, left and the
 * opaque {@link UiSlot#SWITCH_OFF} when off.</li>
 * </ul>
 * Measures are the ones settled in the sister apps (白い熊 kxkb {@code Pills.kt}, FairEmail
 * {@code Pills.java}); copy, do not re-derive.
 */
public final class UiPills {

    public static final float MAIN_DP = 3f;
    public static final float REGULAR_DP = 1.25f;
    private static final float ROUND_DP = 100f;

    private UiPills() {
    }

    public static int dp(Context context, float v) {
        return (int) (v * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // --- derived colours ---

    /** The clear divider: an opaque darker accent, 70 % of the way from the background to the accent. */
    public static int divider() {
        return blend(UiTheme.color(UiSlot.BACKGROUND), UiTheme.color(UiSlot.ACCENT), 0.7f);
    }

    public static int disabled(int color) {
        return UiTheme.adjustAlpha(color, 0.38f);
    }

    public static int ripple(int color) {
        return UiTheme.adjustAlpha(color, 0.30f);
    }

    public static int blend(int from, int to, float t) {
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }

    /** {@code color}, dropping to 38 % while the view is disabled. */
    public static ColorStateList states(int color) {
        return new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[0]},
                new int[]{disabled(color), color});
    }

    // --- the pill ---

    /** An unfilled, fully round pill frame in {@code color} with its ripple. */
    public static Drawable background(Context context, int color, boolean main) {
        return background(context, color, main ? MAIN_DP : REGULAR_DP, ROUND_DP, Color.TRANSPARENT);
    }

    public static Drawable background(Context context, int color, float strokeDp, float radiusDp, int fill) {
        float radius = dp(context, radiusDp);
        GradientDrawable frame = new GradientDrawable();
        frame.setCornerRadius(radius);
        frame.setColor(fill);
        if (strokeDp > 0) {
            frame.setStroke(Math.max(1, dp(context, strokeDp)), states(color));
        }
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(radius);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(ripple(color)), frame, mask);
    }

    /**
     * A push button of the app (the Backup / restore screen, a button inside a dialog's own layout…).
     * It is a pill by rule; the 「ボタン」 group of the UI page can still override the shape — a border
     * width or a corner radius set there wins over the pill's — and supplies colours and font.
     *
     * @param destructive lends the delete red to frame and label
     */
    public static void button(TextView b, boolean main, boolean destructive) {
        if (b == null) {
            return;
        }
        Context context = b.getContext();
        UiConfig config = UiConfig.get();
        String key = UiSlot.BUTTON_BORDER.key;
        float stroke = config.hasBorderWidth(key) ? config.getBorderWidth(key) : (main ? MAIN_DP : REGULAR_DP);
        float radius = config.hasCornerRadius(key) ? config.getCornerRadius(key) : ROUND_DP;
        int fill = config.hasOverride(UiSlot.BUTTON_BG.key) ? UiTheme.color(UiSlot.BUTTON_BG) : Color.TRANSPARENT;
        int frame = destructive ? UiTheme.color(UiSlot.DELETE) : UiTheme.color(UiSlot.BUTTON_BORDER);
        int text = destructive ? UiTheme.color(UiSlot.DELETE) : UiTheme.color(UiSlot.BUTTON_TEXT);

        b.setBackground(background(context, frame, stroke, radius, fill));
        b.setTextColor(states(text));
        b.setTypeface(UiTheme.typeface(UiSlot.BUTTON_TEXT, main ? Typeface.BOLD : Typeface.NORMAL));
        int sp = UiTheme.size(UiSlot.BUTTON_TEXT);
        if (sp > 0) {
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(context, 16), dp(context, 9), dp(context, 16), dp(context, 9));
        b.setTag(R.id.llui_styled, Boolean.TRUE);
    }

    /**
     * A button of a dialog's own button bar. They touch each other, so the pill sits in a small inset.
     * {@code main} is the positive button; {@code color} the {@link UiSlot#DIALOG_BUTTON} colour, or the
     * delete red on a dialog that deletes.
     */
    public static void dialogButton(Button b, boolean main, int color) {
        if (b == null) {
            return;
        }
        Context context = b.getContext();
        b.setBackground(new InsetDrawable(background(context, color, main),
                dp(context, 4), dp(context, 5), dp(context, 4), dp(context, 5)));
        b.setTextColor(states(color));
        b.setTypeface(UiTheme.typeface(UiSlot.DIALOG_BUTTON, main ? Typeface.BOLD : Typeface.NORMAL));
        int sp = UiTheme.size(UiSlot.DIALOG_BUTTON);
        if (sp > 0) {
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setPadding(dp(context, 22), b.getPaddingTop(), dp(context, 22), b.getPaddingBottom());
        b.setMinWidth(dp(context, 80));
        b.setMinimumWidth(dp(context, 80));
        b.setTag(R.id.llui_styled, Boolean.TRUE);
    }

    /** A small outlined chip (the UI page's section index). */
    public static TextView chip(Context context, CharSequence text, int color) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(12);
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(context, 12), dp(context, 5), dp(context, 12), dp(context, 5));
        t.setBackground(background(context, color, false));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /**
     * A pill with its icon inside at the left, the label after it and — when it opens something — a ›
     * at the right end. The whole pill is the touch target.
     *
     * @param color     frame, label and ›
     * @param fullWidth the label takes the remaining width (menu and list rows); otherwise the pill hugs
     *                  its label (settings pages)
     */
    public static LinearLayout iconPill(Context context, CharSequence text, Drawable icon, int color,
                                        boolean main, boolean opens, boolean fullWidth) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(background(context, color, main));
        pill.setClickable(true);
        pill.setFocusable(true);
        pill.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8));

        if (icon != null) {
            View iconView = new View(context);
            iconView.setBackground(icon);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(context, 20), dp(context, 20));
            ilp.setMarginEnd(dp(context, 10));
            pill.addView(iconView, ilp);
        }

        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(15);
        label.setTypeface(UiTheme.typeface(UiSlot.TEXT, main ? Typeface.BOLD : Typeface.NORMAL));
        label.setTag(R.id.llui_styled, Boolean.TRUE);
        pill.addView(label, fullWidth
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (opens) {
            TextView chevron = new TextView(context);
            chevron.setText("›");
            chevron.setTextColor(color);
            chevron.setTextSize(18);
            chevron.setIncludeFontPadding(false);
            chevron.setPadding(dp(context, 10), 0, 0, 0);
            pill.addView(chevron);
        }
        return pill;
    }

    // --- the switch ---

    /**
     * Give a checkbox the look of the outlined switch: its tick box goes, and the switch is drawn at
     * the end of its label (or alone, when it has none). The view stays the {@link CheckBox} the code
     * around it knows, so every {@code isChecked()} / listener keeps working.
     *
     * @param compact the 44 × 24 dp switch of menus and dialogs; otherwise the 52 × 28 dp page one
     */
    public static void applySwitch(CompoundButton box, boolean compact) {
        if (box == null || box.getTag(R.id.llui_styled) != null) {
            return;
        }
        Context context = box.getContext();
        SwitchDrawable sw = new SwitchDrawable(context, compact);
        box.setButtonDrawable(null);
        box.setBackground(null);
        box.setCompoundDrawablesRelative(null, null, sw, null);
        box.setCompoundDrawablePadding(dp(context, 10));
        box.setMinHeight(0);
        box.setMinimumHeight(0);
        box.setMinWidth(0);
        box.setMinimumWidth(0);
        box.setTag(R.id.llui_styled, Boolean.TRUE);
        box.jumpDrawablesToCurrentState();
    }

    /** The outlined switch as a stateful drawable: it follows {@code state_checked} of its view. */
    public static final class SwitchDrawable extends Drawable {
        private final int width;
        private final int height;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean checked;
        private boolean enabled = true;
        private boolean seeded;
        private float position;
        private ValueAnimator animator;

        public SwitchDrawable(Context context, boolean compact) {
            width = dp(context, compact ? 44 : 52);
            height = dp(context, compact ? 24 : 28);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1, dp(context, 2)));
            fill.setStyle(Paint.Style.FILL);
            setBounds(0, 0, width, height);
        }

        @Override
        public int getIntrinsicWidth() {
            return width;
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean nowChecked = false;
            boolean nowEnabled = false;
            for (int s : state) {
                if (s == android.R.attr.state_checked) {
                    nowChecked = true;
                } else if (s == android.R.attr.state_enabled) {
                    nowEnabled = true;
                }
            }
            boolean changed = nowChecked != checked || nowEnabled != enabled || !seeded;
            boolean travel = seeded && nowChecked != checked;
            checked = nowChecked;
            enabled = nowEnabled;
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            float target = checked ? 1f : 0f;
            if (travel && isVisible()) {
                animator = ValueAnimator.ofFloat(position, target);
                animator.setDuration(180);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(a -> {
                    position = (float) a.getAnimatedValue();
                    invalidateSelf();
                });
                animator.start();
            } else {
                position = target;
            }
            seeded = true;
            if (changed) {
                invalidateSelf();
            }
            return changed;
        }

        @Override
        public void jumpToCurrentState() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            position = checked ? 1f : 0f;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            int color = UiTheme.color(checked ? UiSlot.ACCENT : UiSlot.SWITCH_OFF);
            if (!enabled) {
                color = disabled(color);
            }
            float inset = stroke.getStrokeWidth() / 2f;
            rect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            stroke.setColor(color);
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, stroke);
            fill.setColor(color);
            float from = b.left + b.height() / 2f;
            float to = b.right - b.height() / 2f;
            // the dot's diameter is 46 % of the track height
            canvas.drawCircle(from + (to - from) * position, b.exactCenterY(), b.height() * 0.23f, fill);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    // --- whole trees ---

    /**
     * Give a dialog's or a screen's own layout the black-yellow shapes: every checkbox becomes the
     * outlined switch, every radio button takes the accent, and every button with a text label becomes
     * a regular pill. Left alone: a view already styled, and an icon-font or one-glyph tool button —
     * those are not text buttons (the edit bars, the +/- of a slider).
     */
    public static void styleTree(View root) {
        if (root == null) {
            return;
        }
        if (root instanceof RadioButton) {
            ((RadioButton) root).setButtonTintList(ColorStateList.valueOf(UiTheme.color(UiSlot.ACCENT)));
        } else if (root instanceof CheckBox) {
            applySwitch((CheckBox) root, true);
        } else if (root instanceof Button && !(root instanceof CompoundButton)) {
            Button b = (Button) root;
            if (b.getTag(R.id.llui_styled) == null && !isGlyph(b)) {
                button(b, false, false);
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                styleTree(group.getChildAt(i));
            }
        }
    }

    /** True for a tool button that shows a glyph rather than a word. */
    public static boolean isGlyph(TextView b) {
        CharSequence text = b.getText();
        if (text == null || text.length() <= 1) {
            return true;
        }
        LLApp app = LLApp.get();
        return app != null && b.getTypeface() == app.getIconsTypeface();
    }
}
