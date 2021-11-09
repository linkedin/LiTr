package com.linkedin.android.litr.thumbnails.behaviors

import android.graphics.Bitmap
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters

/**
 * An interface used by [ExtractionBehavior] to notify the job of the status of individual frame extraction.
 */
interface ExtractBehaviorFrameListener {
    fun onFrameExtracted(index: Int, bitmap: Bitmap)
    fun onFrameFailed(index: Int)
}

/**
 * Provides a way to customize thumbnail extraction behavior.
 *
 * All methods are guaranteed to be called on the same thread, but the thread will not be the main thread.
 */
interface ExtractionBehavior {
    /**
     * Called before extraction begins, perform initialization here as needed.
     */
    fun init(params: ThumbnailExtractParameters)

    /**
     * Perform frame extraction work here, and notify [listener] for each frame extracted. This method is called only once.
     *
     * For long-running operations, [Thread.isInterrupted] should be checked periodically. If interrupted, implementation should return false from this method.
     *
     * @return Return true if extraction is completed, false if it was canceled.
     */
    fun extract(params: ThumbnailExtractParameters, listener: ExtractBehaviorFrameListener): Boolean

    /**
     * Called when this behavior should clean up any associated resources.
     */
    fun release()
}