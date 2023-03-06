/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.muxers

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.linkedin.android.litr.exception.MediaTargetException
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.io.MediaTargetSample
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "NativeMediaTarget"

/**
 * An implementation of MediaTarget, which wraps FFMpeg's libavformat.
 *
 * Before writing any media samples to MediaMuxer, all tracks must be added and MediaMuxer must be started. Some track
 * transcoders may start writing their output before other track transcoders added their track. This class queues writing
 * media samples until all tracks are created, allowing track transcoders to work independently.
 */
class NativeMediaMuxerMediaTarget(
        private val outputFilePath: String,
        private val trackCount: Int,
        private val orientationHint: Int,
        outputFormat: String
): MediaTarget {

    constructor(outputFilePath: String, trackCount: Int, orientationHint: Int, outputFormat: Int) :
            this(
                    outputFilePath,
                    trackCount,
                    orientationHint,
                    NativeOutputFormats.fromOutputFormat(outputFormat)
            )

    private val queue: LinkedList<MediaTargetSample> = LinkedList()
    private var isStarted: Boolean = false
    private val mediaMuxer : NativeMediaMuxer

    private var numberOfTracksToAdd = 0
    private val mediaFormatsToAdd = arrayOfNulls<MediaFormat>(trackCount)

    init {
        try {
            mediaMuxer = NativeMediaMuxer(outputFilePath, outputFormat)
        } catch (ex: IllegalStateException) {
            throw MediaTargetException(
                    MediaTargetException.Error.INVALID_PARAMS,
                    outputFilePath,
                    outputFormat,
                    ex);
        }
    }

    /**
     * Adds a muxer option. This method *must* be called before the muxer has been started.
     */
    fun addOption(key: String, value: String) = mediaMuxer.addOption(key, value)

    override fun addTrack(mediaFormat: MediaFormat, targetTrack: Int): Int {
        mediaFormatsToAdd[targetTrack] = mediaFormat
        numberOfTracksToAdd++

        if (numberOfTracksToAdd == trackCount) {
            Log.d(TAG, "All tracks added, starting MediaMuxer, writing out ${queue.size} queued samples")

            mediaFormatsToAdd.filterNotNull().forEach {
                mediaMuxer.addTrack(it)
            }

            mediaMuxer.start()
            isStarted = true

            // Write out any queued samples.
            while (queue.isNotEmpty()) {
                val sample = queue.removeFirst()
                mediaMuxer.writeSampleData(sample.targetTrack, sample.buffer, sample.info)
            }
        }

        return targetTrack
    }

    override fun writeSampleData(targetTrack: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        // We always create a sample before we write it, to ensure that the ByteBuffer is in a
        // suitable format to be passed to the native component.
        val sample = MediaTargetSample(targetTrack, buffer, info)
        if (isStarted) {
            mediaMuxer.writeSampleData(sample.targetTrack, sample.buffer, sample.info)
        } else {
            // The NativeMediaMuxer is not yet started, so queue up incoming buffers to write them out later
            queue.addLast(sample)
        }
    }

    override fun release() {
        mediaMuxer.release()
    }

    override fun getOutputFilePath() = outputFilePath
}