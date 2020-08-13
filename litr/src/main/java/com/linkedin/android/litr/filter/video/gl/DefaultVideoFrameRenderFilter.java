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

/**
 * Most basic frame render filter that doesn't modify video pixels but can modify placement of a source frame within a target frame
 */
public class DefaultVideoFrameRenderFilter extends BaseFrameRenderFilter {

    /**
     * Create most basic filter, which scales source frame to fit target frame, with no pixel modification.
     */
    public DefaultVideoFrameRenderFilter() {
        super(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * No pixel data is modified.
     * @param size size in X and Y direction, relative to target video frame
     * @param position position of source video frame  center, in relative coordinate in 0 - 1 range
     *                 in fourth quadrant (0,0 is top left corner)
     * @param rotation rotation angle of overlay, relative to target video frame, counter-clockwise, in degrees
     */
    public DefaultVideoFrameRenderFilter(@NonNull PointF size, @NonNull PointF position, float rotation) {
        super(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER, size, position, rotation);
    }

    @Override
    protected void applyCustomGlAttributes() {
        // default implementation does nothing
    }
}
