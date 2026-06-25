package net.pierrox.lightning_launcher.data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Pair;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.script.Script;

import java.net.URISyntaxException;
import java.text.DecimalFormat;

public class EventAction {
    public int action;
    public String data;
    public EventAction next;

    public EventAction() {
        // empty constructor for serialization
    }

    public EventAction(int action, String data) {
        this.action = action;
        this.data = data;
    }

    public EventAction(int action, String data, EventAction next) {
        this.action = action;
        this.data = data;
        this.next = next;
    }

    public static EventAction UNSET() {
        return new EventAction(GlobalConfig.UNSET, null);
    }

    public static final EventAction NOTHING() {
        return new EventAction(GlobalConfig.NOTHING, null);
    }

    public EventAction clone() {
        return new EventAction(action, data, next == null ? null : next.clone());
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass() != EventAction.class) return false;
        EventAction ea = (EventAction) o;
        if (this.action != ea.action) return false;
        if ((this.data == null && ea.data != null) || (this.data != null && !this.data.equals(ea.data)))
            return false;
        return (this.next != null || ea.next == null) && (this.next == null || this.next.equals(ea.next));
    }

    private static final String JIYU_PACKAGE = "shiroikuma.jiyusagyoban";
    private static final String JIYU_SHORTCUT_TASK_NAME_EXTRA = "com.opentasker.widget.TASK_NAME";

    /** Prepend the 「白い熊 自由作業盤: 」 marker to a shortcut's label when its launch intent is a
     * jiyusagyoban run-task shortcut (component package {@code shiroikuma.jiyusagyoban} carrying the
     * task-name extra), mirroring the "Tasker: <task>" prefix. Idempotent; else returns {@code label}. */
    public static String decorateJiyuShortcutLabel(android.content.Context ctx, Intent intent, String label) {
        if (ctx == null || label == null || intent == null
                || !intent.hasExtra(JIYU_SHORTCUT_TASK_NAME_EXTRA)) {
            return label;
        }
        String pkg = intent.getPackage();
        if (pkg == null && intent.getComponent() != null) {
            pkg = intent.getComponent().getPackageName();
        }
        if (!JIYU_PACKAGE.equals(pkg)) {
            return label;
        }
        String marker = ctx.getString(net.pierrox.lightning_launcher.R.string.acd_jiyu_prefix, "");
        if (label.startsWith(marker)) {
            return label;
        }
        return ctx.getString(net.pierrox.lightning_launcher.R.string.acd_jiyu_prefix, label);
    }

    public String describe(LightningEngine engine) {
        if (data != null) {
            switch (action) {
                case GlobalConfig.RUN_SCRIPT:
                    Pair<Integer, String> idData = Script.decodeIdAndData(data);
                    if (idData != null) {
                        Script script = engine.getScriptManager().getOrLoadScript(idData.first);
                        if (script != null) {
                            return script.name;
                        }
                    }
                    break;

                case GlobalConfig.LAUNCH_APP:
                case GlobalConfig.LAUNCH_SHORTCUT:
                    try {
                        Intent intent = Intent.parseUri(data, 0);
                        // Prefer the name captured when the shortcut/Tasker task was picked, so e.g.
                        // a Tasker task shows "Tasker: <Task>" rather than just the resolved app label.
                        String label = intent.getStringExtra(LightningIntent.INTENT_EXTRA_SHORTCUT_LABEL);
                        if (label != null) {
                            return decorateJiyuShortcutLabel(engine.getContext(), intent, label);
                        }
                        PackageManager packageManager = engine.getContext().getPackageManager();
                        ResolveInfo activity = packageManager.resolveActivity(intent, 0);
                        if (activity != null) {
                            return decorateJiyuShortcutLabel(engine.getContext(), intent, activity.loadLabel(packageManager).toString());
                        }
                    } catch (URISyntaxException e) {
                        // pass
                    }
                    break;

                case GlobalConfig.GO_DESKTOP_POSITION:
                    try {
                        Intent intent = Intent.parseUri(data, 0);
                        int p = intent.getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, Page.FIRST_DASHBOARD_PAGE);
                        Page page = engine.getOrLoadPage(p);
                        String description = Utils.formatPageName(page, page.findFirstOpener());
                        if (intent.hasExtra(LightningIntent.INTENT_EXTRA_X)) {
                            float x = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_X, 0);
                            float y = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_Y, 0);
                            float s = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_SCALE, 1);
                            boolean absolute = intent.getBooleanExtra(LightningIntent.INTENT_EXTRA_ABSOLUTE, true);
                            DecimalFormat df = new DecimalFormat("0.##");
                            description += absolute ? " @" : " +";
                            description += df.format(x) + "x" + df.format(y) + "/" + df.format(s);
                        }
                        return description;
                    } catch (URISyntaxException e) {
                        // pass
                    }
                    break;

                case GlobalConfig.OPEN_FOLDER:
                    try {
                        int folderPage = Integer.parseInt(data);
                        Page page = engine.getOrLoadPage(folderPage);
                        return Utils.formatPageName(page, page.findFirstOpener());
                    } catch (NumberFormatException e) {
                        // pass
                    }
                    break;

                case GlobalConfig.SET_VARIABLE:
                    Variable v = Variable.decode(data);
                    if (v != null) {
                        return v.describe();
                    }
                    break;
            }
        }

        return null;
    }
}
