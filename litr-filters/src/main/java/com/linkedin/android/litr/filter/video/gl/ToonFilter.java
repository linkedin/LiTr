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
 * Frame render filter that applies cartoon-like effect
 */
public class ToonFilter extends Base3x3TextureSamplingFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +

            "uniform samplerExternalOES sTexture;\n" +

            "varying vec2 textureCoordinate;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +

            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 topLeftTextureCoordinate;\n" +
            "varying vec2 topRightTextureCoordinate;\n" +

            "varying vec2 bottomTextureCoordinate;\n" +
            "varying vec2 bottomLeftTextureCoordinate;\n" +
            "varying vec2 bottomRightTextureCoordinate;\n" +

            "uniform highp float threshold;\n" +
            "uniform highp float quantizationLevels;\n" +

            "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "vec4 textureColor = texture2D(sTexture, textureCoordinate);\n" +

                "float bottomLeftIntensity = texture2D(sTexture, bottomLeftTextureCoordinate).r;\n" +
                "float topRightIntensity = texture2D(sTexture, topRightTextureCoordinate).r;\n" +
                "float topLeftIntensity = texture2D(sTexture, topLeftTextureCoordinate).r;\n" +
                "float bottomRightIntensity = texture2D(sTexture, bottomRightTextureCoordinate).r;\n" +
                "float leftIntensity = texture2D(sTexture, leftTextureCoordinate).r;\n" +
                "float rightIntensity = texture2D(sTexture, rightTextureCoordinate).r;\n" +
                "float bottomIntensity = texture2D(sTexture, bottomTextureCoordinate).r;\n" +
                "float topIntensity = texture2D(sTexture, topTextureCoordinate).r;\n" +
                "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
                "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +

                "float mag = length(vec2(h, v));\n" +
                "vec3 posterizedImageColor = floor((textureColor.rgb * quantizationLevels) + 0.5) / quantizationLevels;\n" +
                "float thresholdTest = 1.0 - step(threshold, mag);\n" +
                "gl_FragColor = vec4(posterizedImageColor * thresholdTest, textureColor.a);\n" +
            "}";

    private float threshold;
    private float quantizationLevels;

    /**
     * Create the instance of frame render filter
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param threshold edge detection threshold
     * @param quantizationLevels number of color quantization levels
     */
    public ToonFilter(float texelWidth, float texelHeight, float threshold, float quantizationLevels) {
        super(FRAGMENT_SHADER, texelWidth, texelHeight);

        this.threshold = threshold;
        this.quantizationLevels = quantizationLevels;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param threshold edge detection threshold
     * @param quantizationLevels number of color quantization levels
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public ToonFilter(float texelWidth, float texelHeight, float threshold, float quantizationLevels, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(FRAGMENT_SHADER, texelWidth, texelHeight, size, position, rotation);

        this.threshold = threshold;
        this.quantizationLevels = quantizationLevels;
    }

    @Override
    protected void applyCustomGlAttributes() {
        super.applyCustomGlAttributes();
        GLES20.glUniform1f(getHandle("threshold"), threshold);
        GLES20.glUniform1f(getHandle("quantizationLevels"), quantizationLevels);
    }
}
