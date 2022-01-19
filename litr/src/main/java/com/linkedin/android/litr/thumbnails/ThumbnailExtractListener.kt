/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap

/**
 * Listener for thumbnail extraction events.
 */
interface ThumbnailExtractListener {
    /**
     * Occurs when extraction started, and no frames were processed yet.
     */
    fun onStarted(id: String, timestampsUs: List<Long>)

    /**
     * Occurs when a frame is extracted. May be called multiple times for the same frame (for example, when [ExtractionMode.TwoPass] is used).
     * This method is NOT GUARANTEED TO BE CALLED for every frame, or for any frames.
     */
    fun onExtracted(id: String, index: Int, bitmap: Bitmap)

    /**
     * Occurs when a frame was requested, but could not be extracted. Consumer may want to display an error over the specified frame, or ignore it.
     */
    fun onExtractFrameFailed(id: String, index: Int)

    /**
     * Occurs when extraction completed and terminated normally.
     */
    fun onCompleted(id: String)

    /**
     * Extraction was aborted at some point.
     */
    fun onCancelled(id: String)

    /**
     * An error occurred during extraction.
     */
    fun onError(id: String, cause: Throwable?)
}
