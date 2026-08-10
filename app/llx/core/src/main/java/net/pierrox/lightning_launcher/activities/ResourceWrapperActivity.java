package net.pierrox.lightning_launcher.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.util.Flash;
import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher.util.UiDialogStyler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ResourceWrapperActivity extends AppCompatActivity {
    public static final int REQUEST_PERMISSION_BASE = 1000000;
    public static final int REQUEST_PERMISSION_FONT_PICKER = REQUEST_PERMISSION_BASE + 1;

    private ResourcesWrapperHelper mResourcesWrapperHelper;


    private java.util.Locale mForcedLocale;

    @Override
    protected void attachBaseContext(Context newBase) {
        // 白い熊 雷起動盤 is black-yellow only — never the stock light chrome. Pin AppCompat to night
        // here, i.e. before super.onCreate() builds the delegate, so every DayNight theme resolves to
        // values-night whatever the phone's own dark-mode switch says. (This used to follow
        // SystemConfig.appStyle, whose LIGHT default made a fresh install come up white-on-orange.)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        mForcedLocale = UiConfig.getStoredLocale(newBase);
        super.attachBaseContext(UiConfig.applyUiConfiguration(newBase));
    }

    @Override
    public void applyOverrideConfiguration(android.content.res.Configuration overrideConfiguration) {
        // AppCompat passes a night-mode override config here; re-assert the forced locale so it isn't
        // reset back to the system locale.
        if (overrideConfiguration != null && mForcedLocale != null) {
            overrideConfiguration.setLocale(mForcedLocale);
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (themeSystemBars()) {
            // Black-yellow everywhere: chrome activities get a black status + nav bar. The wallpaper home
            // (Dashboard / app drawer) opts out so it can keep its wallpaper-edge bars.
            getWindow().setStatusBarColor(UiTheme.color(UiSlot.STATUSBAR_BG));
            getWindow().setNavigationBarColor(UiTheme.color(UiSlot.BACKGROUND));
        }
    }

    /** Whether to paint the system bars with the UI theme. Overridden to false by the wallpaper home. */
    protected boolean themeSystemBars() {
        return true;
    }

    @Override
    public final Resources getResources() {
        if(mResourcesWrapperHelper == null) {
            mResourcesWrapperHelper = new ResourcesWrapperHelper(this, super.getResources());
        }
        return mResourcesWrapperHelper.getResources();
    }

    public final Resources getRealResources() {
        return super.getResources();
    }

    public boolean checkPermissions(String[] permissions, int[] rationales, final int requestCode) {
        final ArrayList<String> permissionsToRequest = new ArrayList<>();
        final ArrayList<String> permissionsToExplain = new ArrayList<>();
        for (String p : permissions) {
                if(checkSelfPermission(p) == PackageManager.PERMISSION_DENIED) {
                    if(shouldShowRequestPermissionRationale(p)) {
                        permissionsToExplain.add(p);
                    } else {
                        permissionsToRequest.add(p);
                    }
                }
            }

        if (permissionsToExplain.size() == 0) {
                if(permissionsToRequest.size() > 0) {
                    requestPermissions(listToArray(permissionsToRequest), requestCode);
                    return false;
                } else {
                    return true;
                }
            } else {
                permissionsToRequest.addAll(permissionsToExplain);
                final String[] permissionsToRequestArray = listToArray(permissionsToRequest);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.pr_t));
                SpannableStringBuilder message = new SpannableStringBuilder();
                message.append(getString(R.string.pr_s));
                int l = permissionsToRequest.size();
                for (int i=0; i<l; i++) {
                    String p = permissionsToRequest.get(i);
                    String short_p = p.substring(p.lastIndexOf('.')+1);
                    SpannableString bold_p = new SpannableString(short_p);
                    bold_p.setSpan(new StyleSpan(Typeface.BOLD), 0, short_p.length(), 0);
                    message.append("\n\n • ")
                           .append(bold_p)
                           .append('\n')
                           .append(getString(rationales[i]));
                }
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(permissionsToRequestArray, requestCode);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int l = permissionsToRequestArray.length;
                        int[] grantResults = new int[l];
                        Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
                        onRequestPermissionsResult(requestCode, permissionsToRequestArray, grantResults);
                    }
                });
                UiDialogStyler.styleOnShow(builder.create()).show();
                return false;
            }
    }

    protected boolean areAllPermissionsGranted(int[] grantResults, int errorToast) {
        boolean ok = true;
        for (int r : grantResults) {
            if(r == PackageManager.PERMISSION_DENIED) {
                ok = false;
                break;
            }
        }
        if(ok) {
            return true;
        } else {
            Flash.show(this, errorToast, Toast.LENGTH_LONG);
            finish();
            return false;
        }
    }

    private static String[] listToArray(List<String> l) {
        String[] p = new String[l.size()];
        l.toArray(p);
        return p;
    }
}
