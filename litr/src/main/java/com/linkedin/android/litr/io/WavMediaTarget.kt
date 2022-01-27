/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
// header implementation by Kevin Mark is taken from https://gist.github.com/kmark/d8b1b01fb0d2febf5770 and modified
package com.linkedin.android.litr.io

import android.media.MediaCodec
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.IllegalStateException

private const val BYTES_PER_SAMPLE = 2
private const val MAX_SIZE = 4294967295

/**
 * Implementation of [MediaTarget] that writes a single audio track to WAV file.
 * Accepts only one track in "audio-raw" format that has channel count and sample rate data.
 * Track rata must be in 16 bit little endian PCM format, e.g. coming from a direct ByteBuffer.
 */
class WavMediaTarget(
    private val targetPath: String
) : MediaTarget {

    private val tracks = mutableListOf<MediaFormat>()
    private val outputStream: OutputStream
    private var size: Long = 0

    init {
        outputStream = FileOutputStream(File(targetPath))
    }

    override fun addTrack(mediaFormat: MediaFormat, targetTrack: Int): Int {
        return if (tracks.size == 0 &&
            mediaFormat.containsKey(MediaFormat.KEY_MIME) &&
            mediaFormat.getString(MediaFormat.KEY_MIME) == "audio/raw" &&
            mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) &&
            mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            tracks.add(mediaFormat)
            writeWavHeader(
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                BYTES_PER_SAMPLE
            )
            0
        } else {
            -1
        }
    }

    override fun writeSampleData(targetTrack: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        size += info.size
        if (size >= MAX_SIZE) {
            release()
            throw IllegalStateException("WAV file size cannot exceed $MAX_SIZE bytes")
        }
        outputStream.write(buffer.array(), info.offset, info.size)
    }

    override fun release() {
        outputStream.close()
        updateWavHeader()
    }

    override fun getOutputFilePath(): String {
        return targetPath
    }

    // modified version of https://gist.github.com/kmark/d8b1b01fb0d2febf5770#file-audiorecordactivity-java-L288
    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param channelCount  number of channels
     * @param sampleRate sample rate in hertz
     * @param bytesPerSample number of bytes per audio channel sample
     */
    private fun writeWavHeader(channelCount: Int, sampleRate: Int, bytesPerSample: Int) {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        val littleBytes = ByteBuffer
            .allocate(14)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(channelCount.toShort())
            .putInt(sampleRate)
            .putInt(sampleRate * channelCount * bytesPerSample)
            .putShort((channelCount * bytesPerSample).toShort())
            .putShort((bytesPerSample * 8).toShort())
            .array()

        // Not necessarily the best, but it's very easy to visualize this way
        outputStream.write(byteArrayOf( // RIFF header
            'R'.toByte(), 'I'.toByte(), 'F'.toByte(), 'F'.toByte(),  // ChunkID
            0, 0, 0, 0,  // ChunkSize (must be updated later)
            'W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte(),  // Format
            // fmt subchunk
            'f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte(),  // Subchunk1ID
            16, 0, 0, 0,  // Subchunk1 Size
            1, 0,  // AudioFormat
            littleBytes[0], littleBytes[1],  // NumChannels
            littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5],  // SampleRate
            littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9],  // ByteRate
            littleBytes[10], littleBytes[11],  // BlockAlign
            littleBytes[12], littleBytes[13],  // BitsPerSample
            // data subchunk
            'd'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte(),  // Subchunk2 ID
            0, 0, 0, 0))
    }

    // modified version of https://gist.github.com/kmark/d8b1b01fb0d2febf5770#file-audiorecordactivity-java-L331
    /**
     * Updates the given wav file's header to include the final chunk sizes
     */
    private fun updateWavHeader() {
        val targetFile = File(targetPath)
        val sizes = ByteBuffer
            .allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt((targetFile.length() - 8).toInt()) // ChunkSize
            .putInt((targetFile.length() - 44).toInt()) // Subchunk2Size
            .array()
        var accessWave: RandomAccessFile? = null
        try {
            accessWave = RandomAccessFile(targetFile, "rw")
            // ChunkSize
            accessWave.seek(4)
            accessWave.write(sizes, 0, 4)

            // Subchunk2Size
            accessWave.seek(40)
            accessWave.write(sizes, 4, 4)
        } catch (ex: IOException) {
            throw ex
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close()
                } catch (ex: IOException) {
                    // fail silently
                }
            }
        }
    }
}
