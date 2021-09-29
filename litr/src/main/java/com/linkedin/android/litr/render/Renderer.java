/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.linkedin.android.litr.codec.Frame;

/**
 * Common interface for renderer
 */
public interface Renderer {

    /**
     * Initialize the renderer. Called during track transformer initialization.
     * @param outputSurface {@link Surface} to render onto, null for non OpenGL renderer
     * @param sourceMediaFormat source {@link MediaFormat}
     * @param targetMediaFormat target {@link MediaFormat}
     */
    void init(@Nullable Surface outputSurface, @Nullable MediaFormat sourceMediaFormat, @Nullable MediaFormat targetMediaFormat);

    /**
     * This should be called every time the {@link MediaCodec#INFO_OUTPUT_FORMAT_CHANGED} is returned
     * @param sourceMediaFormat source {@link MediaFormat}
     * @param targetMediaFormat target {@link MediaFormat}
     */
    void onMediaFormatChanged(@Nullable MediaFormat sourceMediaFormat, @Nullable MediaFormat targetMediaFormat);

    /**
     * Get renderer's input surface. Renderer creates it internally.
     * @return {@link Surface} to get pixels from, null for non OpenGL renderer
     */
    @Nullable Surface getInputSurface();

    /**
     * Render a frame
     * @param inputFrame {@link Frame} to operate with. Non-null ror non OpenGL renderer, will contain raw pixels.
     *                           null for GL renderer, which should assume that environment has been set and just invoke Gl calls.
     * @param presentationTimeNs frame presentation time in nanoseconds
     */
    void renderFrame(@Nullable Frame inputFrame, long presentationTimeNs);

    /**
     * Release the renderer and all it resources.
     */
    void release();

    /**
     * Check if renderer has user provided filters
     * @return true if has, false otherwise
     */
    boolean hasFilters();
}
