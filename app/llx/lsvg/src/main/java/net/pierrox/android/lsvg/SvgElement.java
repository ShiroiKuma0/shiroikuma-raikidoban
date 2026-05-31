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

import android.graphics.Matrix;

public abstract class SvgElement {
    private String mId;
    private boolean mIsVisible = true;
    protected Matrix mTransform;

    protected SvgSvg mRoot;
    private SvgGroup mParent;

    public SvgElement(SvgSvg root, SvgGroup parent, boolean isVisible, Matrix transform) {
        mRoot = root;
        mParent = parent;
        mIsVisible = isVisible;
        mTransform = transform;
    }

    public SvgGroup getParent() {
        return mParent;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setVisible(boolean visible) {
        mIsVisible = visible;
        invalidate();
    }

    public void setTransform(Matrix transform) {
        mTransform = transform;
        invalidate();
    }

    public boolean setTransform(String specification) {
        try {
            Matrix matrix = mRoot.parseTransform(specification);
            mTransform = matrix;
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Matrix getTransform() {
        return mTransform;
    }

    public void invalidate() {
        mRoot.invalidate();
    }
}
