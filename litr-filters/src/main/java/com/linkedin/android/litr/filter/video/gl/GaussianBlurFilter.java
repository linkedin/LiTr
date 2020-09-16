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

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;

/**
 * Frame render filter that applies a Gaussian blur distortion to video frame
 */
public class GaussianBlurFilter extends VideoFrameRenderFilter {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +

            "uniform highp float texelWidthOffset;\n" +
            "uniform highp float texelHeightOffset;\n" +
            "uniform highp float blurSize;\n" +

            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +

            "const lowp int GAUSSIAN_SAMPLES = 9;\n" +

            "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +

            "void main()\n" +
            "{\n" +
                "gl_Position = uMVPMatrix * aPosition;\n" +
                "vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +

                // Calculate the positions for the blur
                "int multiplier = 0;\n" +
                "highp vec2 blurStep;\n" +
                "highp vec2 singleStepOffset = vec2(texelHeightOffset, texelWidthOffset) * blurSize;\n" +

                "for (lowp int i = 0; i < GAUSSIAN_SAMPLES; i++)\n" +
                "{\n" +
                    "multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n" +
                    // Blur in x (horizontal)
                    "blurStep = float(multiplier) * singleStepOffset;\n" +
                    "blurCoordinates[i] = vTextureCoord.xy + blurStep;\n" +
                "}\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +

            "const lowp int GAUSSIAN_SAMPLES = 9;\n" +

            "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
            "uniform samplerExternalOES sTexture;\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 sum = vec4(0.0);\n" +

                "sum += texture2D(sTexture, blurCoordinates[0]) * 0.05;\n" +
                "sum += texture2D(sTexture, blurCoordinates[1]) * 0.09;\n" +
                "sum += texture2D(sTexture, blurCoordinates[2]) * 0.12;\n" +
                "sum += texture2D(sTexture, blurCoordinates[3]) * 0.15;\n" +
                "sum += texture2D(sTexture, blurCoordinates[4]) * 0.18;\n" +
                "sum += texture2D(sTexture, blurCoordinates[5]) * 0.15;\n" +
                "sum += texture2D(sTexture, blurCoordinates[6]) * 0.12;\n" +
                "sum += texture2D(sTexture, blurCoordinates[7]) * 0.09;\n" +
                "sum += texture2D(sTexture, blurCoordinates[8]) * 0.05;\n" +

                "gl_FragColor = sum;\n" +
            "}";

    /**
     * Create frame render filter
     * @param texelWidthOffset blur texel width offset
     * @param texelHeightOffset blur texel height offset
     * @param blurSize blur size
     */
    public GaussianBlurFilter(float texelWidthOffset, float texelHeightOffset, float blurSize) {
        this(texelWidthOffset, texelHeightOffset, blurSize, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidthOffset blur texel width offset
     * @param texelHeightOffset blur texel height offset
     * @param blurSize blur size
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public GaussianBlurFilter(float texelWidthOffset, float texelHeightOffset, float blurSize, @Nullable Transform transform) {
        super(VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidthOffset", texelWidthOffset),
                        new Uniform1f("texelHeightOffset", texelHeightOffset),
                        new Uniform1f("blurSize", blurSize)
                },
                transform);
    }

}
