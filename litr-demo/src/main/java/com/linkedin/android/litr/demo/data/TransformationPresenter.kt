/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data

import android.content.Context
import com.linkedin.android.litr.MediaTransformer
import android.content.Intent
import android.graphics.PointF
import com.linkedin.android.litr.filter.GlFilter
import android.graphics.BitmapFactory
import com.linkedin.android.litr.utils.TransformationUtil
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import com.linkedin.android.litr.MimeType

val KEY_ROTATION =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) MediaFormat.KEY_ROTATION else "rotation-degrees"

open class TransformationPresenter(
    private val context: Context,
    private val mediaTransformer: MediaTransformer
) {

    fun cancelTransformation(requestId: String) {
        mediaTransformer.cancel(requestId)
    }

    fun play(contentUri: Uri?) {
        if (contentUri != null) {
            val playIntent = Intent(Intent.ACTION_VIEW)
                .apply {
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(playIntent)
        }
    }

    protected fun createGlFilters(
        sourceMedia: SourceMedia,
        targetTrack: TargetVideoTrack?,
        overlayWidth: Float,
        position: PointF,
        rotation: Float
    ): List<GlFilter>? {
        return targetTrack?.overlay?.let { _ ->
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(targetTrack.overlay))?.let { bitmap ->
                val sourceVideoTrackFormat = sourceMedia.tracks[targetTrack.sourceTrackIndex] as VideoTrackFormat
                val overlayHeight =
                    if (sourceVideoTrackFormat.rotation == 90 || sourceVideoTrackFormat.rotation == 270) {
                        val overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.height
                        val overlayHeightPixels =
                            overlayWidthPixels * bitmap.height / bitmap.width
                        overlayHeightPixels / sourceVideoTrackFormat.width
                    } else {
                        val overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.width
                        val overlayHeightPixels =
                            overlayWidthPixels * bitmap.height / bitmap.width
                        overlayHeightPixels / sourceVideoTrackFormat.height
                    }

                TransformationUtil.createGlFilter(
                    context,
                    targetTrack.overlay,
                    PointF(overlayWidth, overlayHeight),
                    position,
                    rotation
                )?.let {
                    listOf(it)
                }
            }
        }
    }

    protected fun createMediaFormat(targetTrack: TargetTrack?): MediaFormat? {
        return targetTrack?.format?.let { format ->
            if (format.mimeType.startsWith("video")) {
                createVideoMediaFormat(targetTrack.format as VideoTrackFormat)
            } else if (format.mimeType.startsWith("audio")) {
                createAudioMediaFormat(targetTrack.format as AudioTrackFormat)
            } else {
                null
            }
        }
    }

    protected fun createVideoMediaFormat(trackFormat: VideoTrackFormat): MediaFormat {
        return MediaFormat.createVideoFormat(trackFormat.mimeType, trackFormat.width, trackFormat.height)
            .apply {
                setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, trackFormat.keyFrameInterval)
                setInteger(MediaFormat.KEY_FRAME_RATE, trackFormat.frameRate)
                setLong(MediaFormat.KEY_DURATION, trackFormat.duration)
                setInteger(KEY_ROTATION, trackFormat.rotation)
            }
    }

    protected fun createAudioMediaFormat(trackFormat: AudioTrackFormat): MediaFormat {
        return MediaFormat.createAudioFormat(trackFormat.mimeType, trackFormat.samplingRate, trackFormat.channelCount)
            .apply {
                setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate)
                setLong(MediaFormat.KEY_DURATION, trackFormat.duration)
            }
    }

    protected fun hasVp8OrVp9Track(targetTracks: List<TargetTrack>): Boolean {
        return targetTracks.any {
            it.shouldInclude &&
                (it.format.mimeType == MimeType.VIDEO_VP8 || it.format.mimeType == MimeType.VIDEO_VP9)
        }
    }

    protected fun hasVp8OrVp9TrackFormat(trackFormats: List<MediaTrackFormat>): Boolean {
        return trackFormats.any {
            it.mimeType == MimeType.VIDEO_VP8 || it.mimeType == MimeType.VIDEO_VP9
        }
    }
}
