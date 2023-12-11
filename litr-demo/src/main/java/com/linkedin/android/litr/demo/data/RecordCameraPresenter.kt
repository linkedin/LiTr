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
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import com.linkedin.android.litr.MediaTransformer
import com.linkedin.android.litr.MimeType
import com.linkedin.android.litr.TrackTransform
import com.linkedin.android.litr.codec.MediaCodecDecoder
import com.linkedin.android.litr.codec.MediaCodecEncoder
import com.linkedin.android.litr.exception.MediaTransformationException
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.io.*
// import com.linkedin.android.litr.muxers.NativeMediaMuxerMediaTarget
import com.linkedin.android.litr.render.GlVideoRenderer
import java.util.UUID

private const val TAG = "RecordCameraPresenter"

class RecordCameraPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) : TransformationPresenter(context, mediaTransformer) {

    @SuppressLint("MissingPermission")
    fun recordCamera(
            audioMediaSource: AudioRecordMediaSource,
            videoMediaSource: Camera2MediaSource,
            targetMedia: TargetMedia,
            transformationState: TransformationState,
            enableNativeMuxer: Boolean
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
            val mediaTarget = buildMediaTarget(targetMedia, enableNativeMuxer)

            val videoTrackFormat = VideoTrackFormat(0, MimeType.VIDEO_AVC)
                .apply {
                    width = videoMediaSource.width
                    height = videoMediaSource.height
                    frameRate = videoMediaSource.frameRate
                    bitrate = videoMediaSource.bitrate
                    keyFrameInterval = videoMediaSource.keyFrameInterval
                    rotation = videoMediaSource.orientation
                }

            val videoMediaFormat = createVideoMediaFormat(videoTrackFormat)

            val videoTransformBuilder = TrackTransform.Builder(
                videoMediaSource,
                0,
                mediaTarget
            )
                .setTargetTrack(0)
                .setTargetFormat(videoMediaFormat)
                .setEncoder(MediaCodecEncoder())
                .setDecoder(videoMediaSource)
                .setRenderer(GlVideoRenderer(listOf<GlFilter>()))

            val audioTrackFormat = AudioTrackFormat(0, MimeType.AUDIO_AAC)
                .apply {
                    samplingRate = 44100
                    channelCount = 1
                    bitrate = 64 * 1024
                }

            val audioTransformBuilder = TrackTransform.Builder(
                audioMediaSource,
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

            audioMediaSource.startRecording()
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

    fun stopRecording(
            audioMediaSource: AudioRecordMediaSource,
            videoMediaSource: Camera2MediaSource
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Android Marshmallow or newer required")
        }
        audioMediaSource.stopRecording()
        videoMediaSource.stopRecording()
    }

    private fun buildMediaTarget(
        targetMedia: TargetMedia,
        enableNativeMuxer: Boolean
    ): MediaTarget {
        return MediaMuxerMediaTarget(
            targetMedia.targetFile.path,
            2,
            0,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
        // remove above code and uncomment if need to experiment with ffmpeg muxer
//        return if (enableNativeMuxer) {
//            NativeMediaMuxerMediaTarget(
//                targetMedia.targetFile.path,
//                2,
//                0,
//                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
//            )
//        } else {
//            MediaMuxerMediaTarget(
//                targetMedia.targetFile.path,
//                2,
//                0,
//                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
//            )
//        }
    }
}
