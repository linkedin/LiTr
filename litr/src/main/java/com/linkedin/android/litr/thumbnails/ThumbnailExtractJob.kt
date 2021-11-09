package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap
import android.util.Log

/**
 * Provides the request lifecycle for extracting video thumbnails. The specifics of extraction work are delegated to [ExtractionBehavior]s.
 */
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

        try {
            val completed = params.behavior.extract(params) { index, bitmap ->
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

        release()
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
