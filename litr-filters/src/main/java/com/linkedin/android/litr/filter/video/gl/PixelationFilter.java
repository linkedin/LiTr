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

import static android.opengl.GLES20.glUniform1f;

/**
 * Frame render filter that applies pixelation effect to video frame
 */
public class PixelationFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +

            "varying highp vec2 vTextureCoord;\n" +

            "uniform float imageWidthFactor;\n" +
            "uniform float imageHeightFactor;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform float pixelSize;\n" +

            "void main()\n" +
            "{\n" +
                "vec2 uv  = vTextureCoord.xy;\n" +
                "float dx = pixelSize * imageWidthFactor;\n" +
                "float dy = pixelSize * imageHeightFactor;\n" +
                "vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));\n" +
                "vec3 tc = texture2D(sTexture, coord).xyz;\n" +
                "gl_FragColor = vec4(tc, 1.0);\n" +
            "}";

    private float imageWidthFactor;
    private float imageHeightFactor;
    private float pixelSize;

    /**
     * Create the instance of frame render filter
     * @param imageWidthFactor width pixelation size, as a factor of frame width
     * @param imageHeightFactor height pixelation size, as a factor of frame height
     * @param pixelSize pixel size
     */
    public PixelationFilter(float imageWidthFactor, float imageHeightFactor, float pixelSize) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.imageWidthFactor = imageWidthFactor;
        this.imageHeightFactor = imageHeightFactor;
        this.pixelSize = pixelSize;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param imageWidthFactor relative width of an image
     * @param imageHeightFactor relative height of an image
     * @param pixelSize pixel size
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public PixelationFilter(float imageWidthFactor, float imageHeightFactor, float pixelSize, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.imageWidthFactor = imageWidthFactor;
        this.imageHeightFactor = imageHeightFactor;
        this.pixelSize = pixelSize;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("imageWidthFactor"), imageWidthFactor);
        GLES20.glUniform1f(getHandle("imageHeightFactor"), imageHeightFactor);
        glUniform1f(getHandle("pixelSize"), pixelSize);
    }
}
