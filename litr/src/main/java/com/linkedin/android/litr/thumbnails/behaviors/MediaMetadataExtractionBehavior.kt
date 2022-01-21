/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails.behaviors

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import com.linkedin.android.litr.ExperimentalThumbnailsApi
import com.linkedin.android.litr.thumbnails.ExtractionMode
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters

@ExperimentalThumbnailsApi
class MediaMetadataExtractionBehavior(private val context: Context) : ExtractionBehavior {
    private var retrieverToMediaUri: RetrieverToMediaUri? = null

    @Synchronized
    private fun setupRetriever(mediaUri: Uri): MediaMetadataRetriever {
        val currentRetrieverToMediaUri = retrieverToMediaUri

        return if (currentRetrieverToMediaUri == null || currentRetrieverToMediaUri.mediaUri != mediaUri) {
            currentRetrieverToMediaUri?.retriever?.release()

            val newRetriever = MediaMetadataRetriever().apply {
                setDataSource(context, mediaUri)
            }

            retrieverToMediaUri = RetrieverToMediaUri(newRetriever, mediaUri)

            newRetriever
        } else {
            currentRetrieverToMediaUri.retriever
        }
    }

    override fun extract(params: ThumbnailExtractParameters, listener: ExtractBehaviorFrameListener): Boolean {

        val completed = when (params.mode) {
            ExtractionMode.Fast -> {
                retrieveThumbnail(params, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, listener)
            }
            ExtractionMode.Exact -> {
                retrieveThumbnail(params, MediaMetadataRetriever.OPTION_CLOSEST, listener)
            }
        }
        return completed
    }

    override fun release() {
        retrieverToMediaUri?.retriever?.release()
    }

    private fun retrieveThumbnail(params: ThumbnailExtractParameters, retrieverOptions: Int, listener: ExtractBehaviorFrameListener): Boolean {
        val retriever = setupRetriever(params.mediaUri)
        val extractedBitmap = retriever.getFrameAtTime(params.timestampUs, retrieverOptions)

        if (extractedBitmap != null) {
            listener.onFrameExtracted(extractedBitmap)
        } else {
            listener.onFrameFailed()
        }
        return true
    }

    data class RetrieverToMediaUri(val retriever: MediaMetadataRetriever, val mediaUri: Uri)
}
