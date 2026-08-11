package net.pierrox.lightning_launcher.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

import java.util.List;

/**
 * The platform spinner adapter with its rows painted {@link UiSlot#DIALOG_TEXT}. Both
 * {@code simple_spinner_item} (the closed spinner) and {@code simple_spinner_dropdown_item} (the
 * dropdown panel) carry no colour of their own, so a stock {@link ArrayAdapter} draws the platform's
 * white text on our black chrome. The dropdown lives in its own window, out of reach of
 * {@link UiTheme#paintUnstyledText}, which is why this has to happen in the adapter.
 */
public class UiSpinnerAdapter<T> extends ArrayAdapter<T> {

    public UiSpinnerAdapter(Context context, List<T> objects) {
        super(context, android.R.layout.simple_spinner_item, objects);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public UiSpinnerAdapter(Context context, T[] objects) {
        super(context, android.R.layout.simple_spinner_item, objects);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return paint(super.getView(position, convertView, parent));
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return paint(super.getDropDownView(position, convertView, parent));
    }

    /** Both platform row layouts have a {@link TextView} for a root. */
    public static View paint(View row) {
        if (row instanceof TextView) {
            UiTheme.applyTo((TextView) row, UiSlot.DIALOG_TEXT);
        }
        return row;
    }
}
