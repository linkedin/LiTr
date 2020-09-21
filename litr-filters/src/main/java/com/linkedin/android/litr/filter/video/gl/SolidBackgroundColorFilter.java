/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.Color;
import android.opengl.GLES20;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.linkedin.android.litr.filter.GlFilter;

/**
 * Filter to set video background to solid color
 */
public class SolidBackgroundColorFilter implements GlFilter {

    private final float red;
    private final float green;
    private final float blue;

    /**
     * Create using Android integer RGBA color format
     * @param color color value
     */
    public SolidBackgroundColorFilter(@ColorInt int color) {
        this.red = Color.red(color) / 255f;
        this.green = Color.green(color) / 255f;
        this.blue = Color.blue(color) / 255f;
    }

    /**
     * Create using OpenGL color format
     * @param red red value
     * @param green green value
     * @param blue blue value
     */
    public SolidBackgroundColorFilter(@FloatRange(from = 0, to = 1) float red,
                                      @FloatRange(from = 0, to = 1) float green,
                                      @FloatRange(from = 0, to = 1) float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public void init() {}

    @Override
    public void setVpMatrix(@NonNull float[] vpMatrix, int vpMatrixOffset) {}

    @Override
    public void apply(long presentationTimeNs) {
        GLES20.glClearColor(red, green, blue, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void release() {}
}
