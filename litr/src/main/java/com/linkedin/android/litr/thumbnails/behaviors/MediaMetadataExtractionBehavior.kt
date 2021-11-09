package com.linkedin.android.litr.thumbnails.behaviors

import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import com.linkedin.android.litr.thumbnails.ExtractionMode
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters

class MediaMetadataExtractionBehavior(private val retriever: MediaMetadataRetriever, private val mode: ExtractionMode) : ExtractionBehavior {

    override fun extract(params: ThumbnailExtractParameters, listener: ExtractBehaviorFrameListener): Boolean {
        var completed = false
        when (mode) {
            ExtractionMode.SyncFrameOnly -> {
                completed = retrieveThumbnails(params, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, listener)
            }
            ExtractionMode.ExactFrameOnly -> {
                completed = retrieveThumbnails(params, MediaMetadataRetriever.OPTION_CLOSEST, listener)
            }
            ExtractionMode.TwoPass -> {
                completed = retrieveThumbnails(params, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, listener)
                if (completed) {
                    completed = retrieveThumbnails(params, MediaMetadataRetriever.OPTION_CLOSEST, listener)
                }
            }
        }
        return completed
    }

    override fun init(params: ThumbnailExtractParameters) {
        params.renderer.init(params.destSize.x, params.destSize.y)
    }

    override fun release() {
        retriever.release()
    }

    private fun retrieveThumbnails(params: ThumbnailExtractParameters, retrieverOptions: Int, listener: ExtractBehaviorFrameListener): Boolean {
        params.timestampsUs.forEachIndexed { index, frameTimeUs ->
            if (Thread.interrupted()) {
                return false
            }

            val scaledBitmap = retriever.getFrameAtTime(frameTimeUs, retrieverOptions)?.let { fullBitmap ->
                ThumbnailUtils.extractThumbnail(fullBitmap, params.destSize.x, params.destSize.y)
            }

            val renderer = params.renderer

            val transformedBitmap = params.timestampsUs.getOrNull(index)?.let { presentationTime ->
                renderer.renderFrame(scaledBitmap, presentationTime)
            } ?: scaledBitmap

            if (transformedBitmap != null) {
                listener.onFrameExtracted(index, transformedBitmap)
            } else {
                listener.onFrameFailed(index)
            }
        }
        return true
    }
}