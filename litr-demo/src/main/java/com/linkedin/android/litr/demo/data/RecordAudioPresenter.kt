/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.demo.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.MimeType
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.codec.PassthroughDecoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.filter.video.gl.SolidBackgroundColorFilter
import com.linkedin.android.litr.io.AudioRecordMediaSource
import com.linkedin.android.litr.io.MediaMuxerMediaTarget
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.io.MockVideoMediaSource
import com.linkedin.android.litr.render.GlVideoRenderer
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "RecordAudioPresenter"

class RecordAudioPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    @SuppressLint("MissingPermission")
    fun recordAudio(
        mediaSource: AudioRecordMediaSource,
        targetMedia: TargetMedia,
        transformationState: TransformationState
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Android Marshmallow or newer required")
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
            val mediaTarget: MediaTarget = MediaMuxerMediaTarget(
                targetMedia.targetFile.path,
                2,
                0,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            // Create a single (synthetic) video track to ensure our output is playable by the demo
            // app. We only use 1 second of video (a solid color) so the duration of the video will
            // likely depend on the length of the audio recording.
            val videoTrackFormat = VideoTrackFormat(0, MimeType.VIDEO_AVC)
                .apply {
                    duration = TimeUnit.SECONDS.toMicros(1)
                    frameRate = 30
                    width = 512
                    height = 512
                }

            val videoMediaFormat = createVideoMediaFormat(videoTrackFormat)
            val videoMediaSource = MockVideoMediaSource(videoMediaFormat)
            val filter = SolidBackgroundColorFilter(Color.RED)

            val videoTransformBuilder = TrackTransform.Builder(
                videoMediaSource,
                0,
                mediaTarget
            )
                .setTargetTrack(0)
                .setTargetFormat(videoMediaFormat)
                .setEncoder(MediaCodecEncoder())
                .setDecoder(PassthroughDecoder(1))
                .setRenderer(GlVideoRenderer(listOf<GlFilter>(filter)))

            val audioTrackFormat = AudioTrackFormat(0, MimeType.AUDIO_AAC)
                .apply {
                    samplingRate = 44100
                    channelCount = 1
                    bitrate = 64 * 1024
                }

            val audioTransformBuilder = TrackTransform.Builder(
                mediaSource,
                0,
                mediaTarget
            )
                .setTargetTrack(1)
                .setTargetFormat(createAudioMediaFormat(audioTrackFormat))
                .setEncoder(MediaCodecEncoder())
                .setDecoder(MediaCodecDecoder())

            val trackTransforms = mutableListOf<TrackTransform>()
            trackTransforms.add(videoTransformBuilder.build())
            trackTransforms.add(audioTransformBuilder.build())

            mediaSource.startRecording()

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

    fun stopRecording(mediaSource: AudioRecordMediaSource) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Android Marshmallow or newer required")
        }
        mediaSource.stopRecording()
    }
}
