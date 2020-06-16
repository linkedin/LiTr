/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.video.gl;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;

/**
 * Interface that provides animation frames to be overlaid onto video frames.
 */
public interface AnimationFrameProvider {

    /**
     * Get total number of frames in animation
     * @return total frame count
     */
    int getFrameCount();

    /**
     * Get next frame content
     * @return frame bitmap, null if not available
     */
    @Nullable
    Bitmap getNextFrame();

    /**
     * Get next frame delay, AKA duration frame is visible before replaced by next one
     * @return frame delay in nanoseconds
     */
    long getNextFrameDurationNs();

    /**
     * Advance animation to next frame
     */
    void advance();
}
