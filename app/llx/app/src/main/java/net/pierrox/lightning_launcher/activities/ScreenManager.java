/*
MIT License

Copyright (c) 2022 Pierre Hébert, f43nd1r

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.data.Action;
import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Screen;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.util.FilesHolder;
import net.pierrox.lightning_launcher.util.Flash;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher_extreme.R;
import net.pierrox.lightning_launcher.util.UiDialogStyler;
import net.pierrox.lightning_launcher.util.UiChrome;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

import org.koin.java.KoinJavaComponent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ScreenManager extends ResourceWrapperActivity implements OnClickListener, View.OnLongClickListener {

    private final FilesHolder filesHolder = KoinJavaComponent.get(FilesHolder.class);
    protected static final int SCREEN_MODE_LWP = 6;
    private static final int DIALOG_LABEL = 2;
    private static final int DIALOG_CONFIRM_DELETE = 3;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int[] SCREEN_SUBVIEWS_IDS = new int[]{R.id.screen, R.id.screen_icon, R.id.screen_label, R.id.screen_selected};
    private static final String SIS_CURRENT_PAGE = "c";
    private static final int SCREEN_MODE_EDIT = 1;
    private static final int SCREEN_MODE_SELECT = 2;
    private static final int SCREEN_MODE_SELECT_MULTIPLE = 3;
    private static final int SCREEN_MODE_GOTO = 4;
    private static final int SCREEN_MODE_SHORTCUT = 5;
    private static final float PREVIEW_RATIO = 0.3f;
    private final Rect mTmpRect = new Rect();
    private LightningEngine mLightningEngine;
    private net.pierrox.lightning_launcher.engine.Screen mScreen;
    private int mMode;
    private ViewGroup mScreenContainer;
    private List<Screen> mScreens;
    private int mHomePage;
    private int mCurrentPage;
    private Drawable mDefaultIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLightningEngine = LLApp.get().getAppEngine();

        mDefaultIcon = getResources().getDrawable(R.drawable.icon);

        // The real (possibly live) wallpaper shows through: the manifest gives this activity the
        // transparent windowShowWallpaper theme. WallpaperManager.getDrawable() was only ever the
        // static image, never a live wallpaper.

        setContentView(R.layout.screen_manager);

        mScreenContainer = findViewById(R.id.sm_container);

        int[] btns = new int[]{
                R.id.sm_first,
                R.id.sm_previous,
                R.id.sm_next,
                R.id.sm_last,
                R.id.sm_home,
                R.id.sm_delete,
                R.id.sm_add,
                R.id.sm_clone,
        };
        String codes = "ABCDF476";
        Typeface typeface = LLApp.get().getIconsTypeface();
        for (int i = 0; i < btns.length; i++) {
            Button btn = findViewById(btns[i]);
            btn.setOnClickListener(this);
            btn.setOnLongClickListener(this);
            btn.setText(codes.substring(i, i + 1));
            btn.setTypeface(typeface);
            btn.setTextColor(0xff000000);
        }

        init();

        int page = Page.NONE;
        if (savedInstanceState != null) {
            page = savedInstanceState.getInt(SIS_CURRENT_PAGE);
        } else {
            if (mMode == SCREEN_MODE_LWP) {
                page = mLightningEngine.getGlobalConfig().lwpScreen;
            }
            if (page == Page.NONE) {
                page = mMode == SCREEN_MODE_SELECT || mMode == SCREEN_MODE_SHORTCUT || mMode == SCREEN_MODE_LWP ? mLightningEngine.readCurrentPage(Page.FIRST_DASHBOARD_PAGE) : mHomePage;
            }
        }
        selectScreenByPage(page);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mScreen.destroy();
    }

    private void init() {
        mScreen = new net.pierrox.lightning_launcher.engine.Screen(this, 0) {
            @Override
            public ScreenIdentity getIdentity() {
                return ScreenIdentity.DESKTOP_PREVIEW;
            }

            @Override
            protected Resources getRealResources() {
                return ScreenManager.this.getRealResources();
            }

            @Override
            public void launchItem(ItemView itemView) {
                // pass
            }
        };
        mScreen.setWindow(getWindow());

        mScreens = new ArrayList<Screen>();

        GlobalConfig gc = mLightningEngine.getGlobalConfig();
        final int[] screens_order = gc.screensOrder;
        final String[] screens_name = gc.screensNames;
        int n = screens_order.length;
        for (int i = 0; i < n; i++) {
            int page = screens_order[i];
            Bitmap icon;
            try {
                icon = BitmapFactory.decodeFile(Page.getPageIconFile(mLightningEngine.getBaseDir(), page).getAbsolutePath());
            } catch (Throwable t) {
                icon = null;
            }
            Screen screen = new Screen(page, icon, screens_name[i], true);
            mScreens.add(screen);
        }
        mHomePage = gc.homeScreen;

        mMode = getMode();
        if (mMode == SCREEN_MODE_LWP) {
            TextView tv = findViewById(R.id.sm_title);
            tv.setText(R.string.sd);
            tv.setVisibility(View.VISIBLE);
        } else if (mMode == SCREEN_MODE_SELECT_MULTIPLE) {
            Intent intent = getIntent();
            int[] selected_pages = intent.getIntArrayExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS);
            for (Screen screen : mScreens) {
                screen.selected = false;
                for (int page : selected_pages) {
                    if (screen.page == page) {
                        screen.selected = true;
                        break;
                    }
                }
            }
            String title = intent.getStringExtra(API.SCREEN_PICKER_INTENT_EXTRA_TITLE);
            if (title != null) {
                TextView tv = findViewById(R.id.sm_title);
                tv.setText(title);
                tv.setVisibility(View.VISIBLE);
            }
            Button ok = findViewById(R.id.sm_ok);
            ok.setOnClickListener(this);
            ok.setVisibility(View.VISIBLE);
        }

        mScreenContainer.removeAllViews();

        n = mScreens.size();
        for (int i = 0; i < n; i++) {
            addScreen(i, i);
        }

        updateScreenViews();

        findViewById(R.id.sm_buttons).setVisibility(mMode == SCREEN_MODE_EDIT ? View.VISIBLE : View.GONE);

        if (mMode == SCREEN_MODE_EDIT) {
            showGlobalNav();
        }
    }

    protected int getMode() {
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)) {
            return SCREEN_MODE_SELECT;
        } else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            return SCREEN_MODE_SHORTCUT;
        } else if (Intent.ACTION_RUN.equals(action)) {
            return SCREEN_MODE_GOTO;
        } else if (Intent.ACTION_EDIT.equals(action)) {
            return SCREEN_MODE_EDIT;
        } else {
            return SCREEN_MODE_SELECT_MULTIPLE;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SIS_CURRENT_PAGE, mCurrentPage);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mScreen.pause();

        if (mMode == SCREEN_MODE_EDIT) {
            int current = mLightningEngine.readCurrentPage(Page.FIRST_DASHBOARD_PAGE);
            boolean current_exists = false;
            int n = mScreens.size();
            int[] screens_order = new int[n];
            String[] screens_names = new String[n];
            for (int i = 0; i < n; i++) {
                Screen screen = mScreens.get(i);
                int p = screen.page;
                screens_order[i] = p;
                screens_names[i] = screen.label;
                if (p == current) {
                    current_exists = true;
                }
            }

            GlobalConfig gc = mLightningEngine.getGlobalConfig();
            gc.homeScreen = mHomePage;
            gc.screensOrder = screens_order;
            gc.screensNames = screens_names;
            mLightningEngine.notifyGlobalConfigChanged();

            if (!current_exists) {
                (LLApp.get()).displayPagerPage(mHomePage, true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mScreen.resume();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Screen screen = mScreens.get(getScreenIndex(mCurrentPage));

        AlertDialog.Builder builder;

        switch (id) {
            case DIALOG_LABEL:
                builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.l_custom);
                final EditText edit_text = new EditText(this);
                String label = screen.label != null ? screen.label : String.valueOf(screen.page + 1);
                edit_text.setText(label);
                edit_text.setSelection(label.length());
                FrameLayout l = new FrameLayout(this);
                l.setPadding(10, 10, 10, 10);
                l.addView(edit_text);
                builder.setView(l);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        String old_label = screen.label;
                        screen.label = edit_text.getText().toString();
                        updateScreenViews();
                        updateScreenShortcuts(screen, false, true, old_label);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return UiDialogStyler.styleOnShow(builder.create());

            case DIALOG_CONFIRM_DELETE:
                builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.confirm_delete);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        int index = getScreenIndex(mCurrentPage);
                        mLightningEngine.getPageManager().removePage(mCurrentPage);
                        GlobalConfig gc = mLightningEngine.getGlobalConfig();
                        if (gc.lockScreen == mCurrentPage) {
                            gc.lockScreen = Page.NONE;
                            mLightningEngine.notifyGlobalConfigChanged();
                        }
                        if (gc.overlayScreen == mCurrentPage) {
                            gc.overlayScreen = Page.NONE;
                            mLightningEngine.notifyGlobalConfigChanged();
                        }
                        Screen deleted = mScreens.remove(index);
                        mScreenContainer.removeViewAt(index);
                        Screen selected_screen = mScreens.get(index > 0 ? index - 1 : index);
                        if (deleted.page == mHomePage) {
                            mHomePage = selected_screen.page;
                            (LLApp.get()).displayPagerPage(mHomePage, true);
                        }
                        updateScreenViews();
                        selectScreenByPage(selected_screen.page);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return UiDialogStyler.styleOnShow(builder.create());
        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_NAV_ACTION) {
            if (resultCode == RESULT_OK && data != null) {
                applyNavAction(mNavEditPage, mNavEditSlot, EventActionSetup.getEventActionFromIntent(data));
            }
            return;
        }
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                FileOutputStream fos = null;
                try {
                    File tmpImageFile = filesHolder.getTempImageFile();
                    File icon_file = Page.getPageIconFile(mLightningEngine.getBaseDir(), mCurrentPage);
                    Screen screen = mScreens.get(getScreenIndex(mCurrentPage));
                    if (tmpImageFile.exists()) {
                        Uri from_uri = Uri.fromFile(tmpImageFile);
                        Bitmap from = decodeUri(from_uri);

                        icon_file.getParentFile().mkdirs();

                        fos = new FileOutputStream(icon_file);
                        from.compress(CompressFormat.PNG, 100, fos);

                        screen.icon = from;
                    } else {
                        icon_file.delete();
                        screen.icon = null;
                    }

                    updateScreenViews();

                    updateScreenShortcuts(screen, true, false, null);
                } catch (Exception e) {
                    Flash.show(this, R.string.item_settings_icon_copy_failed);
                    e.printStackTrace();
                } finally {
                    if (fos != null) try {
                        fos.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(final View v) {

        if (mMode == SCREEN_MODE_SELECT) {
            int page = (Integer) v.getTag();
            Intent data = new Intent();
            data.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, page);
            setResult(RESULT_OK, data);
            finish();
            return;
        } else if (mMode == SCREEN_MODE_SHORTCUT) {
            int p = (Integer) v.getTag();
            Screen screen = mScreens.get(getScreenIndex(p));
            Page page = mLightningEngine.getOrLoadPage(p);
            Intent shortcut = PhoneUtils.createDesktopBookmarkShortcut(this, null, page, screen.label, screen.icon, true);
            setResult(RESULT_OK, shortcut);
            finish();
        } else if (mMode == SCREEN_MODE_SELECT_MULTIPLE) {
            if (v.getId() == R.id.sm_ok) {
                saveSelectionResult();
                finish();
                return;
            } else {
                int page = (Integer) v.getTag();
                Screen screen = mScreens.get(getScreenIndex(page));
                screen.selected = !screen.selected;
                updateScreenViews();
                return;
            }
        } else if (mMode == SCREEN_MODE_GOTO) {
            int page = (Integer) v.getTag();
            LLApp.get().displayPagerPage(page, false);
            finish();
            return;
        } else if (mMode == SCREEN_MODE_LWP) {
            GlobalConfig gc = mLightningEngine.getGlobalConfig();
            gc.lwpScreen = (Integer) v.getTag();
            mLightningEngine.notifyGlobalConfigChanged();
            finish();
            return;
        }

        final View screen_view;
        Screen screen;
        int page;
        int index = getScreenIndex(mCurrentPage);
        screen_view = mScreenContainer.getChildAt(index);
        int num_screens_minus_one = mScreens.size() - 1;
        boolean ensure_visible = false;

        switch (v.getId()) {
            case R.id.sm_first:
                if (index > 0) {
                    mScreenContainer.removeViewAt(index);
                    mScreenContainer.addView(screen_view, 0);
                    screen = mScreens.remove(index);
                    mScreens.add(0, screen);
                    ensure_visible = true;
                }
                break;

            case R.id.sm_previous:
                if (index > 0) {
                    mScreenContainer.removeViewAt(index);
                    mScreenContainer.addView(screen_view, index - 1);
                    screen = mScreens.remove(index);
                    mScreens.add(index - 1, screen);
                    ensure_visible = true;
                }
                break;

            case R.id.sm_next:
                if (index < num_screens_minus_one) {
                    mScreenContainer.removeViewAt(index);
                    mScreenContainer.addView(screen_view, index + 1);
                    screen = mScreens.remove(index);
                    mScreens.add(index + 1, screen);
                    ensure_visible = true;
                }
                break;

            case R.id.sm_last:
                if (index < num_screens_minus_one) {
                    mScreenContainer.removeViewAt(index);
                    mScreenContainer.addView(screen_view, num_screens_minus_one);
                    screen = mScreens.remove(index);
                    mScreens.add(num_screens_minus_one, screen);
                    ensure_visible = true;
                }
                break;

            case R.id.sm_delete:
                if (mScreens.size() > 1) {
                    showDialog(DIALOG_CONFIRM_DELETE);
                }
                break;

            case R.id.sm_home:
                mHomePage = mCurrentPage;
                updateScreenViews();
                break;

            case R.id.sm_add:
                page = Page.reservePage(mLightningEngine.getBaseDir(), false);
                if (page != Page.NONE) {
                    mScreens.add(index + 1, new Screen(page, null, getString(R.string.sm_an), false));
                    addScreen(getScreenIndex(page), index + 1);
                    updateScreenViews();
                    selectScreenByPage(page);
                }
                break;

            case R.id.sm_clone:
                LightningEngine.PageManager pm = mLightningEngine.getPageManager();
                page = pm.clonePage(pm.getOrLoadPage(mCurrentPage), false).id;
                mScreens.add(index + 1, new Screen(page, null, getString(R.string.sm_cn) + " " + mScreens.get(getScreenIndex(mCurrentPage)).label, false));
                addScreen(getScreenIndex(page), index + 1);
                updateScreenViews();
                selectScreenByPage(page);
                break;

            case R.id.screen_icon:
                page = (Integer) v.getTag();
                selectScreenByPage(page);
                ImagePicker.startActivity(this, REQUEST_PICK_IMAGE);
                break;

            case R.id.screen_label:
                page = (Integer) v.getTag();
                selectScreenByPage(page);
                try {
                    removeDialog(DIALOG_LABEL);
                } catch (Exception e) {
                }
                showDialog(DIALOG_LABEL);
                break;

            case R.id.screen:
                page = (Integer) v.getTag();
                selectScreenByPage(page);
                break;
        }

        if (ensure_visible) {
            mScreenContainer.post(new Runnable() {
                @Override
                public void run() {
                    screen_view.getHitRect(mTmpRect);
                    mTmpRect.offsetTo(0, 0);
                    HorizontalScrollView scroller = findViewById(R.id.sm_scroller);
                    scroller.requestChildRectangleOnScreen(screen_view, mTmpRect, false);
                }
            });
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int label_res = 0;
        switch (view.getId()) {
            case R.id.sm_first:
                label_res = R.string.sm_f;
                break;
            case R.id.sm_previous:
                label_res = R.string.sm_p;
                break;
            case R.id.sm_next:
                label_res = R.string.sm_n;
                break;
            case R.id.sm_last:
                label_res = R.string.sm_l;
                break;
            case R.id.sm_delete:
                label_res = R.string.sm_d;
                break;
            case R.id.sm_home:
                label_res = R.string.sm_h;
                break;
            case R.id.sm_add:
                label_res = R.string.sm_a;
                break;
            case R.id.sm_clone:
                label_res = R.string.sm_c;
                break;

            default:
                int page = (Integer) view.getTag();
                LLApp.get().displayPagerPage(page, false);
                finish();
                startActivity(new Intent(this, Dashboard.class));
                break;
        }
        if (label_res != 0) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            Flash.showAt(this, getString(label_res), Toast.LENGTH_SHORT, Gravity.TOP | Gravity.LEFT, location[0], location[1]);
        }
        return true;
    }

    private void updateScreenViews() {
        int n = mScreenContainer.getChildCount();
        for (int i = 0; i < n; i++) {
            View screen_view = mScreenContainer.getChildAt(i);
            int page = (Integer) screen_view.getTag();
            Screen screen = mScreens.get(getScreenIndex(page));

            TextView is_home_icon = screen_view.findViewById(R.id.screen_home);
            is_home_icon.setTypeface(LLApp.get().getIconsTypeface());
            is_home_icon.setVisibility(page == mHomePage ? View.VISIBLE : View.INVISIBLE);
            is_home_icon.setTextColor(0xffffffff);

            CheckBox selected = screen_view.findViewById(R.id.screen_selected);
            if (mMode == SCREEN_MODE_SELECT_MULTIPLE) {
                selected.setVisibility(View.VISIBLE);
                selected.setChecked(screen.selected);
            } else {
                selected.setVisibility(View.GONE);
            }

            ((TextView) screen_view.findViewById(R.id.screen_label)).setText(screen.label != null ? screen.label : String.valueOf(screen.page + 1));
            ImageView iv = screen_view.findViewById(R.id.screen_icon);
            if (screen.icon == null) {
                iv.setImageDrawable(mDefaultIcon);
            } else {
                iv.setImageBitmap(screen.icon);
            }

            if (mMode == SCREEN_MODE_EDIT) {
                updateNavChips(screen_view, page);
            }
        }
    }

    // ---- Per-desktop navigation gestures (fork) ----
    // Under each desktop in edit mode: its swipe left / swipe right / back / home action. Setting one
    // stores a GO_DESKTOP_POSITION (desktop only, no position) on that desktop's own config; "use the
    // global setting" unsets it so the global action (e.g. the fold-matrix swipes) applies again.

    private static final int[] NAV_CHIP_IDS = new int[]{R.id.screen_nav_swipe_left, R.id.screen_nav_swipe_right, R.id.screen_nav_back, R.id.screen_nav_home};
    private static final int[] NAV_LABELS = new int[]{R.string.sm_nav_swipe_left, R.string.sm_nav_swipe_right, R.string.sm_nav_back, R.string.sm_nav_home};
    private ActionsDescription mNavActions;

    private static EventAction getNavAction(PageConfig c, int slot) {
        switch (slot) {
            case 0: return c.swipeLeft;
            case 1: return c.swipeRight;
            case 2: return c.backKey;
            default: return c.homeKey;
        }
    }

    private static void setNavAction(PageConfig c, int slot, EventAction ea) {
        switch (slot) {
            case 0: c.swipeLeft = ea; break;
            case 1: c.swipeRight = ea; break;
            case 2: c.backKey = ea; break;
            default: c.homeKey = ea; break;
        }
    }

    private static EventAction getGlobalNavAction(GlobalConfig gc, int slot) {
        switch (slot) {
            case 0: return gc.swipeLeft;
            case 1: return gc.swipeRight;
            case 2: return gc.backKey;
            default: return gc.homeKey;
        }
    }

    /** The desktop a GO_DESKTOP_POSITION action jumps to, else Page.NONE. */
    private static int getNavTargetPage(EventAction ea) {
        if (ea == null || ea.action != GlobalConfig.GO_DESKTOP_POSITION || ea.data == null) {
            return Page.NONE;
        }
        try {
            return Intent.parseUri(ea.data, 0).getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, Page.NONE);
        } catch (Exception e) {
            return Page.NONE;
        }
    }

    private String getScreenLabel(int page) {
        for (Screen screen : mScreens) {
            if (screen.page == page) {
                return screen.label != null ? screen.label : String.valueOf(page + 1);
            }
        }
        return String.valueOf(page + 1);
    }

    private String describeNavAction(EventAction ea) {
        if (ea == null || ea.action == GlobalConfig.UNSET) {
            return getString(R.string.sm_nav_unset);
        }
        int target = getNavTargetPage(ea);
        if (target != Page.NONE) {
            return getScreenLabel(target);
        }
        if (mNavActions == null) {
            mNavActions = new ActionsDescription(this, Action.FLAG_TYPE_DESKTOP, false);
        }
        try {
            return mNavActions.getActionName(ea.action);
        } catch (Exception e) {
            return getString(R.string.sm_nav_unset);
        }
    }

    private static final int[] GLOBAL_NAV_CHIP_IDS = new int[]{R.id.sm_global_nav_swipe_left, R.id.sm_global_nav_swipe_right, R.id.sm_global_nav_back, R.id.sm_global_nav_home};

    // Read-only row above the desktops: the global actions an unset desktop gesture falls back to.
    private void showGlobalNav() {
        findViewById(R.id.sm_global_nav).setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.sm_global_nav_title);
        title.setText(R.string.sm_nav_global_title);
        title.setTextColor(UiTheme.color(UiSlot.BUTTON_TEXT));
        title.setShadowLayer(6, 0, 0, 0xff000000); // legible over a busy (live) wallpaper
        GlobalConfig gc = mLightningEngine.getGlobalConfig();
        for (int i = 0; i < GLOBAL_NAV_CHIP_IDS.length; i++) {
            TextView chip = findViewById(GLOBAL_NAV_CHIP_IDS[i]);
            UiChrome.applyButton(chip);
            chip.setPadding(chip.getPaddingLeft() / 2, chip.getPaddingTop() / 2, chip.getPaddingRight() / 2, chip.getPaddingBottom() / 2);
            chip.setText(navChipText(NAV_LABELS[i], describeNavAction(getGlobalNavAction(gc, i))));
            final int slot = i;
            chip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openNavSlot(Page.NONE, slot);
                }
            });
        }
    }

    // Small gesture name over the (full) action, so a long action name gets the room.
    private CharSequence navChipText(int labelRes, String value) {
        SpannableStringBuilder sb = new SpannableStringBuilder(getString(labelRes));
        sb.setSpan(new RelativeSizeSpan(0.7f), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append("\n").append(value);
        return sb;
    }

    private void updateNavChips(View screen_view, int page) {
        PageConfig c = mLightningEngine.getOrLoadPage(page).config;
        GlobalConfig gc = mLightningEngine.getGlobalConfig();
        for (int i = 0; i < NAV_CHIP_IDS.length; i++) {
            TextView chip = screen_view.findViewById(NAV_CHIP_IDS[i]);
            EventAction own = getNavAction(c, i);
            boolean inherited = own == null || own.action == GlobalConfig.UNSET;
            String value = inherited
                    ? describeNavAction(getGlobalNavAction(gc, i)) + " " + getString(R.string.sm_nav_global)
                    : describeNavAction(own);
            chip.setText(navChipText(NAV_LABELS[i], value));
            // the desktop's own setting is bright; an inherited global one is dimmed
            chip.setAlpha(inherited ? 0.55f : 1f);
        }
    }

    private static final int REQUEST_EDIT_NAV_ACTION = 3;
    private int mNavEditPage = Page.NONE;
    private int mNavEditSlot;

    /** Full name of an action plus its detail (target desktop and position, shortcut, script…). */
    private String describeNavActionFull(EventAction ea) {
        if (ea == null || ea.action == GlobalConfig.UNSET) {
            return getString(R.string.sm_nav_unset);
        }
        if (mNavActions == null) {
            mNavActions = new ActionsDescription(this, Action.FLAG_TYPE_DESKTOP, false);
        }
        String name;
        try {
            name = mNavActions.getActionName(ea.action);
        } catch (Exception e) {
            name = String.valueOf(ea.action);
        }
        String detail = null;
        try {
            detail = ea.describe(mLightningEngine);
        } catch (Exception e) {
            // pass
        }
        return detail == null ? name : name + ": " + detail;
    }

    // Tap on a navigation button: the full description of what that gesture does and where, then the
    // choices — a desktop to jump to, the full action editor, or (per desktop) back to the global one.
    // page == Page.NONE means the global action.
    private void openNavSlot(final int page, final int slot) {
        final boolean global = page == Page.NONE;
        GlobalConfig gc = mLightningEngine.getGlobalConfig();
        final EventAction globalEa = getGlobalNavAction(gc, slot);
        final EventAction own = global ? globalEa : getNavAction(mLightningEngine.getOrLoadPage(page).config, slot);
        boolean ownUnset = own == null || own.action == GlobalConfig.UNSET;

        StringBuilder info = new StringBuilder();
        if (global) {
            info.append(getString(R.string.sm_nav_info_global, describeNavActionFull(globalEa)));
            StringBuilder using = new StringBuilder();
            StringBuilder overriding = new StringBuilder();
            for (Screen screen : mScreens) {
                EventAction e = getNavAction(mLightningEngine.getOrLoadPage(screen.page).config, slot);
                if (e == null || e.action == GlobalConfig.UNSET) {
                    if (using.length() > 0) using.append(", ");
                    using.append(getScreenLabel(screen.page));
                } else {
                    if (overriding.length() > 0) overriding.append(", ");
                    overriding.append(getScreenLabel(screen.page)).append(" → ").append(describeNavAction(e));
                }
            }
            info.append("\n\n").append(getString(R.string.sm_nav_info_used_by, using.length() > 0 ? using : getString(R.string.sm_nav_unset)));
            info.append("\n\n").append(getString(R.string.sm_nav_info_overridden_by, overriding.length() > 0 ? overriding : getString(R.string.sm_nav_unset)));
        } else if (ownUnset) {
            info.append(getString(R.string.sm_nav_info_inherits, describeNavActionFull(globalEa)));
        } else {
            info.append(getString(R.string.sm_nav_info_own, describeNavActionFull(own)));
            info.append("\n\n").append(getString(R.string.sm_nav_info_global_unused, describeNavActionFull(globalEa)));
        }

        // choices: [use global] + desktops + full editor
        final int n = mScreens.size();
        final int offset = global ? 0 : 1;
        String[] names = new String[offset + n + 1];
        if (!global) {
            names[0] = getString(R.string.sm_nav_inherit);
        }
        int target = getNavTargetPage(own);
        int checked = !global && ownUnset ? 0 : -1;
        for (int i = 0; i < n; i++) {
            int p = mScreens.get(i).page;
            names[offset + i] = getString(R.string.sm_nav_go_to, getScreenLabel(p));
            if (p == target && !(ownUnset && !global)) {
                checked = offset + i;
            }
        }
        names[offset + n] = getString(R.string.sm_nav_other_action);

        float d = getResources().getDisplayMetrics().density;
        TextView infoView = new TextView(this);
        infoView.setText(info);
        infoView.setTextSize(15);
        int pad = Math.round(20 * d);
        infoView.setPadding(pad, Math.round(8 * d), pad, Math.round(12 * d));
        // the description scrolls with the choices as the list's (non-selectable) header
        final ListView list = new ListView(this);
        list.addHeaderView(infoView, null, false);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, names));
        if (checked >= 0) {
            list.setItemChecked(checked + 1, true);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(NAV_LABELS[slot]) + " — " + (global ? getString(R.string.sm_nav_scope_global) : getScreenLabel(page)));
        builder.setView(list);
        builder.setNegativeButton(android.R.string.cancel, null);
        final AlertDialog dialog = UiDialogStyler.show(builder);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int which = position - list.getHeaderViewsCount();
                dialog.dismiss();
                if (which == offset + n) {
                    mNavEditPage = page;
                    mNavEditSlot = slot;
                    EventAction start = own == null ? EventAction.UNSET() : own;
                    EventActionSetup.startActivityForResult(ScreenManager.this, start, false, Action.FLAG_TYPE_DESKTOP, false, REQUEST_EDIT_NAV_ACTION);
                } else if (which < offset) {
                    applyNavAction(page, slot, EventAction.UNSET());
                } else {
                    Intent go = new Intent(ScreenManager.this, Dashboard.class);
                    go.putExtra(LightningIntent.INTENT_EXTRA_DESKTOP, mScreens.get(which - offset).page);
                    applyNavAction(page, slot, new EventAction(GlobalConfig.GO_DESKTOP_POSITION, go.toUri(0)));
                }
            }
        });
    }

    private void applyNavAction(int page, int slot, EventAction ea) {
        if (page == Page.NONE) {
            GlobalConfig gc = mLightningEngine.getGlobalConfig();
            switch (slot) {
                case 0: gc.swipeLeft = ea; break;
                case 1: gc.swipeRight = ea; break;
                case 2: gc.backKey = ea; break;
                default: gc.homeKey = ea; break;
            }
            mLightningEngine.notifyGlobalConfigChanged();
        } else {
            Page p = mLightningEngine.getOrLoadPage(page);
            setNavAction(p.config, slot, ea);
            p.setModified();
        }
        mLightningEngine.saveData();
        showGlobalNav();
        updateScreenViews();
    }

    private void updateScreenShortcuts(Screen screen, boolean update_icon, boolean update_label, String old_label) {
        // look in all existing pages and shortcuts and update "screen&position" items matching this page

        ComponentName ll_component_name = new ComponentName(this, Dashboard.class);

        File screen_icon = Page.getPageIconFile(mLightningEngine.getBaseDir(), screen.page);
        byte[] buffer = new byte[512];

        for (int p : mLightningEngine.getPageManager().getAllPagesIds()) {
            Page page = mLightningEngine.getOrLoadPage(p);
            File icon_dir = page.getIconDir();
            for (Item i : page.items) {
                boolean modified = false;
                if (i.getClass() == Shortcut.class) {
                    Shortcut s = (Shortcut) i;
                    Intent intent = s.getIntent();
                    ComponentName cn = intent.getComponent();
                    if (cn != null && cn.compareTo(ll_component_name) == 0) {
                        if (intent.hasExtra(LightningIntent.INTENT_EXTRA_DESKTOP) && intent.getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, 0) == screen.page) {
                            // it looks like a screen&position shortcut
                            if (update_icon) {
                                icon_dir.mkdirs();
                                Utils.copyFileSafe(buffer, screen_icon, s.getDefaultIconFile());
                                modified = true;
                            }

                            if (update_label) {
                                if (old_label.equals(s.getLabel())) {
                                    s.setLabel(screen.label);
                                    modified = true;
                                }
                            }
                        }
                    }
                }
                if (modified) {
                    i.notifyChanged();
                }
            }
        }
    }

    private void selectScreenByPage(int page) {
        mCurrentPage = page;
        int n = mScreenContainer.getChildCount();
        for (int i = 0; i < n; i++) {
            View screen_view = mScreenContainer.getChildAt(i);
            View border = screen_view.findViewById(R.id.screen_border);
            int screen_page = (Integer) screen_view.getTag();

            boolean selected = (mMode != SCREEN_MODE_SELECT_MULTIPLE && screen_page == page);
            border.setBackgroundColor(selected ? 0x80ffffff : 0);
            border.setPadding(10, 10, 10, 10);
        }
    }

    private int getScreenIndex(int page) {
        for (int i = mScreens.size() - 1; i >= 0; i--) {
            if (mScreens.get(i).page == page) {
                return i;
            }
        }
        return 0;
    }

    private void addScreen(int index, int pos) {
        Screen screen = mScreens.get(index);
        int page = screen.page;
        View screen_view = getLayoutInflater().inflate(R.layout.screen, null);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        screen_view.setLayoutParams(lp);

        for (int id : SCREEN_SUBVIEWS_IDS) {
            View v = screen_view.findViewById(id);
            v.setOnClickListener(this);
            v.setTag(page);
        }
        screen_view.setOnLongClickListener(this);

        ImageView preview = screen_view.findViewById(R.id.screen_preview);
        preview.setImageDrawable(new PageDrawable(page));

        if (mMode == SCREEN_MODE_EDIT) {
            screen_view.findViewById(R.id.screen_nav).setVisibility(View.VISIBLE);
            for (int i = 0; i < NAV_CHIP_IDS.length; i++) {
                final int slot = i;
                TextView chip = screen_view.findViewById(NAV_CHIP_IDS[i]);
                UiChrome.applyButton(chip);
                chip.setPadding(chip.getPaddingLeft() / 2, chip.getPaddingTop() / 2, chip.getPaddingRight() / 2, chip.getPaddingBottom() / 2);
                chip.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openNavSlot(page, slot);
                    }
                });
            }
        }

        mScreenContainer.addView(screen_view, pos);
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = Utils.getStandardIconSize();

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    private void saveSelectionResult() {
        int count = 0;
        for (Screen screen : mScreens) {
            if (screen.selected) {
                count++;
            }
        }
        int[] selected_screens;
//		if(count == 0) {
//			selected_screens = new int[] { mHomePage };
//		} else {
        selected_screens = new int[count];
        count = 0;
        for (Screen screen : mScreens) {
            if (screen.selected) {
                selected_screens[count++] = screen.page;
            }
        }
//		}
        Intent data = new Intent();
        data.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS, selected_screens);
        setResult(RESULT_OK, data);
    }

    private class PageDrawable extends Drawable {
        private final int mPage;
        private final ItemLayout il;
        private final int mWidth;
        private final int mHeight;

        public PageDrawable(int page) {
            mPage = page;

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            mWidth = metrics.widthPixels;
            mHeight = metrics.heightPixels;

            Page p = mLightningEngine.getOrLoadPage(page);
            il = new ItemLayout(ScreenManager.this, null);
            il.setAllowDelayedViewInit(false);
            il.setScreen(mScreen);
            mScreen.takeItemLayoutOwnership(il);
            il.setPage(p);
            il.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
            il.layout(0, 0, mWidth, mHeight);
        }

        @Override
        public int getIntrinsicWidth() {
            return (int) (mWidth * PREVIEW_RATIO);
        }

        @Override
        public int getIntrinsicHeight() {
            return (int) (mHeight * PREVIEW_RATIO);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.scale(PREVIEW_RATIO, PREVIEW_RATIO);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            il.draw(canvas);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            // pass
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // pass
        }

    }
}
