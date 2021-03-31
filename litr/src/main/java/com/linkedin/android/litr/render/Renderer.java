/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render;

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
     * Get renderer's input surface. Renderer creates it internally.
     * @return {@link Surface} to get pixels from, null for non OpenGL renderer
     */
    @Nullable Surface getInputSurface();

    /**
     * Render an input frame
     * @param inputFrame {@link Frame} to operate with. Non-null ror non OpenGL renderer, will contain raw pixels.
     *                           null for GL renderer, which should assume that environment has been set and just invoke Gl calls.
     * @param presentationTimeNs frame presentation time in nanoseconds
     * @param sourceMediaFormat the raw(source) {@link MediaFormat}
     * @param targetMediaFormat the target {@link MediaFormat}
     */
    void renderFrame(@Nullable Frame inputFrame, long presentationTimeNs, MediaFormat sourceMediaFormat,
            MediaFormat targetMediaFormat);

    /**
     * Release the renderer and all it resources.
     */
    void release();

    /**
     * Check if renderer has filters
     * @return true if has, false otherwise
     */
    boolean hasFilters();
}
