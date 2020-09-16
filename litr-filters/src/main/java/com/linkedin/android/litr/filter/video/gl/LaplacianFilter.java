/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.litr.filter.video.gl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;
import com.linkedin.android.litr.filter.video.gl.parameter.UniformMatrix3fv;
import com.linkedin.android.litr.filter.video.gl.shader.VertexShader;

/**
 * Laplacian transformation
 */
public class LaplacianFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision highp float;\n" +

            "uniform samplerExternalOES sTexture;\n" +

            "uniform mediump mat3 convolutionMatrix;\n" +

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
                "mediump vec3 bottomColor = texture2D(sTexture, bottomTextureCoordinate).rgb;\n" +
                "mediump vec3 bottomLeftColor = texture2D(sTexture, bottomLeftTextureCoordinate).rgb;\n" +
                "mediump vec3 bottomRightColor = texture2D(sTexture, bottomRightTextureCoordinate).rgb;\n" +
                "mediump vec4 centerColor = texture2D(sTexture, textureCoordinate);\n" +
                "mediump vec3 leftColor = texture2D(sTexture, leftTextureCoordinate).rgb;\n" +
                "mediump vec3 rightColor = texture2D(sTexture, rightTextureCoordinate).rgb;\n" +
                "mediump vec3 topColor = texture2D(sTexture, topTextureCoordinate).rgb;\n" +
                "mediump vec3 topRightColor = texture2D(sTexture, topRightTextureCoordinate).rgb;\n" +
                "mediump vec3 topLeftColor = texture2D(sTexture, topLeftTextureCoordinate).rgb;\n" +

                "mediump vec3 resultColor = topLeftColor * convolutionMatrix[0][0] + topColor * convolutionMatrix[0][1] + topRightColor * convolutionMatrix[0][2];\n" +
                "resultColor += leftColor * convolutionMatrix[1][0] + centerColor.rgb * convolutionMatrix[1][1] + rightColor * convolutionMatrix[1][2];\n" +
                "resultColor += bottomLeftColor * convolutionMatrix[2][0] + bottomColor * convolutionMatrix[2][1] + bottomRightColor * convolutionMatrix[2][2];\n" +

                "// Normalize the results to allow for negative gradients in the 0.0-1.0 colorspace\n" +
                "resultColor = resultColor + 0.5;\n" +

                "gl_FragColor = vec4(resultColor, centerColor.a);\n" +
            "}";

    /**
     * Create the instance of frame render filter
     * @param convolutionMatrix 3x3 convolution matrix
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     */
    public LaplacianFilter(@NonNull float[] convolutionMatrix, float texelWidth, float texelHeight) {
        this(convolutionMatrix, texelWidth, texelHeight, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param convolutionMatrix 3x3 convolution matrix
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public LaplacianFilter(@NonNull float[] convolutionMatrix, float texelWidth, float texelHeight, @Nullable Transform transform) {
        super(VertexShader.THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidth", texelWidth),
                        new Uniform1f("texelHeight", texelHeight),
                        new UniformMatrix3fv("convolutionMatrix", 1, false, convolutionMatrix, 0)
                },
                transform);
    }
}
