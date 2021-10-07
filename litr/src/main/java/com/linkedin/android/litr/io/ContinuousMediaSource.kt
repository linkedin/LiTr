/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io

import android.media.MediaFormat
import android.os.Build
import com.linkedin.android.litr.transcoder.TrackTranscoder
import java.nio.ByteBuffer

/**
 * Implementation of [MediaSource] which represents a media source that does not end until stopped.
 * @param trackFormat an instance of [MediaFormat] which describes video track format.
 * If target video is rotated, [trackFormat] contain [MediaFormat.KEY_ROTATION] defined, otherwise 0 value will be used.
 */
class ContinuousMediaSource(
    private val trackFormat: MediaFormat
) : MediaSource {

    private val frameDuration: Long
    private val orientationHint: Int

    private var selectedTrack: Int = TrackTranscoder.NO_SELECTED_TRACK

    init {
        assert(trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE))
        frameDuration = 1_000_000L / trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)

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

    override fun seekTo(position: Long, mode: Int) {}

    override fun getSampleTrackIndex(): Int {
        return selectedTrack
    }

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        return 0
    }

    override fun getSampleTime(): Long {
        return 0L
    }

    override fun getSampleFlags(): Int {
        return 0
    }

    override fun advance() {}

    override fun release() {
    }

    override fun getSize(): Long {
        return -1
    }
}