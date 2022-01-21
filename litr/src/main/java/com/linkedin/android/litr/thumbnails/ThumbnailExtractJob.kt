/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.util.Log
import com.linkedin.android.litr.thumbnails.behaviors.ExtractionBehavior
import com.linkedin.android.litr.thumbnails.behaviors.ExtractBehaviorFrameListener

/**
 * Provides the request lifecycle for extracting video thumbnails. The specifics of extraction work are delegated to [ExtractionBehavior]s.
 */
open class ThumbnailExtractJob constructor(
    private val jobId: String,
    private val params: ThumbnailExtractParameters,
    private val behavior: ExtractionBehavior,
    private val listener: ThumbnailExtractListener?
) : Runnable {
    private val behaviorFrameListener = object: ExtractBehaviorFrameListener {
        override fun onFrameExtracted(bitmap: Bitmap) {
            val renderedBitmap = renderExtractedFrame(bitmap)

            if (renderedBitmap != null) {
                listener?.onExtracted(jobId, params.timestampUs, renderedBitmap)
            } else {
                listener?.onError(jobId, params.timestampUs, null)
            }
        }

        override fun onFrameFailed() {
            listener?.onError(jobId, params.timestampUs, null)
        }
    }

    override fun run() {
        try {
            extract()
        } catch (e: RuntimeException) {
            Log.e(TAG, "ThumbnailExtractJob error", e)
            when (e.cause) {
                is InterruptedException -> {
                    listener?.onCancelled(jobId, params.timestampUs)
                }
                else -> error(e)
            }
        }
    }

    private fun renderExtractedFrame(bitmap: Bitmap): Bitmap? {
        if (Thread.interrupted()) {
            listener?.onCancelled(jobId, params.timestampUs)
            return null
        }

        val resizedBitmap = if (params.destSize != null) {
            ThumbnailUtils.extractThumbnail(bitmap, params.destSize.x, params.destSize.y)
        } else {
            bitmap
        }

        if (Thread.interrupted()) {
            listener?.onCancelled(jobId, params.timestampUs)
            return null
        }

        val renderer = params.renderer
        return renderer.renderFrame(resizedBitmap, params.timestampUs * 1000L)
    }

    private fun extract() {
        listener?.onStarted(jobId, params.timestampUs)

        if (params.timestampUs < 0) {
            listener?.onCancelled(jobId, params.timestampUs)
            return
        }

        try {
            if (Thread.interrupted()) {
                listener?.onCancelled(jobId, params.timestampUs)
                return
            }

            val completed = behavior.extract(params, behaviorFrameListener)

            if (!completed) {
                listener?.onCancelled(jobId, params.timestampUs)
            }
        } catch (ex: Throwable) {
            error(ex)
        }
    }

    private fun error(cause: Throwable?) {
        Log.e(TAG, "Error encountered while extracting thumbnails", cause)
        listener?.onError(jobId, params.timestampUs, cause)
    }

    companion object {
        private val TAG: String = ThumbnailExtractJob::class.java.simpleName
    }
}
