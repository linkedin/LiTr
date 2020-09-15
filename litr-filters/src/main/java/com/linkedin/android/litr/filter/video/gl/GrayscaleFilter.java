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

public class GrayscaleFilter extends VideoFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "const highp vec3 weight = vec3(0.2125, 0.7154, 0.0721);\n" +

            "void main()\n" +
            "{\n" +
                "float luminance = dot(texture2D(sTexture, vTextureCoord).rgb, weight);\n" +
                "gl_FragColor = vec4(vec3(luminance), 1.0);\n" +
            "}";

    public GrayscaleFilter() {
        this(null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public GrayscaleFilter(@Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, null, transform);
    }
}
