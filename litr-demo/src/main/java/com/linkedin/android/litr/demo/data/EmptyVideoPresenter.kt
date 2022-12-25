/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data

import android.content.Context
import android.graphics.PointF
import android.media.MediaMuxer
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.codec.PassthroughDecoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MockVideoMediaSource
import com.linkedin.android.litr.render.GlVideoRenderer
import com.linkedin.android.litr.utils.TransformationUtil
import java.util.UUID

private const val TAG = "EmptyVideoPresenter"

class EmptyVideoPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun createEmptyVideo(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        transformationState: TransformationState
    ) {
        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete()
        }

        transformationState.requestId = UUID.randomUUID().toString()
        val transformationListener = MediaTransformationListener(
            context,
            transformationState.requestId,
            transformationState,
            targetMedia
        )

        try {
            var videoRotation = 0
            for (trackFormat in sourceMedia.tracks) {
                if (trackFormat.mimeType.startsWith("video")) {
                    videoRotation = (trackFormat as VideoTrackFormat).rotation
                    break
                }
            }

            val mediaTarget = MediaMuxerMediaTarget(
                targetMedia.targetFile.path,
                targetMedia.includedTrackCount,
                videoRotation,
                if (hasVp8OrVp9TrackFormat(sourceMedia.tracks)) MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM else MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            require(sourceMedia.tracks.isNotEmpty()) { "No source tracks!" }

            val trackTransforms = mutableListOf<TrackTransform>()
            val sourceTrackFormat = sourceMedia.tracks[0]

            val mediaSource = MockVideoMediaSource(createVideoMediaFormat((sourceTrackFormat as VideoTrackFormat)))

            for (targetTrack in targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue
                }

                val mediaFormat = createMediaFormat(targetTrack)

                val trackTransformBuilder = TrackTransform.Builder(
                    mediaSource,
                    targetTrack.sourceTrackIndex,
                    mediaTarget
                )
                    .setTargetTrack(trackTransforms.size)
                    .setTargetFormat(mediaFormat)
                    .setEncoder(MediaCodecEncoder())
                    .setDecoder(PassthroughDecoder(1))

                if (targetTrack.format is VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    val filters = mutableListOf<GlFilter>()
                    if (targetMedia.backgroundImageUri != null) {
                        TransformationUtil.createGlFilter(
                            context,
                            targetMedia.backgroundImageUri,
                            PointF(1f, 1f),
                            PointF(0.5f, 0.5f),
                            0f
                        )?.let {
                            filters.add(it)
                        }
                    }
                    if ((targetTrack as TargetVideoTrack).overlay != null) {
                        createGlFilters(
                            sourceMedia,
                            targetTrack, 0.3f, PointF(0.25f, 0.25f), 30f
                        )?.let {
                            filters.addAll(it)
                        }
                    }
                    trackTransformBuilder.setRenderer(GlVideoRenderer(filters))
                }
                trackTransforms.add(trackTransformBuilder.build())
            }

            mediaTransformer.transform(
                transformationState.requestId,
                trackTransforms,
                transformationListener,
                MediaTransformer.GRANULARITY_DEFAULT
            )
        } catch (ex: MediaTransformationException) {
            Log.e(TAG, "Exception when trying to perform track operation", ex)
        }
    }
}
