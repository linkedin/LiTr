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

public class CgaColorspaceFilter extends BaseFrameRenderFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +

            "precision mediump float;\n" +

            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +

            "void main()\n" +
            "{\n" +
            "highp vec2 sampleDivisor = vec2(1.0 / 200.0, 1.0 / 320.0);\n" +

            "highp vec2 samplePos = vTextureCoord - mod(vTextureCoord, sampleDivisor);\n" +
            "highp vec4 color = texture2D(sTexture, samplePos);\n" +

            "mediump vec4 colorCyan = vec4(85.0 / 255.0, 1.0, 1.0, 1.0);\n" +
            "mediump vec4 colorMagenta = vec4(1.0, 85.0 / 255.0, 1.0, 1.0);\n" +
            "mediump vec4 colorWhite = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "mediump vec4 colorBlack = vec4(0.0, 0.0, 0.0, 1.0);\n" +

            "mediump vec4 endColor;\n" +
            "highp float blackDistance = distance(color, colorBlack);\n" +
            "highp float whiteDistance = distance(color, colorWhite);\n" +
            "highp float magentaDistance = distance(color, colorMagenta);\n" +
            "highp float cyanDistance = distance(color, colorCyan);\n" +

            "mediump vec4 finalColor;\n" +

            "highp float colorDistance = min(magentaDistance, cyanDistance);\n" +
            "colorDistance = min(colorDistance, whiteDistance);\n" +
            "colorDistance = min(colorDistance, blackDistance);\n" +

            "if (colorDistance == blackDistance)\n" +
            "{\n" +
                "finalColor = colorBlack;\n" +
            "}\n" +
            "else if (colorDistance == whiteDistance)\n" +
            "{\n" +
                "finalColor = colorWhite;\n" +
            "}\n" +
            "else if (colorDistance == cyanDistance)\n" +
            "{\n" +
                "finalColor = colorCyan;\n" +
            "}\n" +
            "else\n" +
            "{\n" +
                "finalColor = colorMagenta;\n" +
            "}\n" +

            "gl_FragColor = finalColor;\n" +
            "}";

    public CgaColorspaceFilter() {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public CgaColorspaceFilter(@NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER, size, position, rotation);
    }

    @Override
    protected void applyCustomGlAttributes() {
        // no need to do anything here
    }
}
