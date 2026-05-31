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
import android.graphics.RadialGradient;

public class SvgRadialGradient extends SvgGradient {
    float cx, cy, r;

    SvgRadialGradient(SvgSvg root, SvgGroup parent) {
        super(root, parent);
    }

    void updatePaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RadialGradient rg = new RadialGradient(cx, cy, r, getStopColors(), getStopOffsets(), getTileMode());
        if (mTransform != null) {
            rg.setLocalMatrix(mTransform);
        }
        mPaint.setShader(rg);
    }
}
