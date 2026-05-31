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

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SvgSvg extends SvgGroup {
    private Drawable mHolderDrawable;
    public float width;
    public float height;
    private HashMap<String,SvgElement> mElementsById;

    SvgSvg() {
        super(null, null, true, null);
        this.width = 1;
        this.height = 1;
        this.mElementsById = new HashMap<>();
    }

    public void setHolderDrawable(Drawable holderDrawable) {
        mHolderDrawable = holderDrawable;
    }

    public SvgElement getElementById(String id) {
        return mElementsById.get(id);
    }

    @Override
    public void invalidate() {
        if(mHolderDrawable != null) {
            mHolderDrawable.invalidateSelf();
        }
    }

    public void parse(InputStream is) throws IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp;
        SvgHandler svg_handler;
        try {
            sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            svg_handler=new SvgHandler();
            xr.setContentHandler(svg_handler);
            xr.parse(new InputSource(is));
        } catch (ParserConfigurationException | SAXException | IOException e1) {
            throw new IOException(e1.getMessage());
        }
    }

    private class SvgHandler extends org.xml.sax.helpers.DefaultHandler {
        private Stack<SvgGroup> mCurrentGroup;
        private SvgGradient mCurrentGradient;
        private RectF mTmpRectF = new RectF();

        public SvgHandler() {
            mCurrentGroup = new Stack<>();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            SvgElement e = null;
            if(localName.equals("svg")) {
                float width;
                float height;
                String viewBox = atts.getValue("viewBox");
                if(viewBox != null) {
                    String[] tokens = viewBox.split(" ");
                    width = Float.parseFloat(tokens[2]);
                    height = Float.parseFloat(tokens[3]);
                } else {
                    width = Float.parseFloat(atts.getValue("width"));
                    height = Float.parseFloat(atts.getValue("height"));
                }
                SvgSvg.this.width = width;
                SvgSvg.this.height = height;
                mCurrentGroup.push(SvgSvg.this);
                e = SvgSvg.this;
            } else if(localName.equals("g")) {
                Matrix t = parseTransform(atts.getValue("transform"));
                String style = atts.getValue("style");
                try {
                    SvgSvg.ParsedStyle parsedStyle = parseStyle(style);
                    SvgGroup group = new SvgGroup(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, t);
                    addElement(group);
                    mCurrentGroup.push(group);
                    e = group;
                } catch (Exception r) {
                    r.printStackTrace();
                }
            } else if(localName.equals("path")) {
                String d = atts.getValue("d");
                String style = composeStyle(atts);
                String transform = atts.getValue("transform");
                Matrix t = parseTransform(transform);
                ParsedStyle parsedStyle = parseStyle(style);
                Path path = parsePath(d);
                SvgPath svgPath = new SvgPath(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, t, path, parsedStyle.fillPaint, parsedStyle.strokePaint);
                addElement(svgPath);
                e = svgPath;
            } else if(localName.equals("rect")) {
                float x = SvgSvg.parseFloat(atts, "x", 0);
                float y = SvgSvg.parseFloat(atts, "y", 0);
                float width = SvgSvg.parseFloat(atts, "width", 0);
                float height = SvgSvg.parseFloat(atts, "height", 0);
                float rx = parseFloat(atts, "rx", 0);
                float ry = parseFloat(atts, "ry", 0);

                Path p = new Path();
                p.addRoundRect(new RectF(x, y, x + width, y + height), rx, ry, Path.Direction.CW);

                String style = composeStyle(atts);
                ParsedStyle parsedStyle = parseStyle(style);

                String transform = atts.getValue("transform");
                Matrix matrix = parseTransform(transform);

                SvgPath path = new SvgPath(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, matrix, p, parsedStyle.fillPaint, parsedStyle.strokePaint);
                addElement(path);
                e = path;
            } else if(localName.equals("ellipse") || localName.equals("circle")) {
                float centerX, centerY, radiusX, radiusY;

                centerX = SvgSvg.parseFloat(atts, "cx", 0);
                centerY = SvgSvg.parseFloat(atts, "cy", 0);
                if (localName.equals("ellipse")) {
                    radiusX = SvgSvg.parseFloat(atts, "rx", 0);
                    radiusY = SvgSvg.parseFloat(atts, "ry", 0);
                } else {
                    radiusX = radiusY = SvgSvg.parseFloat(atts, "r", 0);
                }
                mTmpRectF.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);

                Path p = new Path();
                p.addOval(mTmpRectF, Path.Direction.CW);

                String style = composeStyle(atts);
                ParsedStyle parsedStyle = parseStyle(style);

                String transform = atts.getValue("transform");
                Matrix matrix = parseTransform(transform);

                SvgPath path = new SvgPath(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, matrix, p, parsedStyle.fillPaint, parsedStyle.strokePaint);
                addElement(path);
                e = path;
            } else if(localName.equals("linearGradient")) {
                SvgLinearGradient g = new SvgLinearGradient(SvgSvg.this, getCurrentGroup());
                parseGradient(g, atts);
                g.x1 = parseFloat(atts, "x1", 0);
                g.y1 = parseFloat(atts, "y1", 0);
                g.x2 = parseFloat(atts, "x2", SvgSvg.this.width);
                g.y2 = parseFloat(atts, "y2", 0);
                mCurrentGradient = g;
                e = g;
            } else if(localName.equals("radialGradient")) {
                SvgRadialGradient g = new SvgRadialGradient(SvgSvg.this, getCurrentGroup());
                parseGradient(g, atts);
                g.cx = parseFloat(atts, "cx", SvgSvg.this.width / 2);
                g.cy = parseFloat(atts, "cy", SvgSvg.this.height / 2);
                g.r = parseFloat(atts, "r", SvgSvg.this.width / 2);
                mCurrentGradient = g;
                e = g;
            } else if(localName.equals("stop")) {
                float offset = Float.parseFloat(atts.getValue("offset"));
                String style = atts.getValue("style");
                int color = Color.BLACK;
                int alpha = 255;
                for(String token : style.split(";")) {
                    String[] elems = token.split(":");
                    String name = elems[0];
                    String value = elems[1];
                    if("stop-color".equals(name)) {
                        color = Color.parseColor(value);
                    } else if("stop-opacity".equals(name)) {
                        alpha = Math.round(Float.parseFloat(value) * 255);
                    }
                }

                color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));

                mCurrentGradient.addStopOffset(offset);
                mCurrentGradient.addStopColors(color);
            }

            if(e != null) {
                String id = atts.getValue("id");
                if(id != null) {
                    e.setId(id);
                    mElementsById.put(id, e);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(localName.equals("g")) {
                mCurrentGroup.pop();
            } else if(localName.equals("defs")) {
                for(SvgElement e : mElementsById.values()) {
                    if(e instanceof SvgGradient) {
                        SvgGradient gradient = (SvgGradient) e;
                        gradient.resolveHref(mElementsById);
                        gradient.updatePaint();
                    }
                }
            } else if(localName.equals("linearGradient") || localName.equals("radialGradient")) {
                if(mCurrentGradient.getHref() == null) {
                    mCurrentGradient.updatePaint();
                }
                mCurrentGradient = null;
            }
        }

        private SvgGroup getCurrentGroup() {
            return mCurrentGroup.lastElement();
        }

        private void addElement(SvgElement e) {
            getCurrentGroup().getChildren().add(e);
        }
    }

    /*package*/ static class ParsedStyle {
        Paint strokePaint;
        Paint fillPaint;
        boolean isVisible;

        public ParsedStyle(Paint strokePaint, Paint fillPaint, boolean isVisible) {
            this.strokePaint = strokePaint;
            this.fillPaint = fillPaint;
            this.isVisible = isVisible;
        }
    }

    // Extracted from https://github.com/Pixplicity/sharp/blob/master/library/src/main/java/com/pixplicity/sharp/Sharp.java
    /*package*/ static Path parsePath(String s) {
        int n = s.length();
        SvgParserHelper ph = new SvgParserHelper(s, 0);
        ph.skipWhitespace();
        Path p = new Path();
        float lastX = 0;
        float lastY = 0;
        float lastX1 = 0;
        float lastY1 = 0;
        float subPathStartX = 0;
        float subPathStartY = 0;
        char prevCmd = 0;
        while (ph.pos < n) {
            char cmd = s.charAt(ph.pos);
            switch (cmd) {
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (prevCmd == 'm' || prevCmd == 'M') {
                        cmd = (char) (((int) prevCmd) - 1);
                        break;
                    } else if (("lhvcsqta").indexOf(Character.toLowerCase(prevCmd)) >= 0) {
                        cmd = prevCmd;
                        break;
                    }
                default: {
                    ph.advance();
                    prevCmd = cmd;
                }
            }

            boolean wasCurve = false;
            switch (cmd) {
                case 'Z':
                case 'z': {
                    // Close path
                    p.close();
                    p.moveTo(subPathStartX, subPathStartY);
                    lastX = subPathStartX;
                    lastY = subPathStartY;
                    lastX1 = subPathStartX;
                    lastY1 = subPathStartY;
                    wasCurve = true;
                    break;
                }
                case 'M':
                case 'm': {
                    // Move
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        subPathStartX += x;
                        subPathStartY += y;
                        p.rMoveTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        subPathStartX = x;
                        subPathStartY = y;
                        p.moveTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'L':
                case 'l': {
                    // Line
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        p.rLineTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        p.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'H':
                case 'h': {
                    // Horizontal line
                    float x = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        p.rLineTo(x, 0);
                        lastX += x;
                    } else {
                        p.lineTo(x, lastY);
                        lastX = x;
                    }
                    break;
                }
                case 'V':
                case 'v': {
                    // Vertical line
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        p.rLineTo(0, y);
                        lastY += y;
                    } else {
                        p.lineTo(lastX, y);
                        lastY = y;
                    }
                    break;
                }
                case 'C':
                case 'c': {
                    // Cubic Bézier (six parameters)
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x1 += lastX;
                        x2 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y2 += lastY;
                        y += lastY;
                    }
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'S':
                case 's': {
                    // Shorthand cubic Bézier (four parameters)
                    wasCurve = true;
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x2 += lastX;
                        x += lastX;
                        y2 += lastY;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'Q':
                case 'q': {
                    // Quadratic Bézier (four parameters)
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x1 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y += lastY;
                    }
                    p.quadTo(x1, y1, x, y);
                    lastX1 = x1;
                    lastY1 = y1;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'T':
                case 't': {
                    // Shorthand quadratic Bézier (two parameters)
                    wasCurve = true;
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x += lastX;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.quadTo(x1, y1, x, y);
                    lastX1 = x1;
                    lastY1 = y1;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'A':
                case 'a': {
                    // Elliptical arc
                    float rx = ph.nextFloat();
                    float ry = ph.nextFloat();
                    float theta = ph.nextFloat();
                    int largeArc = ph.nextFlag();
                    int sweepArc = ph.nextFlag();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        x += lastX;
                        y += lastY;
                    }
                    drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
                    lastX = x;
                    lastY = y;
                    break;
                }
            }
            if (!wasCurve) {
                lastX1 = lastX;
                lastY1 = lastY;
            }
            ph.skipWhitespace();
        }
        return p;
    }

    private static float angle(float y1, float x1, float y2, float x2) {
        return (float) Math.toDegrees(Math.atan2(y1, x1) - Math.atan2(y2, x2)) % 360;
    }

    private static final RectF arcRectf = new RectF();
    private static final Matrix arcMatrix = new Matrix();
    private static final Matrix arcMatrix2 = new Matrix();

    private static void drawArc(Path p, float lastX, float lastY, float x, float y,
                                float rx, float ry, float theta, int largeArc, int sweepArc) {
        //Log.d("drawArc", "from (" + lastX + "," + lastY + ") to (" + x + ","+ y + ") r=(" + rx + "," + ry + ") theta=" + theta + " flags="+ largeArc + "," + sweepArc);

        // http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes

        if (rx == 0 || ry == 0) {
            p.lineTo(x, y);
            return;
        }

        if (x == lastX && y == lastY) {
            return; // nothing to draw
        }

        rx = Math.abs(rx);
        ry = Math.abs(ry);

        final float thrad = theta * (float) Math.PI / 180;
        final float st = (float) Math.sin(thrad);
        final float ct = (float) Math.cos(thrad);

        final float xc = (lastX - x) / 2;
        final float yc = (lastY - y) / 2;
        final float x1t = ct * xc + st * yc;
        final float y1t = -st * xc + ct * yc;

        final float x1ts = x1t * x1t;
        final float y1ts = y1t * y1t;
        float rxs = rx * rx;
        float rys = ry * ry;

        float lambda = (x1ts / rxs + y1ts / rys) * 1.001f; // add 0.1% to be sure that no out of range occurs due to limited precision
        if (lambda > 1) {
            float lambdasr = (float) Math.sqrt(lambda);
            rx *= lambdasr;
            ry *= lambdasr;
            rxs = rx * rx;
            rys = ry * ry;
        }

        final float R = (float) Math.sqrt((rxs * rys - rxs * y1ts - rys * x1ts) / (rxs * y1ts + rys * x1ts))
                * ((largeArc == sweepArc) ? -1 : 1);
        final float cxt = R * rx * y1t / ry;
        final float cyt = -R * ry * x1t / rx;
        final float cx = ct * cxt - st * cyt + (lastX + x) / 2;
        final float cy = st * cxt + ct * cyt + (lastY + y) / 2;

        final float th1 = angle(1, 0, (x1t - cxt) / rx, (y1t - cyt) / ry);
        float dth = angle((x1t - cxt) / rx, (y1t - cyt) / ry, (-x1t - cxt) / rx, (-y1t - cyt) / ry);

        if (sweepArc == 0 && dth > 0) {
            dth -= 360;
        } else if (sweepArc != 0 && dth < 0) {
            dth += 360;
        }

        // draw
        if ((theta % 360) == 0) {
            // no rotate and translate need
            arcRectf.set(cx - rx, cy - ry, cx + rx, cy + ry);
            p.arcTo(arcRectf, th1, dth);
        } else {
            // this is the hard and slow part :-)
            arcRectf.set(-rx, -ry, rx, ry);

            arcMatrix.reset();
            arcMatrix.postRotate(theta);
            arcMatrix.postTranslate(cx, cy);
            arcMatrix.invert(arcMatrix2);

            p.transform(arcMatrix2);
            p.arcTo(arcRectf, th1, dth);
            p.transform(arcMatrix);
        }
    }

    private static String composeStyle(Attributes atts) {
        String style = atts.getValue("style");
        if(style != null) {
            return style;
        }

        StringBuilder sb = new StringBuilder();
        addStyleAttribute(atts, sb, "fill");
        addStyleAttribute(atts, sb, "stroke");
        addStyleAttribute(atts, sb, "stroke-width");
        addStyleAttribute(atts, sb, "stroke-linecap");
        addStyleAttribute(atts, sb, "stroke-linejoin");
        addStyleAttribute(atts, sb, "stroke-miterlimit");
        addStyleAttribute(atts, sb, "stroke-dasharray");
        addStyleAttribute(atts, sb, "stroke-dashoffset");
        addStyleAttribute(atts, sb, "stroke-opacity");
        addStyleAttribute(atts, sb, "fill-opacity");
        addStyleAttribute(atts, sb, "display");

        return sb.toString();
    }

    private static void addStyleAttribute(Attributes atts, StringBuilder sb, String name) {
        String value = atts.getValue(name);
        if(value == null) {
            return;
        }

        if(sb.length() > 0) {
            sb.append(';');
        }
        sb.append(name).append(':').append(value);
    }

    /*package*/ ParsedStyle parseStyle(String style) {
        if(style==null || "".equals(style)) {
            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);
            return new ParsedStyle(null, fillPaint, true);
        }

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setStyle(Paint.Style.FILL);
        boolean hasFill = true;

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setStyle(Paint.Style.STROKE);
        boolean hasStroke = false;

        boolean isVisible = true;

        int fcolor = fp.getColor();
        int falpha = fp.getAlpha();
        int scolor = sp.getColor();
        int salpha = sp.getAlpha();
        float stroke_width = 1;
        float[] stroke_dash_array = null;
        float stroke_dash_offset = 0;

        if(style != null) {
            String[] tokens = style.split(";");
            for(String s : tokens) {
                int n = s.indexOf(":");
                String name = s.substring(0, n);
                String value = s.substring(n+1);
                if("fill".equals(name)) {
                    if("none".equals(value)) {
                        hasFill = false;
                    } else if(value.startsWith("url")) {
                        String id = value.substring(value.indexOf('#')+1, value.lastIndexOf(')'));
                        SvgGradient g = (SvgGradient) mElementsById.get(id);
                        // TODO make the link with element using this gradient so that updates to the gradient can be taken into account. See Gradient.addUser()
                        fp = new Paint(g.getPaint());
                        fp.setStyle(Paint.Style.FILL);
                        hasFill = true;
                        continue;
                    } else {
                        try {
                            fcolor = Color.parseColor(value);
                            hasFill = fcolor != 0;
                        } catch (IllegalArgumentException ex) { /*pass*/ }
                    }
                } else if("stroke".equals(name)) {
                    if("none".equals(value)) {
                        hasStroke = false;
                    } else if(value.startsWith("url")) {
                        String id = value.substring(value.indexOf('#')+1, value.lastIndexOf(')'));
                        SvgGradient g = (SvgGradient) mElementsById.get(id);
                        // TODO make the link with element using this gradient so that updates to the gradient can be taken into account. See Gradient.addUser()
                        sp = new Paint(g.getPaint());
                        sp.setStyle(Paint.Style.STROKE);
                        hasStroke = true;
                        continue;
                    } else {
                        try {
                            scolor = Color.parseColor(value);
                            hasStroke = scolor != 0;
                        } catch (IllegalArgumentException ex) { /*pass*/ }
                    }
                } else if("stroke-width".equals(name)) {
                    if(sp != null) {
                        int j;
                        for (j = value.length() - 1; j >= 0; j--) {
                            char c = value.charAt(j);
                            if (Character.isDigit(c) || c == '.') break;
                        }
                        stroke_width = Float.parseFloat(value.substring(0, j + 1));
                    }
                } else if("stroke-linecap".equals(name)) {
                    if (sp != null) {
                        Paint.Cap cap = sp.getStrokeCap();
                        if ("round".equals(value)) {
                            cap = Paint.Cap.ROUND;
                        } else if ("butt".equals(value)) {
                            cap = Paint.Cap.BUTT;
                        } else if ("square".equals(value)) {
                            cap = Paint.Cap.SQUARE;
                        }
                        sp.setStrokeCap(cap);
                    }
                } else if("stroke-linejoin".equals(name)) {
                    if(sp != null) {
                        Paint.Join join = sp.getStrokeJoin();
                        if ("miter".equals(value)) {
                            join = Paint.Join.MITER;
                        } else if ("round".equals(value)) {
                            join = Paint.Join.ROUND;
                        } else if ("bevel".equals(value)) {
                            join = Paint.Join.BEVEL;
                        }
                        sp.setStrokeJoin(join);
                    }
                } else if("stroke-miterlimit".equals(name)) {
                    if(sp != null) sp.setStrokeMiter(Float.parseFloat(value));
                } else if("stroke-dasharray".equals(name)) {
                    if(!"none".equals(value)) {
                        stroke_dash_array = parseFloats(value);
                    }
                } else if("stroke-dashoffset".equals(name)) {
                    stroke_dash_offset = Float.parseFloat(value);
                } else if("stroke-opacity".equals(name)) {
                    salpha = (int)(255* Float.parseFloat(value));
                } else if("fill-opacity".equals(name)) {
                    falpha = (int)(255* Float.parseFloat(value));
                } else if("display".equals(name)) {
                    isVisible = !"none".equals(value);
                }
            }
        }

        if(!hasFill) {
            fp = null;
        }

        if(fp != null) {
            fp.setColor(fcolor);
            fp.setAlpha(falpha);
        }

        if(!hasStroke) {
            sp = null;
        }

        if(sp != null) {
            sp.setColor(scolor);
            sp.setAlpha(salpha);
            sp.setStrokeWidth(stroke_width);
            if(stroke_dash_array != null) {
                sp.setPathEffect(new DashPathEffect(stroke_dash_array, stroke_dash_offset));
            }
        }

        return new ParsedStyle(sp, fp, isVisible);
    }

    private static void parseGradient(SvgGradient g, Attributes atts) {
        g.setTransform(parseTransform(atts.getValue("gradientTransform")));
        g.setHref(atts.getValue("xlink:href"));
        g.setTileMode(Shader.TileMode.CLAMP);
        String spreadMethod = atts.getValue("spreadMethod");
        if(spreadMethod != null) {
            if("repeat".equals(spreadMethod)) {
                g.setTileMode(Shader.TileMode.REPEAT);
            } else if("reflect".equals(spreadMethod)) {
                g.setTileMode(Shader.TileMode.MIRROR);
            }
        }
    }

    /*package*/ static Matrix parseTransform(String s) {
        if(s == null) {
            return null;
        }

        if (s.startsWith("matrix(")) {
            float[] floats = parseFloats(s, "matrix(");
            if (floats.length == 6) {
                Matrix matrix = new Matrix();
                matrix.setValues(new float[]{
                        // Row 1
                        floats[0],
                        floats[2],
                        floats[4],
                        // Row 2
                        floats[1],
                        floats[3],
                        floats[5],
                        // Row 3
                        0,
                        0,
                        1,
                });
                return matrix;
            }
        } else if (s.startsWith("translate(")) {
            float[] floats = parseFloats(s, "translate(");
            if (floats.length > 0) {
                float tx = floats[0];
                float ty = floats.length == 1 ? 0 : floats[1];
                Matrix matrix = new Matrix();
                matrix.postTranslate(tx, ty);
                return matrix;
            }
        } else if (s.startsWith("scale(")) {
            float[] floats = parseFloats(s, "scale(");
            if (floats.length > 0) {
                float sx = floats[0];
                float sy = floats.length == 1 ? 0 : floats[1];
                Matrix matrix = new Matrix();
                matrix.postScale(sx, sy);
                return matrix;
            }
        } else if (s.startsWith("skewX(")) {
            float[] floats = parseFloats(s, "skewX(");
            if (floats.length > 0) {
                float angle = floats[0];
                Matrix matrix = new Matrix();
                matrix.postSkew((float) Math.tan(angle), 0);
                return matrix;
            }
        } else if (s.startsWith("skewY(")) {
            float[] floats = parseFloats(s, "skewY(");
            if (floats.length > 0) {
                float angle = floats[0];
                Matrix matrix = new Matrix();
                matrix.postSkew(0, (float) Math.tan(angle));
                return matrix;
            }
        } else if (s.startsWith("rotate(")) {
            float[] floats = parseFloats(s, "rotate(");
            if (floats.length > 0) {
                float angle = floats[0];
                float cx = 0;
                float cy = 0;
                if (floats.length > 2) {
                    cx = floats[1];
                    cy = floats[2];
                }
                Matrix matrix = new Matrix();
                matrix.postTranslate(cx, cy);
                matrix.postRotate(angle);
                matrix.postTranslate(-cx, -cy);
                return matrix;
            }
        }
        return null;
    }

    private static Float parseFloat(Attributes atts, String name, float defaultValue) {
        String v = atts.getValue(name);
        return v == null ? defaultValue : Float.parseFloat(v);
    }

    private static float[] parseFloats(String list, String prefix) {
        return parseFloats(list.substring(prefix.length(), list.lastIndexOf(')')));
    }

    private static float[] parseFloats(String list) {
        String[] tokens = list.split(list.indexOf(',')==-1?" ":",");
        int l = tokens.length;
        float[] floats = new float[l];
        for(int i=0; i<l; i++) {
            floats[i] = Float.parseFloat(tokens[i]);
        }
        return floats;
    }
}
