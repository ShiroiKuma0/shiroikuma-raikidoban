package net.pierrox.lightning_launcher.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.view.menu.ListMenuItemView;
import androidx.appcompat.view.menu.MenuItemImpl;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.util.MenuPills;
import net.pierrox.lightning_launcher.util.UiIcons;
import net.pierrox.lightning_launcher.util.UiPills;
import net.pierrox.lightning_launcher_extreme.R;

/**
 * 白い熊: one row of an AppCompat popup menu — the toolbar ⋮ overflow of Lightning Settings and
 * Customize, and any popup added later. Swapped in for the library's row through the app's own copies
 * of {@code abc_popup_menu_item_layout} / {@code abc_cascading_menu_item_layout}, so no call site has to
 * know. An action row is a full-width outlined pill with its icon; a checkable row stays plain, 36 dp
 * tall, with the outlined switch instead of a tick box. Same shape as 白い熊 FairEmail's.
 */
@SuppressLint("RestrictedApi")
public class PillMenuItemView extends ListMenuItemView {
    private LinearLayout mContent;
    private TextView mTitle;
    private ImageView mArrow;
    private int mPadStart, mPadTop, mPadEnd, mPadBottom, mContentHeight;
    private boolean mListStyled;

    public PillMenuItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PillMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.content);
        mTitle = findViewById(R.id.title);
        mArrow = findViewById(R.id.submenuarrow);
        if (mContent != null) {
            mPadStart = mContent.getPaddingStart();
            mPadTop = mContent.getPaddingTop();
            mPadEnd = mContent.getPaddingEnd();
            mPadBottom = mContent.getPaddingBottom();
            mContentHeight = mContent.getLayoutParams().height;
        }
        View divider = findViewById(R.id.group_divider);
        if (divider != null) {
            // the clear 1 dp divider, its ends on the pill edges
            divider.setBackgroundColor(UiPills.divider());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
            lp.leftMargin = UiPills.dp(getContext(), 8);
            lp.rightMargin = UiPills.dp(getContext(), 8);
            lp.height = Math.max(1, UiPills.dp(getContext(), 1));
            divider.setLayoutParams(lp);
        }
    }

    @Override
    public void initialize(MenuItemImpl item, int menuType) {
        super.initialize(item, menuType);
        try {
            style(item);
        } catch (RuntimeException e) {
            // a row the library built differently than expected stays a stock row
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent parent = getParent();
        if (!mListStyled && parent instanceof AbsListView) {
            // the pill carries the ripple; the list's own rectangular highlight is switched off
            ((AbsListView) parent).setSelector(new ColorDrawable(0));
            ((AbsListView) parent).setBackgroundColor(UiTheme.color(UiSlot.MENU_BG));
            mListStyled = true;
        }
    }

    private void style(MenuItemImpl item) {
        if (mContent == null || mTitle == null) {
            return;
        }
        Context context = getContext();
        int id = item.getItemId();
        boolean checkable = item.isCheckable();
        boolean header = !item.isEnabled() && !item.hasSubMenu() && id == 0;
        boolean pill = !checkable && !header;
        boolean destructive = pill && MenuPills.isDestructive(id);
        int color = destructive ? UiTheme.color(UiSlot.DELETE) : UiTheme.color(UiSlot.MENU_TEXT);
        if (!item.isEnabled() && !header) {
            color = UiPills.disabled(color);
        }

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mContent.getLayoutParams();
        int h = UiPills.dp(context, 8);
        int v = pill ? UiPills.dp(context, 3) : 0;
        lp.height = checkable ? UiPills.dp(context, 36) : mContentHeight;
        lp.setMargins(h, v, h, v);
        mContent.setLayoutParams(lp);
        mContent.setBackground(pill ? UiPills.background(context, color, false) : null);
        // no end padding on a toggle row: the switch's right edge meets the pills' right edge
        mContent.setPaddingRelative(mPadStart, mPadTop, checkable ? 0 : mPadEnd, mPadBottom);

        mTitle.setTextColor(color);
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        mTitle.setTypeface(UiTheme.typeface(UiSlot.MENU_TEXT, android.graphics.Typeface.NORMAL));

        CharSequence text = item.getTitle();
        boolean opens = pill && (item.hasSubMenu() || MenuPills.opens(id)
                || (text != null && text.toString().endsWith("…")));
        if (mArrow != null) {
            if (opens) {
                mArrow.setImageDrawable(UiIcons.drawable(context, UiIcons.Icon.CHEVRON, color, 16f));
            }
            mArrow.setVisibility(opens ? VISIBLE : GONE);
        }

        for (int i = 0; i < mContent.getChildCount(); i++) {
            View child = mContent.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox box = (CheckBox) child;
                UiPills.applySwitch(box, true);
                box.setEnabled(item.isEnabled());
            } else if (child instanceof ImageView && child != mArrow) {
                ImageView icon = (ImageView) child;
                ViewGroup.MarginLayoutParams ilp = (ViewGroup.MarginLayoutParams) icon.getLayoutParams();
                ilp.setMarginStart(UiPills.dp(context, 14));
                ilp.setMarginEnd(-UiPills.dp(context, 6));
                icon.setLayoutParams(ilp);
                icon.setImageTintList(null);
                icon.setAlpha(item.isEnabled() ? 1f : 0.38f);
            }
        }
    }
}
