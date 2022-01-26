/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract.behaviors

import android.graphics.Bitmap
import com.linkedin.android.litr.ExperimentalFrameExtractorApi
import com.linkedin.android.litr.frameextract.FrameExtractParameters

/**
 * An interface used by [FrameExtractBehavior] to notify the job of the status of individual frame extraction.
 */
interface FrameExtractBehaviorFrameListener {
    fun onFrameExtracted(bitmap: Bitmap)
    fun onFrameFailed()
}

/**
 * Provides a way to customize frame extraction behavior.
 *
 * All methods are guaranteed to be called on the same thread, but the thread will not be the main thread.
 */
@ExperimentalFrameExtractorApi
interface FrameExtractBehavior {
    /**
     * Perform frame extraction work here, and notify [listener] for each frame extracted. This method is called only once.
     *
     * For long-running operations, [Thread.isInterrupted] should be checked periodically. If interrupted, implementation should return false from this method.
     *
     * @return Return true if extraction is completed/scheduled, false if it was canceled.
     */
    fun extract(params: FrameExtractParameters, listener: FrameExtractBehaviorFrameListener): Boolean

    /**
     * Called when this behavior should clean up any associated resources.
     */
    fun release()
}
