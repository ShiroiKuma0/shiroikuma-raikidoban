/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.configuration.UiConfig;
import net.pierrox.lightning_launcher.configuration.UiSlot;
import net.pierrox.lightning_launcher.configuration.UiTheme;

public class ColorPickerDialog
        extends
        Dialog
        implements
        ColorPickerView.OnColorChangedListener,
        View.OnClickListener, DialogInterface.OnCancelListener {

    private ColorPickerView mColorPicker;

    private EditText mHexEditor;
    private LinearLayout mRecentColors;
    /** The live selected colour; the OK button commits this. */
    private int mCurrentColor;

    private OnColorChangedListener mListener;

    public ColorPickerDialog(Context context, int initialColor) {
        super(context);

        init(initialColor);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (mListener != null) mListener.onColorDialogCanceled();
    }

    private void init(int color) {
        // To fight color banding.
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setUp(color);

    }

    private void setUp(int color) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.dialog_color_picker, null);

        setContentView(layout);

        setTitle(R.string.dialog_color_picker);

        mColorPicker = layout.findViewById(R.id.color_picker_view);
        mHexEditor = layout.findViewById(R.id.hex_editor);
        mRecentColors = layout.findViewById(R.id.recent_colors);
        mCurrentColor = color;
        mHexEditor.clearFocus();

        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (dest.length() - (dend - dstart) + (end - start) > 8) {
                    return "";
                }

                boolean ok = true;
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                        ok = false;
                        break;
                    }
                }

                return ok ? null : "";
            }
        };
        mHexEditor.setFilters(new InputFilter[]{filter});
        mHexEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // pass
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // pass
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int color = Color.parseColor("#" + fillHex(s.toString()));
                    if (color != mColorPicker.getColor()) {
                        mColorPicker.setColor(color, false);
                        mCurrentColor = color;
                    }
                } catch (Exception e) {
                    // pass
                    e.printStackTrace();
                }
            }
        });

//		((LinearLayout) mOldColor.getParent()).setPadding(
//			Math.round(mColorPicker.getDrawingOffset()),
//			0,
//			Math.round(mColorPicker.getDrawingOffset()),
//			0
//		);

        Button cancel = layout.findViewById(R.id.cancel_button);
        Button ok = layout.findViewById(R.id.ok_button);
        cancel.setOnClickListener(this);
        ok.setOnClickListener(this);
        styleButton(cancel);
        styleButton(ok);

        mColorPicker.setOnColorChangedListener(this);
        mColorPicker.setColor(color, true);
        setOnCancelListener(this);

        buildRecentColors();
    }

    // Style a Cancel / OK button with the BUTTONS slots (BUTTON_BG fill, BUTTON_BORDER stroke + corner
    // radius, BUTTON_TEXT colour + font) — black bg / yellow text + border by default, all configurable
    // under the 「白い熊 雷起動盤 UI」 Buttons section. Mirrors UiChrome.applyButton, which lives in the
    // app module and so is not reachable from this core class.
    private void styleButton(Button button) {
        if (button == null) {
            return;
        }
        float d = button.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiTheme.color(UiSlot.BUTTON_BG));
        bg.setCornerRadius(UiTheme.cornerRadiusDp(UiSlot.BUTTON_BORDER) * d);
        int width = UiTheme.borderWidthDp(UiSlot.BUTTON_BORDER);
        if (width > 0) {
            bg.setStroke(Math.round(width * d), UiTheme.color(UiSlot.BUTTON_BORDER));
        }
        button.setBackground(bg);
        button.setTextColor(UiTheme.color(UiSlot.BUTTON_TEXT));
        UiTheme.applyFont(button, UiSlot.BUTTON_TEXT);
        int padH = Math.round(16 * d);
        int padV = Math.round(10 * d);
        button.setPadding(padH, padV, padH, padV);
    }

    // A row of round one-tap swatches for recently-picked colours (see UiConfig). Tapping one selects
    // that colour immediately. Hidden when there are no recent colours.
    private void buildRecentColors() {
        if (mRecentColors == null) {
            return;
        }
        mRecentColors.removeAllViews();
        int[] recents = UiConfig.get().getRecentColors();
        if (recents.length == 0) {
            mRecentColors.setVisibility(View.GONE);
            return;
        }
        float d = getContext().getResources().getDisplayMetrics().density;
        int size = Math.round(28 * d);
        int margin = Math.round(5 * d);
        int stroke = Math.max(1, Math.round(d));
        int strokeColor = UiTheme.color(UiSlot.ACCENT);
        for (final int swatchColor : recents) {
            View swatch = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.leftMargin = margin;
            lp.rightMargin = margin;
            swatch.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(swatchColor);
            g.setStroke(stroke, strokeColor);
            swatch.setBackground(g);
            swatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    commit(swatchColor);
                }
            });
            mRecentColors.addView(swatch);
        }
        mRecentColors.setVisibility(View.VISIBLE);
    }

    // Apply the chosen colour (notify the listener), record it as recent, and close.
    private void commit(int color) {
        if (mListener != null) {
            mListener.onColorChanged(color);
        }
        UiConfig.get().addRecentColor(color);
        dismiss();
    }

    @Override
    public void onColorChanged(int color) {

        mCurrentColor = color;

		/*
		if (mListener != null) {
			mListener.onColorChanged(color);
		}
		*/

        String hex = Integer.toHexString(color);

        mHexEditor.setText(fillHex(hex));
    }

    private String fillHex(String hex) {
        while (hex.length() < 8) {
            hex = '0' + hex;
        }
        return hex;
    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     *
     * @param listener
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ok_button) {
            commit(mCurrentColor);
        } else if (id == R.id.cancel_button) {
            if (mListener != null) {
                mListener.onColorDialogCanceled();
            }
            dismiss();
        }
    }

    public interface OnColorChangedListener {
        void onColorChanged(int color);

        void onColorDialogSelected(int color);

        void onColorDialogCanceled();
    }
//	
//	@Override
//	public Bundle onSaveInstanceState() {
//		Bundle state = super.onSaveInstanceState();
//		state.putInt("old_color", mOldColor.getColor());
//		state.putInt("new_color", mNewColor.getColor());
//		return state;
//	}
//	
//	@Override
//	public void onRestoreInstanceState(Bundle savedInstanceState) {
//		super.onRestoreInstanceState(savedInstanceState);
//		mOldColor.setColor(savedInstanceState.getInt("old_color"));
//		mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
//	}
}
