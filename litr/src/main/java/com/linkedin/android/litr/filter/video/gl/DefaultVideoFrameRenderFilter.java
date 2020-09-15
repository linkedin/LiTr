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

/**
 * Most basic frame render filter that doesn't modify video pixels but can modify placement of a source frame within a target frame
 */
public class DefaultVideoFrameRenderFilter extends VideoFrameRenderFilter {

    /**
     * Create most basic filter, which scales source frame to fit target frame, with no pixel modification.
     */
    public DefaultVideoFrameRenderFilter() {
        this(null);
    }

    /**
     * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
     * No pixel data is modified.
     * @param transform {@link Transform} that defines positioning of source video frame within target video frame
     */
    public DefaultVideoFrameRenderFilter(@Nullable Transform transform) {
        super(DEFAULT_VERTEX_SHADER,
                DEFAULT_FRAGMENT_SHADER,
                null,
                transform);
    }
}
