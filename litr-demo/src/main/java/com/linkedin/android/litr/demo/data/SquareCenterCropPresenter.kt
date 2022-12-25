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
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.filter.Transform
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaSource
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.render.GlVideoRenderer
import java.util.UUID

private const val TAG = "SquareCenterCropPresent"

class SquareCenterCropPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun squareCenterCrop(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        transformationState: TransformationState
    ) {
        if (targetMedia.includedTrackCount < 1) {
            return
        }

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

            val mediaTarget: MediaTarget = MediaMuxerMediaTarget(
                targetMedia.targetFile.path,
                targetMedia.includedTrackCount,
                videoRotation,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            val mediaSource: MediaSource = MediaExtractorMediaSource(context, sourceMedia.uri)

            val trackTransforms = mutableListOf<TrackTransform>()
            for (targetTrack in targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue
                }

                val trackTransformBuilder = TrackTransform.Builder(
                    mediaSource,
                    targetTrack.sourceTrackIndex,
                    mediaTarget
                )
                    .setTargetTrack(trackTransforms.size)
                    .setEncoder(MediaCodecEncoder())
                    .setDecoder(MediaCodecDecoder())

                if (targetTrack.format is VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    val filters = mutableListOf<GlFilter>()

                    val width = (targetTrack.format as VideoTrackFormat).width
                    val height = (targetTrack.format as VideoTrackFormat).height
                    val transform = if (videoRotation == 0 || videoRotation == 180) {
                        // landscape
                        Transform(PointF(width.toFloat() / height, 1.0f), PointF(0.5f, 0.5f), 0f)
                    } else {
                        // portrait
                        Transform(PointF(1.0f, width.toFloat() / height), PointF(0.5f, 0.5f), 0f)
                    }

                    filters.add(DefaultVideoFrameRenderFilter(transform))
                    trackTransformBuilder.setRenderer(GlVideoRenderer(filters))

                    // hack to make video square, should be done more elegantly in prod code
                    (targetTrack.format as VideoTrackFormat).width = TargetMedia.DEFAULT_VIDEO_HEIGHT
                }

                trackTransformBuilder.setTargetFormat(createMediaFormat(targetTrack))
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
