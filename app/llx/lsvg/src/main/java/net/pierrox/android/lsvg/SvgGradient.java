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

import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

abstract class SvgGradient extends SvgElement {
    private String mHref;
    private ArrayList<Float> mStopOffsets;
    private ArrayList<Integer> mStopColors;
    protected Paint mPaint;
    private Shader.TileMode mTileMode;
    private ArrayList<SvgElement> mUsers;

    SvgGradient(SvgSvg root, SvgGroup parent) {
        super(root, parent, true, null);
        mStopOffsets = new ArrayList<>();
        mStopColors = new ArrayList<>();
    }

    abstract void updatePaint();

    public Paint getPaint() {
        return mPaint;
    }

    public void setHref(String href) {
        mHref = href;
    }

    public String getHref() {
        return mHref;
    }

    void resolveHref(HashMap<String, SvgElement> elements_by_id) {
        if (mHref != null) {
            SvgGradient rg = (SvgGradient) elements_by_id.get(mHref.substring(1));
            if (rg != null) {
                mStopColors = rg.mStopColors;
                mStopOffsets = rg.mStopOffsets;
            } else {
                Log.e("XXX", mHref);
            }
        }
    }

    public void addStopColors(int stopColor) {
        this.mStopColors.add(stopColor);
    }

    int[] getStopColors() {
        int size = mStopColors.size();
        int[] a = new int[size];
        for (int i = 0; i < size; i++) {
            a[i] = mStopColors.get(i);
        }
        return a;
    }

    public void addStopOffset(float stopOffset) {
        this.mStopOffsets.add(stopOffset);
    }

    float[] getStopOffsets() {
        int size = mStopOffsets.size();
        float[] a = new float[size];
        for (int i = 0; i < size; i++) {
            a[i] = mStopOffsets.get(i);
        }
        return a;
    }

    public void setTileMode(Shader.TileMode tileMode) {
        this.mTileMode = tileMode;
    }

    Shader.TileMode getTileMode() {
        return mTileMode;
    }

    void addUser(SvgElement svgElement) {
        if(mUsers == null) {
            mUsers = new ArrayList<>();
        }
        mUsers.add(svgElement);
    }
}
