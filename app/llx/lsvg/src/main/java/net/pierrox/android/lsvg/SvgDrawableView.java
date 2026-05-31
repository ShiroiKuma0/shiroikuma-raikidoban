/*
 * Copyright (C) 2017 The Android Open Source Project
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
package net.pierrox.android.lsvg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Method;

public class SvgDrawableView extends ImageView {
    private SvgDrawable mSvgDrawable;
    private Matrix mInverseMatrix = new Matrix();
    private Integer mForcedLayerType;

    public SvgDrawableView(Context context, String name) {
        super(context, null);

        setImageDrawable(new SvgDrawable(context, name));
    }

    @SuppressLint("ResourceType")
    public SvgDrawableView(Context context, AttributeSet set) {
        super(context, set);

        int[] attrs = new int[] {
                android.R.attr.src,
                android.R.attr.layerType,
        };

        TypedArray a = context.getTheme().obtainStyledAttributes(set, attrs, 0, 0);

        try {
            String layerType = a.getString(1);
            if(layerType != null) {
                mForcedLayerType = Integer.parseInt(layerType);
            }

            int srcId = a.getResourceId(0, 0);
            if(srcId != 0) {
                if (SvgDrawable.isSvgObject(context, srcId)) {
                    setImageDrawable(new SvgDrawable(context, srcId));
                } else {
                    setImageDrawable(getResources().getDrawable(srcId));
                }
            }
        } finally {
            a.recycle();
        }
    }

    private void disableHardwareAccelerations() {
        if(mForcedLayerType == null) {
            try {
                Method setLayerType = getClass().getMethod("setLayerType", int.class, Paint.class);
                setLayerType.invoke(this, View.LAYER_TYPE_SOFTWARE, null);
            } catch (Exception e) {
                // pass
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        updateMatrix();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updateMatrix();
    }

    private void updateMatrix() {
        getImageMatrix().invert(mInverseMatrix);
    }

    @Override
    public void setImageDrawable(Drawable d) {
        super.setImageDrawable(d);
        if(d instanceof SvgDrawable) {
            disableHardwareAccelerations();
            mSvgDrawable = (SvgDrawable) d;
            updateMatrix();
        }
    }

    public SvgDrawable getSvgDrawable() {
        return mSvgDrawable;
    }

    /**
     * Map from the view viewport to the svg viewport.
     * @param pts
     */
    public void mapPointToSvgDrawable(float[] pts) {
        mInverseMatrix.mapPoints(pts);
    }
}
