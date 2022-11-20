/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.io

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.linkedin.android.litr.MimeType
import com.linkedin.android.litr.codec.Decoder
import com.linkedin.android.litr.codec.Frame
import com.linkedin.android.litr.codec.PassthroughDecoder
import com.linkedin.android.litr.exception.MediaSourceException
import java.nio.ByteBuffer

private const val TAG = "CaptureMediaSource"
private const val DECODER_NAME = "CaptureMediaSource.Decoder"

private const val DEFAULT_FRAME_RATE = 30
private const val DEFAULT_BITRATE = 10_000_000
private const val DEFAULT_KEY_FRAME_INTERVAL = 5

/**
 * An implementation of MediaSource, which exposes the input {@link Surface} of the encoder via a
 * Callback. An instance of this class is expected to be both the MediaSource, and Decoder for which
 * the pipeline is configured. This allows these components to be bypassed.
 */
open class CaptureMediaSource: MediaSource, Decoder {
    /**
     * Callback which notifies when the input surface is available to be written too.
     */
    interface Callback {
        fun onInputSurfaceAvailable(inputSurface: Surface)
        fun onFrameSkipped(frameSkipCount: Int)
    }

    var width = 0
    var height = 0
    var bitrate = DEFAULT_BITRATE
    var keyFrameInterval = DEFAULT_KEY_FRAME_INTERVAL
    var orientation = 0

    private var callback: Callback? = null

    // We compute the presentation time stamps ourselves using the known (fixed) frame rate to know
    // the duration of each frame.
    private var sampleIncrementUs = getSampleIncrementUs(DEFAULT_FRAME_RATE)
    var frameRate = DEFAULT_FRAME_RATE
        set(value) {
            field = value

            // After the frame rate has been modified, we need to make sure we update our sample
            // increment.
            sampleIncrementUs = getSampleIncrementUs(value)
        }

    // Internally, we use the PassthroughDecoder to handle input and output Frames. This is because
    // we are rendering to the Surface directly, but still need to manage input/output presentation
    // times as well as flags.
    private var passthroughDecoder = PassthroughDecoder(1)

    private var frameCounter = 0
    private var frameAvailable = false
    private var frameSkipCount = 0
    private var isCapturing = false

    /**
     * Sets the callback that will be notified when the input Surface is available, and details of
     * any skipped frames.
     */
    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    //region MediaSource...
    override fun getOrientationHint() = orientation

    override fun getTrackCount() = 1

    override fun getTrackFormat(track: Int): MediaFormat {
        return MediaFormat.createVideoFormat(
                MimeType.VIDEO_RAW,
                width,
                height
        ).apply {
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        }
    }

    override fun selectTrack(track: Int) {
        // Since we only support a single (audio) track, there is nothing to select.
    }

    override fun seekTo(position: Long, mode: Int) {
        // We don't support seeking, since we're recording live.
    }

    override fun getSampleTrackIndex() = 0

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        // If the recording has been stopped, then there are no more samples to be read.
        if (!isCapturing) {
            Log.i(TAG, "Reporting no more samples")
            return -1
        }

        // There is no real data to read, but we can pretend...
        return 0
    }

    override fun getSampleTime(): Long {
        // If the recording has been stopped, then there are no more samples to be read.
        if (!isCapturing) {
            Log.i(TAG, "Reporting no more sample times")
            return -1
        }

        // We overwrite the presentation time stamp when decoding, to match which frame we expect to
        // be rendering. This is more reliable than trying to guess/assume inside the MediaSource.
        return 0
    }

    override fun getSampleFlags(): Int {
        // If the capturing has been stopped, then any further samples read should report the end of
        // the stream.
        if (!isCapturing) {
            Log.i(TAG, "Reporting end of stream")
            return MediaCodec.BUFFER_FLAG_END_OF_STREAM
        }

        return 0
    }

    override fun advance() {
        // Nothing to advance.
    }

    override fun getSize() = -1L

    //endregion MediaSource

    //region Decoder

    override fun init(mediaFormat: MediaFormat, surface: Surface?) {
        passthroughDecoder.init(mediaFormat, surface)

        // Notify via our Callback that the Surface has become available.
        surface?.let { callback?.onInputSurfaceAvailable(it) }
    }

    override fun start() {
        // Check to make sure we've been configured with a valid width and height.
        if (width <= 0 || height <= 0) {
            throw MediaSourceException(
                    MediaSourceException.Error.DATA_SOURCE,
                    null,
                    IllegalStateException("Invalid width and height"))
        }

        passthroughDecoder.start()
        isCapturing = true
    }

    override fun isRunning() = passthroughDecoder.isRunning

    override fun dequeueInputFrame(timeout: Long) = passthroughDecoder.dequeueInputFrame(timeout)

    override fun getInputFrame(tag: Int) = passthroughDecoder.getInputFrame(tag)

    override fun queueInputFrame(frame: Frame) = passthroughDecoder.queueInputFrame(frame)

    override fun dequeueOutputFrame(timeout: Long) = passthroughDecoder.dequeueOutputFrame(timeout)

    override fun getOutputFrame(tag: Int): Frame? {
        // The input frame will not have a correct presentation time stamp. We will therefore
        // update it based upon which frame we expect to be rendering.
        return passthroughDecoder.getOutputFrame(tag)?.apply {
            val frame = (frameCounter - 1).coerceAtLeast(0)
            bufferInfo.presentationTimeUs = frame * sampleIncrementUs
        }
    }

    override fun releaseOutputFrame(tag: Int, render: Boolean) {
        if (render && !frameAvailable) {
            Log.e(TAG, "Frame not yet available")
        }

        passthroughDecoder.releaseOutputFrame(tag, false)
        frameAvailable = false
    }

    override fun getOutputFormat() = getTrackFormat(0)

    override fun stop() {
        passthroughDecoder.stop()
    }

    override fun getName() = DECODER_NAME

    //endregion

    /**
     * Method to be called when a new frame is available from the Surface provided via
     * {@link Callback}.
     */
    fun onFrameAvailable() {
        // If a frame is still available when a new frame becomes available, this means it will be
        // skipped in the output. The encoder hasn't had the opportunity to read it from the
        // Surface. Too many skipped frames could suggest that we're trying to capture a video at a
        // quality that is too high for the device's encoder.
        if (frameAvailable && isCapturing) {
            frameSkipCount++

            Log.e(TAG, "Frame Skipped (Count: $frameSkipCount)")
            callback?.onFrameSkipped(frameSkipCount)
        }

        frameCounter += 1
        frameAvailable = true
    }

    /**
     * Method to notify that the Media Source should expect no further renders. This will allow the
     * source to notify that we've hit the end of the stream, allowing the transcode to complete.
     */
    fun stopExternal() {
        isCapturing = false
    }

    override fun release() {
        passthroughDecoder.release()
        isCapturing = false

        if (frameCounter > 0) {
            Log.i(TAG, "Frame Count: $frameCounter")
            Log.i(TAG, "Video Duration: ${frameCounter * sampleIncrementUs}")
        }

        if (frameSkipCount > 0) {
            Log.e(TAG, "Frame Skip Count: $frameSkipCount")
        }

        frameCounter = 0
        frameSkipCount = 0
    }

    /**
     * Computes the duration of each sample based upon the target frame rate, in microseconds.
     */
    private fun getSampleIncrementUs(frameRate: Int): Long {
        val frameIncrementS = 1.0 / frameRate
        return (frameIncrementS * 1000000).toLong()
    }
}
