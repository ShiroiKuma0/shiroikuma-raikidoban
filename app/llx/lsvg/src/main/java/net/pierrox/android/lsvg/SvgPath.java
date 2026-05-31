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
import android.graphics.Paint;
import android.graphics.Path;

public class SvgPath extends SvgElement {
    private Path mPath;
    /*package*/ Paint fillPaint;
    /*package*/ Paint strokePaint;


    SvgPath(SvgSvg root, SvgGroup parent, boolean isVisible, Matrix transform, Path path, Paint fillPaint, Paint strokePaint) {
        super(root, parent, isVisible, transform);
        this.mPath = path;
        this.fillPaint = fillPaint;
        this.strokePaint = strokePaint;
    }

    public void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
        invalidate();
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setStrokePaint(Paint strokePaint) {
        this.strokePaint = strokePaint;
        invalidate();
    }

    public Paint getStrokePaint() {
        return strokePaint;
    }

    public boolean setPath(String d) {
        try {
            this.mPath = mRoot.parsePath(d);
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Path getPath() {
        return mPath;
    }

    public boolean setStyle(String style) {
        try {
            SvgSvg.ParsedStyle parsedStyle = mRoot.parseStyle(style);
            setVisible(parsedStyle.isVisible);
            this.fillPaint = parsedStyle.fillPaint;
            this.strokePaint = parsedStyle.strokePaint;
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
