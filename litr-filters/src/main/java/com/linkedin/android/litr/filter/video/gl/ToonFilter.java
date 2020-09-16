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
import com.linkedin.android.litr.filter.video.gl.shader.VertexShader;

/**
 * Frame render filter that applies cartoon-like effect
 */
public class ToonFilter extends VideoFrameRenderFilter {

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

    /**
     * Create the instance of frame render filter
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param threshold edge detection threshold
     * @param quantizationLevels number of color quantization levels
     */
    public ToonFilter(float texelWidth, float texelHeight, float threshold, float quantizationLevels) {
        this(texelWidth, texelHeight, threshold, quantizationLevels, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param threshold edge detection threshold
     * @param quantizationLevels number of color quantization levels
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public ToonFilter(float texelWidth, float texelHeight, float threshold, float quantizationLevels, @Nullable Transform transform) {
        super(VertexShader.THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidth", texelWidth),
                        new Uniform1f("texelHeight", texelHeight),
                        new Uniform1f("threshold", threshold),
                        new Uniform1f("quantizationLevels", quantizationLevels)
                },
                transform);
    }
}
