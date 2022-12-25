/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data

import android.content.Context
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.MimeType
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaRange
import com.linkedin.android.litr.render.GlVideoRenderer
import java.util.UUID

private const val TAG = "MuxVideoAndAudioPresent"

class MuxVideoAndAudioPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun muxVideoAndAudio(
        sourceVideo: SourceMedia,
        sourceAudio: SourceMedia,
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
            for (trackFormat in sourceVideo.tracks) {
                if (trackFormat.mimeType.startsWith("video")) {
                    videoRotation = (trackFormat as VideoTrackFormat).rotation
                    break
                }
            }

            val mediaTarget = MediaMuxerMediaTarget(
                targetMedia.targetFile.path,
                2,
                videoRotation,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            var audioTrimRange = MediaRange(0, Long.MAX_VALUE)

            val trackTransforms = mutableListOf<TrackTransform>()

            for (trackFormat in sourceVideo.tracks) {
                if (trackFormat is VideoTrackFormat) {
                    val trackTransformBuilder = TrackTransform.Builder(
                        MediaExtractorMediaSource(context, sourceVideo.uri),
                        trackFormat.index,
                        mediaTarget
                    )
                        .setTargetTrack(trackTransforms.size)
                        .setTargetFormat(null)
                        .setEncoder(MediaCodecEncoder())
                        .setDecoder(MediaCodecDecoder())
                        .setRenderer(GlVideoRenderer(null))
                    // trim audio track to video track's duration
                    audioTrimRange = MediaRange(0, trackFormat.duration)
                    trackTransforms.add(trackTransformBuilder.build())
                    break
                }
            }

            for (trackFormat in sourceAudio.tracks) {
                if (trackFormat is AudioTrackFormat) {
                    // make sure audio is in AVC compatible encoding
                    val targetAudioFormat = createAudioMediaFormat(trackFormat)
                    targetAudioFormat.setString(MediaFormat.KEY_MIME, MimeType.AUDIO_AAC)

                    val trackTransformBuilder = TrackTransform.Builder(
                        MediaExtractorMediaSource(context, sourceAudio.uri, audioTrimRange),
                        trackFormat.index,
                        mediaTarget
                    )
                        .setTargetTrack(trackTransforms.size)
                        .setTargetFormat(targetAudioFormat)
                        .setEncoder(MediaCodecEncoder())
                        .setDecoder(MediaCodecDecoder())
                    trackTransforms.add(trackTransformBuilder.build())
                    break
                }
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
