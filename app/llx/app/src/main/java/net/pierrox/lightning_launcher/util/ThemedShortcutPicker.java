package net.pierrox.lightning_launcher.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * In-app, yellow-on-black replacement for the framework {@code ACTION_PICK_ACTIVITY} chooser that picks
 * which app should create a shortcut (the "Select a shortcut" list). The system chooser is drawn by
 * another process, so it can't follow our night theme — it shows up white-on-grey. This dialog inherits
 * the chrome look (and the user's 「白い熊 雷起動盤 UI」 overrides) and hands the chosen
 * {@code ACTION_CREATE_SHORTCUT} component back to the caller, which then launches it to build the
 * shortcut — exactly what {@code ACTION_PICK_ACTIVITY}'s result would have produced.
 */
public final class ThemedShortcutPicker {

    public interface Listener {
        /** Called with an {@code ACTION_CREATE_SHORTCUT} intent whose component is the chosen activity. */
        void onChosen(Intent chosen);

        /** Called when the dialog is dismissed without a choice (cancel / back / outside tap). */
        void onCancelled();
    }

    private ThemedShortcutPicker() {
    }

    public static void show(final Activity activity, final Listener listener) {
        final PackageManager pm = activity.getPackageManager();
        final List<ResolveInfo> shortcuts = pm.queryIntentActivities(new Intent(Intent.ACTION_CREATE_SHORTCUT), 0);
        // Tasker is the #1 choice -> pin it to the top; everything else follows alphabetically.
        final ResolveInfo.DisplayNameComparator nameComparator = new ResolveInfo.DisplayNameComparator(pm);
        Collections.sort(shortcuts, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                boolean aTasker = TaskerWidgets.TASKER_PACKAGE.equals(a.activityInfo.packageName);
                boolean bTasker = TaskerWidgets.TASKER_PACKAGE.equals(b.activityInfo.packageName);
                if (aTasker != bTasker) {
                    return aTasker ? -1 : 1;
                }
                return nameComparator.compare(a, b);
            }
        });

        final LayoutInflater inflater = activity.getLayoutInflater();
        ArrayAdapter<ResolveInfo> adapter = new ArrayAdapter<ResolveInfo>(activity, R.layout.shortcut_picker_item, R.id.label, shortcuts) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView != null ? convertView : inflater.inflate(R.layout.shortcut_picker_item, parent, false);
                ResolveInfo ri = shortcuts.get(position);
                TextView label = v.findViewById(R.id.label);
                label.setText(ri.loadLabel(pm));
                UiTheme.applyTo(label, UiSlot.DIALOG_TEXT);
                ((ImageView) v.findViewById(R.id.icon)).setImageDrawable(ri.loadIcon(pm));
                return v;
            }
        };

        // onDismiss fires for every teardown (choice, cancel, back, outside tap); the flag tells the two
        // apart so a cancel can roll back a freshly-added action while a choice does not.
        final boolean[] chosen = {false};
        AlertDialog pickerDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.tools_pick_shortcut)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chosen[0] = true;
                        ResolveInfo ri = shortcuts.get(which);
                        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
                        intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                        listener.onChosen(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        pickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!chosen[0]) {
                    listener.onCancelled();
                }
            }
        });
        pickerDialog.show();
        UiDialogStyler.style(pickerDialog);
    }
}
