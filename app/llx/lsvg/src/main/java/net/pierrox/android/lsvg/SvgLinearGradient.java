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

import android.graphics.LinearGradient;
import android.graphics.Paint;

public class SvgLinearGradient extends SvgGradient {
    float x1, y1, x2, y2;

    SvgLinearGradient(SvgSvg root, SvgGroup parent) {
        super(root, parent);
    }

    void updatePaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient lg = new LinearGradient(x1, y1, x2, y2, getStopColors(), getStopOffsets(), getTileMode());
        if (mTransform != null) {
            lg.setLocalMatrix(mTransform);
        }
        mPaint.setShader(lg);
    }
}
