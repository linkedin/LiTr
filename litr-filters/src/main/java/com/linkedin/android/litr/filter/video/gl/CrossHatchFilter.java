/*
 * Copyright 2018 Masayuki Suda
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;
import android.opengl.GLES20;

import androidx.annotation.NonNull;

/**
 * Frame render filter that converts video frame into a "cross hatch" rendering
 */
public class CrossHatchFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float crossHatchSpacing;\n" +
            "uniform highp float lineWidth;\n" +
            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "highp float luminance = dot(texture2D(sTexture, vTextureCoord).rgb, W);\n" +
                "lowp vec4 colorToDisplay = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "if (luminance < 1.00)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x + vTextureCoord.y, crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "if (luminance < 0.75)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x - vTextureCoord.y, crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "if (luminance < 0.50)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x + vTextureCoord.y - (crossHatchSpacing / 2.0), crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "if (luminance < 0.3)\n" +
                "{\n" +
                    "if (mod(vTextureCoord.x - vTextureCoord.y - (crossHatchSpacing / 2.0), crossHatchSpacing) <= lineWidth)\n" +
                    "{\n" +
                        "colorToDisplay = vec4(0.0, 0.0, 0.0, 1.0);\n" +
                    "}\n" +
                "}\n" +
                "gl_FragColor = colorToDisplay;\n" +
            "}";

    private float crossHatchSpacing;
    private float lineWidth;

    /**
     * Create cross hatch filter
     * @param crossHatchSpacing spacing between cross hatches, in relative coordinates
     * @param lineWidth thickness of cross hatch line, in relative coordinates
     */
    public CrossHatchFilter(float crossHatchSpacing, float lineWidth) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.crossHatchSpacing = crossHatchSpacing;
        this.lineWidth = lineWidth;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param crossHatchSpacing spacing between cross hatches, in relative coordinates
     * @param lineWidth thickness of cross hatch line, in relative coordinates
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public CrossHatchFilter(float crossHatchSpacing, float lineWidth, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.crossHatchSpacing = crossHatchSpacing;
        this.lineWidth = lineWidth;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("crossHatchSpacing"), crossHatchSpacing);
        GLES20.glUniform1f(getHandle("lineWidth"), lineWidth);
    }
}
