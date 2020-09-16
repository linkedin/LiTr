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
 * Frame render filter that performs weak pixel inclusion effect
 */
public class WeakPixelInclusionFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision lowp float;\n" +

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

            "void main()\n" +
            "{\n" +
                "float bottomLeftIntensity = texture2D(sTexture, bottomLeftTextureCoordinate).r;\n" +
                "float topRightIntensity = texture2D(sTexture, topRightTextureCoordinate).r;\n" +
                "float topLeftIntensity = texture2D(sTexture, topLeftTextureCoordinate).r;\n" +
                "float bottomRightIntensity = texture2D(sTexture, bottomRightTextureCoordinate).r;\n" +
                "float leftIntensity = texture2D(sTexture, leftTextureCoordinate).r;\n" +
                "float rightIntensity = texture2D(sTexture, rightTextureCoordinate).r;\n" +
                "float bottomIntensity = texture2D(sTexture, bottomTextureCoordinate).r;\n" +
                "float topIntensity = texture2D(sTexture, topTextureCoordinate).r;\n" +
                "float centerIntensity = texture2D(sTexture, textureCoordinate).r;\n" +

                "float pixelIntensitySum = bottomLeftIntensity + topRightIntensity + topLeftIntensity + bottomRightIntensity + leftIntensity + rightIntensity + bottomIntensity + topIntensity + centerIntensity;\n" +
                "float sumTest = step(1.5, pixelIntensitySum);\n" +
                "float pixelTest = step(0.01, centerIntensity);\n" +

                "gl_FragColor = vec4(vec3(sumTest * pixelTest), 1.0);\n" +
            "}";

    /**
     * Create the instance of frame render filter
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     */
    public WeakPixelInclusionFilter(float texelWidth, float texelHeight) {
        this(texelWidth, texelHeight, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public WeakPixelInclusionFilter(float texelWidth, float texelHeight, @Nullable Transform transform) {
        super(VertexShader.THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidth", texelWidth),
                        new Uniform1f("texelHeight", texelHeight)
                },
                transform);
    }
}
