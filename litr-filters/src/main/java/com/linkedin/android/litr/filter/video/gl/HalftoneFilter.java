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
 * Frame render filter that applies halftone effect
 */
public class HalftoneFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;" +
            " varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform highp float fractionalPixelWidth;\n" +
            "uniform highp float aspectRatio;\n" +
            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "highp vec2 sampleDivisor = vec2(fractionalPixelWidth, fractionalPixelWidth / aspectRatio);\n" +
                "highp vec2 samplePos = vTextureCoord - mod(vTextureCoord, sampleDivisor) + 0.5 * sampleDivisor;\n" +
                "highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "highp vec2 adjustedSamplePos = vec2(samplePos.x, (samplePos.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "highp float distanceFromSamplePoint = distance(adjustedSamplePos, textureCoordinateToUse);\n" +
                "lowp vec3 sampledColor = texture2D(sTexture, samplePos).rgb;\n" +
                "highp float dotScaling = 1.0 - dot(sampledColor, W);\n" +
                "lowp float checkForPresenceWithinDot = 1.0 - step(distanceFromSamplePoint, (fractionalPixelWidth * 0.5) * dotScaling);\n" +
                "gl_FragColor = vec4(vec3(checkForPresenceWithinDot), 1.0);\n" +
            "}";

    private float fractionalPixelWidth;
    private float aspectRatio;

    /**
     * Create the instance of frame render filter
     * @param fractionalPixelWidth width of fractional pixel
     * @param aspectRatio aspect ratio of a pixel
     */
    public HalftoneFilter(float fractionalPixelWidth, float aspectRatio) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        this.fractionalPixelWidth = fractionalPixelWidth;
        this.aspectRatio = aspectRatio;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param fractionalPixelWidth width of fractional pixel
     * @param aspectRatio aspect ratio of a pixel
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public HalftoneFilter(float fractionalPixelWidth, float aspectRatio, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);

        this.fractionalPixelWidth = fractionalPixelWidth;
        this.aspectRatio = aspectRatio;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("fractionalPixelWidth"), fractionalPixelWidth);
        GLES20.glUniform1f(getHandle("aspectRatio"), aspectRatio);
    }
}
