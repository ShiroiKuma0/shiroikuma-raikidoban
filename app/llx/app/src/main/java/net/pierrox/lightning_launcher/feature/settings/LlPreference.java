package net.pierrox.lightning_launcher.feature.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

/**
 * A framework {@link Preference} that themes its title + summary from 「白い熊 雷起動盤 UI」 (PREF slots),
 * so the Lightning Settings rows follow the configured colours + font. Referenced by FQN from
 * res/xml/preference_root.xml.
 */
public class LlPreference extends Preference {

    public LlPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LlPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LlPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LlPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View title = holder.findViewById(android.R.id.title);
        if (title instanceof TextView) {
            UiTheme.applyTo((TextView) title, UiSlot.PREF_TITLE);
        }
        View summary = holder.findViewById(android.R.id.summary);
        if (summary instanceof TextView) {
            UiTheme.applyTo((TextView) summary, UiSlot.PREF_SUMMARY);
        }
    }
}
