package net.pierrox.lightning_launcher.feature.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

/**
 * A framework {@link PreferenceCategory} whose heading follows the PREF_CATEGORY slot of
 * 「白い熊 雷起動盤 UI」. Referenced by FQN from res/xml/preference_root.xml.
 */
public class LlPreferenceCategory extends PreferenceCategory {

    public LlPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LlPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LlPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LlPreferenceCategory(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View title = holder.findViewById(android.R.id.title);
        if (title instanceof TextView) {
            UiTheme.applyTo((TextView) title, UiSlot.PREF_CATEGORY);
        }
    }
}
