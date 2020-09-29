/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.parameter.ShaderParameter;
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform1f;
import com.linkedin.android.litr.filter.video.gl.shader.VertexShader;


public class LocalBinaryPatternFilter extends VideoFrameRenderFilter{

    private static final String LBP_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

                    "precision highp float;\n" +

                    "uniform samplerExternalOES sTexture;\n" +
                    "\n"+
                    "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec2 leftTextureCoordinate;\n" +
                    "varying highp vec2 rightTextureCoordinate;\n" +
                    "\n" +
                    "varying highp vec2 topTextureCoordinate;\n" +
                    "varying highp vec2 topLeftTextureCoordinate;\n" +
                    "varying highp vec2 topRightTextureCoordinate;\n" +
                    "\n" +
                    "varying highp vec2 bottomTextureCoordinate;\n" +
                    "varying highp vec2 bottomLeftTextureCoordinate;\n" +
                    "varying highp vec2 bottomRightTextureCoordinate;\n" +
                    "\n" +
                    "float dotProduct(mat3 v, mat3 t) {\n" +
                        "float value = 0.0;\n" +
                        "for (int i = 0; i < 3; i++) {\n" +
                            "for (int j = 0; j < 3; j++) {\n" +
                                "value += v[i][j] * t[i][j];\n" +
                            "}\n" +
                        "}\n" +
                        "return value;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                        "float c = texture2D(sTexture, textureCoordinate).g;\n" +
                        "float tl = step(c, texture2D(sTexture, topLeftTextureCoordinate).g);\n" +
                        "float t = step(c, texture2D(sTexture, topTextureCoordinate).g);\n" +
                        "float tr = step(c, texture2D(sTexture, topRightTextureCoordinate).g);\n" +
                        "float l = step(c, texture2D(sTexture, leftTextureCoordinate).g);\n" +
                        "float r = step(c, texture2D(sTexture, rightTextureCoordinate).g);\n" +
                        "float bl = step(c, texture2D(sTexture, bottomLeftTextureCoordinate).g);\n" +
                        "float b = step(c, texture2D(sTexture, bottomTextureCoordinate).g);\n" +
                        "float br = step(c, texture2D(sTexture, bottomRightTextureCoordinate).g);\n" +
                        "\n" +
                        "mat3 threshold = mat3(tl, t, tr, l, c, r, bl, b, br);\n" +
                        "mat3 weight = mat3(4.0, 2.0, 1.0, 8.0, 0.0, 128.0, 16.0, 32.0, 64.0) * 1.0/255.0;\n" +
                        "float conv = dotProduct(threshold, weight);\n" +
                        "gl_FragColor = vec4(vec3(conv), 1.0);\n" +
                    "}\n";

    /**
     * Create the instance of frame render filter
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     */
    public LocalBinaryPatternFilter(float texelWidth, float texelHeight) {
        this(texelWidth, texelHeight, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param texelWidth relative width of a texel
     * @param texelHeight relative height of a texel
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public LocalBinaryPatternFilter(float texelWidth, float texelHeight, @Nullable Transform transform) {
        super(VertexShader.THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER,
                LBP_FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("texelWidth", texelWidth),
                        new Uniform1f("texelHeight", texelHeight),
                },
                transform);
    }

}
