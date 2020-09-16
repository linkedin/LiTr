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
import com.linkedin.android.litr.filter.video.gl.parameter.Uniform3fv;

import java.nio.FloatBuffer;

public class ColorMonochromeFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform lowp samplerExternalOES sTexture;\n" +
            "uniform lowp vec3 newColor;\n" +
            "uniform lowp float intensity;\n" +
            "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
                "float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
                "lowp vec4 desat = vec4(vec3(luminance), 1.0);\n" +
                "lowp vec4 outputColor = vec4(\n" +
                "(desat.r < 0.5 ? (2.0 * desat.r * newColor.r) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - newColor.r))),\n" +
                "(desat.g < 0.5 ? (2.0 * desat.g * newColor.g) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - newColor.g))),\n" +
                "(desat.b < 0.5 ? (2.0 * desat.b * newColor.b) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - newColor.b))),\n" +
                "1.0\n" +
                ");\n" +
                "gl_FragColor = vec4(mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);\n" +
            "}";

    /**
     * Create the instance frame render filter
     *
     * @param inputColorRGB contains the color information (i.e. r,g,b) from 0.0 to 1.0
     * @param intensity     value, from range 0.0 to 1.0;
     */
    public ColorMonochromeFilter(float[] inputColorRGB, float intensity) {
        this(inputColorRGB, intensity, null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     *
     * @param inputColorRGB contains the color information (i.e. r,g,b) from 0.0 to 1.0
     * @param intensity     value, from range 0.0 to 1.0;
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public ColorMonochromeFilter(float[] inputColorRGB, float intensity, @Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                FRAGMENT_SHADER,
                new ShaderParameter[] {
                        new Uniform1f("intensity", intensity),
                        new Uniform3fv("newColor", 1, FloatBuffer.wrap(inputColorRGB))
                },
                transform);
    }
}
