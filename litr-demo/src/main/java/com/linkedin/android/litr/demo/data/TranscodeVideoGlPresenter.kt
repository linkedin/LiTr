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
import android.net.Uri
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.filter.audio.VolumeFilter
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaRange
import com.linkedin.android.litr.io.MediaSource
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.muxers.NativeMediaMuxerMediaTarget
import com.linkedin.android.litr.render.AudioRenderer
import com.linkedin.android.litr.render.GlVideoRenderer
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "TranscodeVideoGlPresent"

class TranscodeVideoGlPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun startTransformation(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        trimConfig: TrimConfig,
        audioVolumeConfig: AudioVolumeConfig,
        transformationState: TransformationState,
        enableNativeMuxer: Boolean
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

            val mediaTarget = buildMediaTarget(
                    context,
                    targetMedia,
                    videoRotation,
                    enableNativeMuxer
            )

            val trackTransforms: MutableList<TrackTransform> = ArrayList(targetMedia.tracks.size)

            val mediaRange = if (trimConfig.enabled) MediaRange(
                TimeUnit.MILLISECONDS.toMicros((trimConfig.range[0] * 1000).toLong()),
                TimeUnit.MILLISECONDS.toMicros((trimConfig.range[1] * 1000).toLong())
            ) else MediaRange(0, Long.MAX_VALUE)

            val mediaSource: MediaSource = MediaExtractorMediaSource(context, sourceMedia.uri, mediaRange)

            for (targetTrack in targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue
                }
                val encoder = MediaCodecEncoder()

                val trackTransformBuilder = TrackTransform.Builder(
                    mediaSource,
                    targetTrack.sourceTrackIndex,
                    mediaTarget
                )
                    .setTargetTrack(trackTransforms.size)
                    .setTargetFormat(if (targetTrack.shouldTranscode) createMediaFormat(targetTrack) else null)
                    .setEncoder(encoder)
                    .setDecoder(MediaCodecDecoder())

                if (targetTrack.format is VideoTrackFormat) {
                    trackTransformBuilder.setRenderer(
                        GlVideoRenderer(
                            createGlFilters(
                                sourceMedia,
                                targetTrack as TargetVideoTrack,
                                0.56f,
                                PointF(0.6f, 0.4f), 30f
                            )
                        )
                    )
                } else if (targetTrack.format is AudioTrackFormat && audioVolumeConfig.enabled) {
                    trackTransformBuilder.setRenderer(
                        AudioRenderer(
                            encoder,
                            mutableListOf(VolumeFilter(audioVolumeConfig.value.toDouble()))
                        )
                    )
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

    private fun buildMediaTarget(
            context: Context,
            targetMedia: TargetMedia,
            videoRotation: Int,
            enableNativeMuxer: Boolean
    ): MediaTarget {
        val outputFormat = if (hasVp8OrVp9Track(targetMedia.tracks))
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        else
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4

        return if (enableNativeMuxer) {
            NativeMediaMuxerMediaTarget(
                    targetMedia.targetFile.absolutePath,
                    targetMedia.includedTrackCount,
                    videoRotation,
                    outputFormat
            )
        } else {
            MediaMuxerMediaTarget(
                    context,
                    Uri.fromFile(targetMedia.targetFile),
                    targetMedia.includedTrackCount,
                    videoRotation,
                    outputFormat
            )
        }
    }
}
