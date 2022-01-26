/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract

import android.graphics.Bitmap
import com.linkedin.android.litr.ExperimentalFrameExtractorApi

/**
 * Listener for frame extraction events.
 */
@ExperimentalFrameExtractorApi
interface FrameExtractListener {
    /**
     * Occurs when the specified job has started.
     */
    fun onStarted(id: String, timestampUs: Long) {}

    /**
     * Occurs when a frame is extracted. This method is not guaranteed to be called if there is an error.
     */
    fun onExtracted(id: String, timestampUs: Long, bitmap: Bitmap) {}

    /**
     * Extraction was aborted at some point.
     */
    fun onCancelled(id: String, timestampUs: Long) {}

    /**
     * An error occurred during extraction.
     */
    fun onError(id: String, timestampUs: Long, cause: Throwable?) {}
}
