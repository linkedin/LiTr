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
import com.linkedin.android.litr.render.GlVideoRenderer
import com.linkedin.android.litr.utils.TransformationUtil
import java.util.UUID

private const val TAG = "FreeTransformVideoGlPre"

class FreeTransformVideoGlPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun startVideoOverlayTransformation(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        targetVideoConfiguration: TargetVideoConfiguration,
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
            val mediaTarget = MediaMuxerMediaTarget(
                targetMedia.targetFile.path,
                targetMedia.includedTrackCount,
                targetVideoConfiguration.rotation,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val mediaSource = MediaExtractorMediaSource(context, sourceMedia.uri)

            val trackTransforms = mutableListOf<TrackTransform>()
            for (targetTrack in targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue
                }

                val mediaFormat = createMediaFormat(targetTrack)
                if (mediaFormat != null && targetTrack.format is VideoTrackFormat) {
                    mediaFormat.setInteger(KEY_ROTATION, targetVideoConfiguration.rotation)
                }

                val trackTransformBuilder = TrackTransform.Builder(
                    mediaSource,
                    targetTrack.sourceTrackIndex,
                    mediaTarget
                )
                    .setTargetTrack(trackTransforms.size)
                    .setTargetFormat(mediaFormat)
                    .setEncoder(MediaCodecEncoder())
                    .setDecoder(MediaCodecDecoder())

                if (targetTrack.format is VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    val filters: MutableList<GlFilter?> = ArrayList()
                    if (targetMedia.backgroundImageUri != null) {
                        val backgroundImageFilter = TransformationUtil.createGlFilter(
                            context,
                            targetMedia.backgroundImageUri,
                            PointF(1f, 1f),
                            PointF(0.5f, 0.5f), 0f
                        )
                        filters.add(backgroundImageFilter)
                    }

                    filters.add(
                        DefaultVideoFrameRenderFilter(
                            Transform(PointF(0.25f, 0.25f), PointF(0.65f, 0.55f), 30f)
                        )
                    )

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
