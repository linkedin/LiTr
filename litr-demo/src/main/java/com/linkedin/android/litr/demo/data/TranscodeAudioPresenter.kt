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
import com.linkedin.android.litr.codec.PassthroughBufferEncoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.io.MediaExtractorMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaRange
import com.linkedin.android.litr.io.WavMediaTarget
import com.linkedin.android.litr.render.AudioRenderer
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "TranscodeAudioPresenter"

class TranscodeAudioPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    fun transcodeAudio(
        sourceMedia: SourceMedia,
        targetMedia: TargetMedia,
        trimConfig: TrimConfig,
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

        val mediaRange = if (trimConfig.enabled) MediaRange(
            TimeUnit.MILLISECONDS.toMicros((trimConfig.range[0] * 1000).toLong()),
            TimeUnit.MILLISECONDS.toMicros((trimConfig.range[1] * 1000).toLong())
        ) else MediaRange(0, Long.MAX_VALUE)

        try {
            val targetMimeType = if (targetMedia.writeToWav) MimeType.AUDIO_RAW else MimeType.AUDIO_AAC
            val mediaTarget =
                if (targetMedia.writeToWav) WavMediaTarget(targetMedia.targetFile.path) else MediaMuxerMediaTarget(
                    targetMedia.targetFile.path,
                    1,
                    0,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )

            val mediaSource = MediaExtractorMediaSource(context, sourceMedia.uri, mediaRange)

            val trackTransforms = mutableListOf<TrackTransform>()
            for (targetTrack in targetMedia.tracks) {
                if (targetTrack.format is AudioTrackFormat) {
                    val trackFormat = targetTrack.format as AudioTrackFormat
                    val mediaFormat = MediaFormat.createAudioFormat(
                        targetMimeType,
                        trackFormat.samplingRate,
                        trackFormat.channelCount
                    ).apply {
                        setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate)
                        setLong(MediaFormat.KEY_DURATION, trackFormat.duration)
                    }

                    val encoder =
                        if (targetMedia.writeToWav) PassthroughBufferEncoder(8192) else MediaCodecEncoder()

                    val trackTransform = TrackTransform.Builder(
                        mediaSource,
                        targetTrack.sourceTrackIndex,
                        mediaTarget
                    )
                        .setTargetTrack(0)
                        .setDecoder(MediaCodecDecoder())
                        .setEncoder(encoder)
                        .setRenderer(AudioRenderer(encoder))
                        .setTargetFormat(mediaFormat)
                        .build()
                    trackTransforms.add(trackTransform)
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
            Log.e(TAG, "Exception when trying to transcode audio", ex)
        }
    }
}
