/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import com.linkedin.android.litr.transcoder.TrackTranscoder
import com.linkedin.android.litr.utils.MediaFormatUtils
import java.nio.ByteBuffer

/**
 * Implementation of [MediaSource] which emulates single video track media with empty frames.
 * @param trackFormat an instance of [MediaFormat] which describes video track.
 * Must contain [MediaFormat.KEY_DURATION], [MediaFormat.KEY_FRAME_RATE] defined.
 * If target video is rotated, must also contain [MediaFormat.KEY_ROTATION] defined, otherwise 0 value will be used.
 */
class MockVideoMediaSource(
    private val trackFormat: MediaFormat
) : MediaSource {

    private val trackDuration: Long
    private val frameDuration: Long
    private val orientationHint: Int

    private var selectedTrack: Int = TrackTranscoder.NO_SELECTED_TRACK
    private var currentPosition = 0L

    init {
        assert(trackFormat.containsKey(MediaFormat.KEY_DURATION))
        trackDuration = trackFormat.getLong(MediaFormat.KEY_DURATION)

        assert(trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE))
        frameDuration = 1_000_000L / MediaFormatUtils.getFrameRate(trackFormat, -1).toInt()

        val keyRotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) MediaFormat.KEY_ROTATION else "rotation-degrees"
        orientationHint = if (trackFormat.containsKey(keyRotation)) trackFormat.getInteger(keyRotation) else 0
    }

    override fun getOrientationHint(): Int {
        return orientationHint
    }

    override fun getTrackCount(): Int {
        return 1
    }

    override fun getTrackFormat(track: Int): MediaFormat {
        return trackFormat
    }

    override fun selectTrack(track: Int) {
        selectedTrack = track
    }

    override fun seekTo(position: Long, mode: Int) {
        currentPosition = position
    }

    override fun getSampleTrackIndex(): Int {
        return selectedTrack
    }

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        // pretend that we read a single byte of data
        return 1
    }

    override fun getSampleTime(): Long {
        return currentPosition
    }

    override fun getSampleFlags(): Int {
        return if (currentPosition < trackDuration) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM
    }

    override fun advance() {
        currentPosition += frameDuration
    }

    override fun release() {
    }

    override fun getSize(): Long {
        return -1
    }
}