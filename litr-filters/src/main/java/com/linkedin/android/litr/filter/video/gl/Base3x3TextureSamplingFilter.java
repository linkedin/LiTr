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
 * Base class for all 3x3 texture sampling filters. Sets up a common vertex shader and texel size attributes.
 */
public abstract class Base3x3TextureSamplingFilter extends BaseFrameRenderFilter {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +

            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +

            "uniform highp float texelWidth;\n" +
            "uniform highp float texelHeight;\n" +

            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 leftTextureCoordinate;\n" +
            "varying highp vec2 rightTextureCoordinate;\n" +

            "varying highp vec2 topTextureCoordinate;\n" +
            "varying highp vec2 topLeftTextureCoordinate;\n" +
            "varying highp vec2 topRightTextureCoordinate;\n" +

            "varying highp vec2 bottomTextureCoordinate;\n" +
            "varying highp vec2 bottomLeftTextureCoordinate;\n" +
            "varying highp vec2 bottomRightTextureCoordinate;\n" +

            "void main()\n" +
            "{\n" +
                "gl_Position = uMVPMatrix * aPosition;\n" +

                "vec2 widthStep = vec2(texelWidth, 0.0);\n" +
                "vec2 heightStep = vec2(0.0, texelHeight);\n" +
                "vec2 widthHeightStep = vec2(texelWidth, texelHeight);\n" +
                "vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);\n" +

                "textureCoordinate = (uSTMatrix * aTextureCoord).xy;\n" +
                "leftTextureCoordinate = textureCoordinate - widthStep;\n" +
                "rightTextureCoordinate = textureCoordinate + widthStep;\n" +

                "topTextureCoordinate = textureCoordinate - heightStep;\n" +
                "topLeftTextureCoordinate = textureCoordinate - widthHeightStep;\n" +
                "topRightTextureCoordinate = textureCoordinate + widthNegativeHeightStep;\n" +

                "bottomTextureCoordinate = textureCoordinate + heightStep;\n" +
                "bottomLeftTextureCoordinate = textureCoordinate - widthNegativeHeightStep;\n" +
                "bottomRightTextureCoordinate = textureCoordinate + widthHeightStep;\n" +
            "}";

    private float texelWidth;
    private float texelHeight;

    /**
     * Create the instance of frame render filter
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     */
    public Base3x3TextureSamplingFilter(@NonNull String fragmentShader, float texelWidth, float texelHeight) {
        super(VERTEX_SHADER, fragmentShader);

        this.texelWidth = texelWidth;
        this.texelHeight = texelHeight;
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public Base3x3TextureSamplingFilter(@NonNull String fragmentShader, float texelWidth, float texelHeight, @NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, fragmentShader, size, position, rotation);

        this.texelWidth = texelWidth;
        this.texelHeight = texelHeight;
    }

    @Override
    protected void applyCustomGlAttributes() {
        GLES20.glUniform1f(getHandle("texelWidth"), texelWidth);
        GLES20.glUniform1f(getHandle("texelHeight"), texelHeight);
    }
}
