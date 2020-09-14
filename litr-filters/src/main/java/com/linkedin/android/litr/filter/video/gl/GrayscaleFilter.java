/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.PointF;

import androidx.annotation.NonNull;

public class GrayscaleFilter extends BaseFrameRenderFilter {

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
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public GrayscaleFilter(@NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);
    }

    @Override
    protected void applyCustomGlAttributes() {
        // no need to do anything here
    }
}
