/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap
import android.util.Log
import com.linkedin.android.litr.thumbnails.behaviors.ExtractionBehavior
import com.linkedin.android.litr.thumbnails.behaviors.ExtractBehaviorFrameListener

/**
 * Provides the request lifecycle for extracting video thumbnails. The specifics of extraction work are delegated to [ExtractionBehavior]s.
 */
class ThumbnailExtractJob constructor(
    private val jobId: String,
    private val params: ThumbnailExtractParameters,
    private val listener: ThumbnailExtractListener?
) : Runnable {
    private val behaviorFrameListener = object: ExtractBehaviorFrameListener {
        override fun onFrameExtracted(index: Int, bitmap: Bitmap) {
            listener?.onExtracted(jobId, index, bitmap)
        }

        override fun onFrameFailed(index: Int) {
            listener?.onExtractFrameFailed(jobId, index)
        }
    }

    override fun run() {
        try {
            extract()
        } catch (e: RuntimeException) {
            Log.e(TAG, "ThumbnailExtractJob error", e)
            when (e.cause) {
                is InterruptedException -> {
                    listener?.onCancelled(jobId)
                    release()
                }
                else -> error(e)
            }
        }
    }

    private fun extract() {
        listener?.onStarted(jobId, params.timestampsUs)

        if (params.timestampsUs.isEmpty()) {
            listener?.onCompleted(jobId)
            return
        }

        params.behavior.init(params)

        try {
            val completed = params.behavior.extract(params, behaviorFrameListener)

            if (completed) {
                listener?.onCompleted(jobId)
            } else {
                listener?.onCancelled(jobId)
            }
        } catch (ex: Throwable) {
            error(ex)
        }

        release()
    }

    private fun error(cause: Throwable?) {
        Log.e(TAG, "Error encountered while extracting thumbnails", cause)
        listener?.onError(jobId, cause)
        release()
    }

    private fun release() {
        params.behavior.release()
    }

    companion object {
        private val TAG: String = ThumbnailExtractJob::class.java.simpleName
    }
}
