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
 * Frame render filter that applies sharpening effect to video frame
 */
public class SharpenFilter extends VideoFrameRenderFilter {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +

            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +

            "uniform float texelWidth;\n" +
            "uniform float texelHeight;\n" +
            "uniform float sharpness;\n" +

            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 leftTextureCoordinate;\n" +
            "varying highp vec2 rightTextureCoordinate;\n" +
            "varying highp vec2 topTextureCoordinate;\n" +
            "varying highp vec2 bottomTextureCoordinate;\n" +

            "varying float centerMultiplier;\n" +
            "varying float edgeMultiplier;\n" +

            "void main()\n" +
            "{\n" +
                "gl_Position = uMVPMatrix * aPosition;\n" +

                "mediump vec2 widthStep = vec2(texelWidth, 0.0);\n" +
                "mediump vec2 heightStep = vec2(0.0, texelHeight);\n" +

                "textureCoordinate = (uSTMatrix * aTextureCoord).xy;\n" +
                "leftTextureCoordinate = textureCoordinate - widthStep;\n" +
                "rightTextureCoordinate = textureCoordinate + widthStep;\n" +
                "topTextureCoordinate = textureCoordinate + heightStep;\n" +
                "bottomTextureCoordinate = textureCoordinate - heightStep;\n" +

                "centerMultiplier = 1.0 + 4.0 * sharpness;\n" +
                "edgeMultiplier = sharpness;\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +

            "uniform samplerExternalOES sTexture;\n" +

            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 leftTextureCoordinate;\n" +
            "varying highp vec2 rightTextureCoordinate;\n" +
            "varying highp vec2 topTextureCoordinate;\n" +
            "varying highp vec2 bottomTextureCoordinate;\n" +

            "varying float centerMultiplier;\n" +
            "varying float edgeMultiplier;\n" +

            "void main()\n" +
            "{\n" +
                "mediump vec3 textureColor = texture2D(sTexture, textureCoordinate).rgb;\n" +
                "mediump vec3 leftTextureColor = texture2D(sTexture, leftTextureCoordinate).rgb;\n" +
                "mediump vec3 rightTextureColor = texture2D(sTexture, rightTextureCoordinate).rgb;\n" +
                "mediump vec3 topTextureColor = texture2D(sTexture, topTextureCoordinate).rgb;\n" +
                "mediump vec3 bottomTextureColor = texture2D(sTexture, bottomTextureCoordinate).rgb;\n" +

                "gl_FragColor = vec4((textureColor * centerMultiplier - (leftTextureColor * edgeMultiplier + rightTextureColor * edgeMultiplier + topTextureColor * edgeMultiplier + bottomTextureColor * edgeMultiplier)), texture2D(sTexture, bottomTextureCoordinate).w);\n" +
            "}";

    /**
     * Create frame render filter
     * @param texelWidth sharpness texel width, in relative coordinates
     * @param texelHeight sharpness texel height, in relative coordinates
     * @param sharpness sharpness level
     */
    public SharpenFilter(float texelWidth, float texelHeight, float sharpness) {
        this(texelWidth, texelHeight, sharpness, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth sharpness texel width, in relative coordinates
     * @param texelHeight sharpness texel height, in relative coordinates
     * @param sharpness sharpness level
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public SharpenFilter(float texelWidth, float texelHeight, float sharpness, @Nullable Transform transform) {
        super(VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidth", texelWidth),
                        new Uniform1f("texelHeight", texelHeight),
                        new Uniform1f("sharpness", sharpness)
                },
                transform);
    }
}
