package net.pierrox.lightning_launcher.data;

public class LightningIntent {
    public static final String INTENT_EXTRA_ACTION = "a";
    public static final String INTENT_EXTRA_DATA = "d";
    public static final String INTENT_EXTRA_EVENT_ACTION = "ea";
    public static final String INTENT_EXTRA_TARGET = "t";
    public static final String INTENT_EXTRA_DESKTOP = "p";
    public static final String INTENT_EXTRA_X = "x";
    public static final String INTENT_EXTRA_Y = "y";
    public static final String INTENT_EXTRA_SCALE = "s";
    public static final String INTENT_EXTRA_LOAD_SCRIPT_FROM_PACKAGE = "b";
    public static final String INTENT_EXTRA_ANIMATE = "n";
    public static final String INTENT_EXTRA_ABSOLUTE = "l";
    // Human-readable name of a picked shortcut/Tasker task, embedded in the launch intent at bind
    // time so EventAction.describe() can show e.g. "Tasker: <Task>" instead of just the app label.
    public static final String INTENT_EXTRA_SHORTCUT_LABEL = "net.pierrox.ll.shortcut_label";
}
