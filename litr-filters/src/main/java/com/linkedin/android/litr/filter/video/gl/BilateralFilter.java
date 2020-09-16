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
 * Bilateral smoothing filter
 */
public class BilateralFilter extends VideoFrameRenderFilter {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +

            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +

            "const lowp int GAUSSIAN_SAMPLES = 9;\n" +

            "uniform highp float texelWidth;\n" +
            "uniform highp float texelHeight;\n" +
            "uniform highp float blurSize;\n" +

            "varying highp vec2 vTextureCoord;\n" +
            "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +

            "void main()\n" +
            "{\n" +
                "gl_Position = uMVPMatrix * aPosition;\n" +
                "vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +

                // Calculate the positions for the blur
                "int multiplier = 0;\n" +
                "highp vec2 blurStep;\n" +
                "highp vec2 singleStepOffset = vec2(texelHeight, texelWidth) * blurSize;\n" +

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

            "uniform samplerExternalOES sTexture;\n" +

            "const lowp int GAUSSIAN_SAMPLES = 9;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +

            "const mediump float distanceNormalizationFactor = 1.5;\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 centralColor = texture2D(sTexture, blurCoordinates[4]);\n" +
                "lowp float gaussianWeightTotal = 0.18;\n" +
                "lowp vec4 sum = centralColor * 0.18;\n" +

                "lowp vec4 sampleColor = texture2D(sTexture, blurCoordinates[0]);\n" +
                "lowp float distanceFromCentralColor;\n" +

                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +

                "lowp float gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[1]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[2]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[3]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[5]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[6]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[7]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "sampleColor = texture2D(sTexture, blurCoordinates[8]);\n" +
                "distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n" +
                "gaussianWeightTotal += gaussianWeight;\n" +
                "sum += sampleColor * gaussianWeight;\n" +

                "gl_FragColor = sum / gaussianWeightTotal;\n" +
            "}";

    /**
     * Create the instance of frame render filter
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param blurSize blur size
     */
    public BilateralFilter(float texelWidth, float texelHeight, float blurSize) {
        this(texelWidth, texelHeight, blurSize, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param blurSize blur size
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public BilateralFilter(float texelWidth, float texelHeight, float blurSize, @Nullable Transform transform) {
        super(VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidth", texelWidth),
                        new Uniform1f("texelHeight", texelHeight),
                        new Uniform1f("blurSize", blurSize)
                },
                transform);
    }
}
