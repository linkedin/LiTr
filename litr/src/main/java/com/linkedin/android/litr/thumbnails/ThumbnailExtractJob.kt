package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap
import android.util.Log


class ThumbnailExtractJob constructor(
    private val jobId: String,
    private val params: ThumbnailExtractParameters,
    private val listener: ThumbnailExtractListener?
) : Runnable {
    private var isRendererInitialized = false

    override fun run() {
        try {
            extract()
        } catch (e: RuntimeException) {
            Log.e(TAG, "ThumbnailExtractJob error", e)
            when (e.cause) {
                is InterruptedException -> stop()
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

        var completed = false

        try {
            completed = params.behavior.extract(params) { index, bitmap ->
                handleExtractedFrame(index, bitmap)
            }

            if (completed) {
                listener?.onCompleted(jobId)
            } else {
                listener?.onCancelled(jobId)
            }
        } catch (ex: Throwable) {
            error(ex)
        }

        release(completed)
    }

    private fun handleExtractedFrame(index: Int, bitmap: Bitmap) {

        val renderer = params.renderer
        if (!isRendererInitialized) {
            isRendererInitialized = true
            renderer.init(bitmap.width, bitmap.height)
        }

        val transformedBitmap = params.timestampsUs.getOrNull(index)?.let { presentationTime ->
            renderer.renderFrame(bitmap, presentationTime)
        } ?: bitmap

        listener?.onExtracted(jobId, index, transformedBitmap)
    }

    private fun stop() {
        release(true)
    }

    private fun error(cause: Throwable?) {
        Log.e(TAG, "Error encountered while extracting thumbnails", cause)
        release(false)
        listener?.onError(jobId, cause)
    }

    private fun release(success: Boolean) {
        params.behavior.release()
        if (success) {
            listener?.onCompleted(jobId)
        }
    }

    companion object {
        private val TAG: String = ThumbnailExtractJob::class.java.simpleName
    }
}
