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

import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

private const val TAG = "NativeMediaMuxer"

private const val MUXER_STATE_UNINITIALIZED = -1
private const val MUXER_STATE_INITIALIZED = 0
private const val MUXER_STATE_STARTED = 1
private const val MUXER_STATE_STOPPED = 2

private fun convertMuxerStateCodeToString(state: Int): String {
    return when(state) {
        MUXER_STATE_UNINITIALIZED -> { "UNINITIALIZED" }
        MUXER_STATE_INITIALIZED -> { "INITIALIZED" }
        MUXER_STATE_STARTED -> { "STARTED" }
        MUXER_STATE_STOPPED -> { "STOPPED " }
        else -> { "UNKNOWN" }
    }
}

/**
 * A Muxer that is based off Android's built in MediaMuxer, but internally uses ffmpeg's libavformat
 * for generating the output file. This allows support for a lot more advanced formats.
 */
class NativeMediaMuxer(val path: String, val format: String) {
    private var nativeObject: Long = 0L
    private var state: Int = MUXER_STATE_UNINITIALIZED

    private var lastTrackIndex = -1

    // Muxers support a number of options when started. This will control their behaviour, for
    // example MP4 vs fMP4. We store the configured options to pass through to libavformat when
    // we're about to start muxing.
    private var options = mutableMapOf<String, String>()

    init {
        // Initialise the native muxer.
        nativeObject = nativeSetup(path, format)
        state = MUXER_STATE_INITIALIZED
    }

    /**
     * Adds a track with the specified format.
     */
    fun addTrack(format: MediaFormat): Int {
        if (state != MUXER_STATE_INITIALIZED) {
            throw IllegalStateException("Muxer is not initialized.")
        }
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }

        // Add the track to the muxer.
        val extra = format.getExtraData()
        val trackIndex = if (format.isAudio()) {
            nativeAddAudioTrack(
                    nativeObject,
                    format.getCodecId(),
                    format.getBitrate(),
                    format.getChannelCount(),
                    format.getSampleRate(),
                    format.getSampleSize(),
                    extra,
                    extra?.capacity() ?: 0
            )
        } else if (format.isVideo()) {
            nativeAddVideoTrack(
                    nativeObject,
                    format.getCodecId(),
                    format.getBitrate(),
                    format.getWidth(),
                    format.getHeight(),
                    extra,
                    extra?.capacity() ?: 0
            )
        } else {
            error("Unable to add unsupported track")
        }

        // The returned track index is expected to be incremented as addTrack succeeds. However, if
        // the format is invalid, it will get a negative track index.
        if (lastTrackIndex >= trackIndex) {
            throw IllegalStateException("Invalid format")
        }

        Log.i(TAG, "Stream Added (ID: ${format.getCodecId()} Index: $trackIndex)")
        lastTrackIndex = trackIndex
        return trackIndex
    }

    /**
     * Adds a muxer option. This method *must* be called before the muxer has been started.
     */
    fun addOption(key: String, value: String) {
        if (state != MUXER_STATE_INITIALIZED) {
            throw IllegalStateException("Muxer is not initialized.")
        }
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }

        // Store the option for later.
        Log.i(TAG, "Option Added (Key: $key Value: $value)")
        options[key] = value
    }

    /**
     * Starts the muxer.
     */
    fun start() {
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }

        if (state == MUXER_STATE_INITIALIZED) {
            nativeStart(nativeObject, options.keys.toTypedArray(), options.values.toTypedArray())
            state = MUXER_STATE_STARTED
        } else {
            throw IllegalStateException(
                    "Can't start due to wrong state (${convertMuxerStateCodeToString(state)})")
        }
    }

    /**
     * Stops the muxer.
     */
    fun stop() {
        if (state == MUXER_STATE_STARTED) {
            try {
                nativeStop(nativeObject)
                Log.i(TAG, "Muxer stopped successfully")
            } catch (e: Exception) {
                throw e
            } finally {
                state = MUXER_STATE_STOPPED
            }
        } else {
            throw IllegalStateException(
                    "Can't stop due to wrong state (${convertMuxerStateCodeToString(state)})")
        }
    }

    /**
     * Writes an encoded sample into the muxer.
     */
    fun writeSampleData(trackIndex: Int, byteBuf: ByteBuffer, bufferInfo: BufferInfo) {
        if (trackIndex < 0 || trackIndex > lastTrackIndex) {
            throw IllegalArgumentException("trackIndex is invalid")
        }
        if (bufferInfo.size < 0 || bufferInfo.offset < 0 ||
                (bufferInfo.offset + bufferInfo.size) > byteBuf.capacity()) {
            throw IllegalArgumentException("bufferInfo must specify a valid buffer offset and size")
        }
        if (!byteBuf.hasArray()) {
            throw IllegalArgumentException("byteBuf must have an accessible buffer.")
        }
        if (nativeObject == 0L) {
            throw IllegalStateException("Muxer has been released")
        }
        if (state != MUXER_STATE_STARTED) {
            throw IllegalStateException("Can't write, muxer is not started")
        }

        // Pass the buffer and info to the native layer.
        nativeWriteSampleData(
                nativeObject,
                trackIndex,
                byteBuf,
                bufferInfo.offset,
                bufferInfo.size,
                bufferInfo.presentationTimeUs,
                bufferInfo.flags
        )
    }

    /**
     * Make sure you call this when you're done to free up any resources, instead of relying on the
     * garbage collector to do this for you at some point in the future.
     */
    fun release() {
        if (state == MUXER_STATE_STARTED) {
            stop()
        }

        if (nativeObject != 0L) {
            nativeRelease(nativeObject)
            nativeObject = 0L
        }

        state = MUXER_STATE_UNINITIALIZED
    }

    protected fun finalize() {
        if (nativeObject != 0L) {
            nativeRelease(nativeObject)
            nativeObject = 0L
        }
    }

    private external fun nativeSetup(outputPath: String, formatName: String): Long
    private external fun nativeRelease(nativeObject: Long)
    private external fun nativeStart(nativeObject: Long, keys: Array<String>, values: Array<String>)
    private external fun nativeStop(nativeObject: Long)
    private external fun nativeAddAudioTrack(nativeObject: Long, codecId: String, bitrate: Int,
                                             channelCount: Int, sampleRate: Int, frameSize: Int,
                                             byteBuf: ByteBuffer?, size: Int): Int
    private external fun nativeAddVideoTrack(nativeObject: Long, codecId: String, bitrate: Int,
                                             width: Int, height: Int, byteBuf: ByteBuffer?,
                                             size: Int): Int
    private external fun nativeWriteSampleData(nativeObject: Long, trackIndex: Int,
                                               byteBuf: ByteBuffer, offset: Int, size: Int,
                                               presentationTimeUs: Long, flags: Int)

    companion object {
        init {
            // Static initializer to ensure that our native libraries are loaded and we have
            // configured their logging to be captured by Android.
            NativeMuxersLib.loadLibraries()
            NativeLogger.setup(
                    if (BuildConfig.DEBUG)
                        NativeLogger.LEVEL_TRACE
                    else
                        NativeLogger.LEVEL_WARNING
            )
        }
    }
}