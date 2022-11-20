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

import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.linkedin.android.litr.MimeType
import com.linkedin.android.litr.exception.MediaSourceException
import java.nio.ByteBuffer

private const val TAG = "AudioRecordMediaSource"

private const val MEDIA_FORMAT_PCM_KEY = "pcm-encoding"
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val BYTES_PER_SAMPLE = 2

/**
 * An implementation of MediaSource, which utilizes Android's {@link AudioRecord} to record audio
 * from a given audio source (i.e. the device's microphone).
 *
 * This class requires Android M+ in order to support reading data from {@link AudioRecord} in a
 * non-blocking way.
 */
@RequiresApi(Build.VERSION_CODES.M)
class AudioRecordMediaSource(
        private val audioSource: Int = DEFAULT_AUDIO_SOURCE,
        private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
        private val channelCount: Int = DEFAULT_CHANNEL_COUNT
) : MediaSource {

    private val channelConfig = when(channelCount) {
        1 -> AudioFormat.CHANNEL_IN_MONO
        2 -> AudioFormat.CHANNEL_IN_STEREO
        else -> throw IllegalArgumentException("Unsupported channel count configuration")
    }

    // Compute the appropriate buffer size based upon the audio configuration.
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AUDIO_FORMAT) * 8

    // Compute the number of bytes per microsecond of audio.
    private val bytesPerUs = (sampleRate * BYTES_PER_SAMPLE * channelCount) / 1000000.0

    // The AudioRecord instance used to record the audio source.
    private var audioRecord: AudioRecord? = null

    // We track the amount of 16-bit PCM we have returned and calculate its duration, in order to
    // know the presentation time of the next sample.
    private var nextSampleTimeUs = 0L
    private var sampleIncrementUs = 0L
    private var totalDurationUs = 0L

    private var isRecording = false

    @Synchronized
    fun startRecording() {
        createAudioRecord()

        // Check to make sure the AudioRecord instance is initialized.
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Error initializing AudioRecord")
            throw MediaSourceException(
                    MediaSourceException.Error.DATA_SOURCE,
                    null,
                    IllegalStateException("Error initializing AudioRecord: ${audioRecord?.state}"))
        }

        // Start recording.
        audioRecord?.startRecording()
        isRecording = true
    }

    private fun createAudioRecord() {
        audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                AUDIO_FORMAT,
                bufferSize
        )
    }

    @Synchronized
    fun stopRecording() {
        if (isRecording) {
            // Stop recording.
            audioRecord?.stop()
            isRecording = false
        }
    }

    override fun getOrientationHint() = 0

    override fun getTrackCount() = 1

    override fun getTrackFormat(track: Int) = MediaFormat.createAudioFormat(
            MimeType.AUDIO_RAW,
            sampleRate,
            channelCount
    ).apply {
        setInteger(MEDIA_FORMAT_PCM_KEY, AudioFormat.ENCODING_PCM_16BIT)
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
        if (!isRecording) {
            return -1
        }

        // Slice the buffer at the given offset.
        val sampleBuffer = buffer.sliceAtOffset(offset)

        // Read the next audio sample from the recorder. As per the AudioRecord documentation, we
        // need to leave some buffer for the AudioRecord to continue to queue too. We also don't
        // want to block waiting for more data, we'll take whatever is available.
        val targetSize = (bufferSize / 2).coerceAtMost(sampleBuffer.capacity())
        val readBytes = audioRecord?.read(sampleBuffer, targetSize, AudioRecord.READ_NON_BLOCKING) ?: -1

        // We look at how much data has been returned, to know the duration of that sample. We
        // therefore know when the next sample will start, which we track.
        sampleIncrementUs = (readBytes / bytesPerUs).toLong()
        return readBytes
    }

    override fun getSampleTime(): Long {
        // If the recording has been stopped, then there are no more samples to be read.
        if (!isRecording) {
            return -1
        }

        val sampleTime = nextSampleTimeUs
        nextSampleTimeUs += sampleIncrementUs
        totalDurationUs += sampleIncrementUs
        return sampleTime
    }

    override fun getSampleFlags(): Int {
        // If the recording has been stopped, then any further samples read should report the end of
        // the stream.
        if (!isRecording) {
            return MediaCodec.BUFFER_FLAG_END_OF_STREAM
        }

        return 0
    }

    override fun advance() {
        // Nothing to advance.
    }

    override fun release() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        nextSampleTimeUs = 0L
        sampleIncrementUs = 0L

        if (totalDurationUs > 0L) {
            Log.i(TAG, "Audio Duration: $totalDurationUs")
        }

        totalDurationUs = 0L
    }

    override fun getSize() = -1L

    /**
     * Slices a ByteBuffer at a given Offset.
     */
    private fun ByteBuffer.sliceAtOffset(offset: Int): ByteBuffer {
        // Check to make sure we haven't been given an offset which is outside of our ByteBuffer.
        if (offset > capacity() - 1) {
            return this
        }

        val original = position()
        try {
            // Modify the buffer's position to the given offset, so that the returned slice will
            // start there.
            position(offset)
            return slice()
        } finally {
            // Reset the position back to it's original value.
            position(original)
        }
    }

    companion object {
        const val DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val DEFAULT_SAMPLE_RATE = 44100
        const val DEFAULT_CHANNEL_COUNT = 1
    }
}
