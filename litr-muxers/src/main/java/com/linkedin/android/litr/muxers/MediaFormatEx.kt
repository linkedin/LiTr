/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.muxers

import android.media.MediaFormat
import com.linkedin.android.litr.MimeType
import java.nio.ByteBuffer

const val KEY_MIME_TYPE = MediaFormat.KEY_MIME
const val KEY_BIT_RATE = MediaFormat.KEY_BIT_RATE
const val KEY_WIDTH = MediaFormat.KEY_WIDTH
const val KEY_HEIGHT = MediaFormat.KEY_HEIGHT
const val KEY_FRAME_RATE = MediaFormat.KEY_FRAME_RATE
const val KEY_CHANNEL_COUNT = MediaFormat.KEY_CHANNEL_COUNT
const val KEY_SAMPLE_RATE = MediaFormat.KEY_SAMPLE_RATE

fun MediaFormat.isVideo(): Boolean {
    val mimeType = getStringSafe(KEY_MIME_TYPE, "")
    return MimeType.IsVideo(mimeType)
}

fun MediaFormat.isAudio(): Boolean {
    val mimeType = getStringSafe(KEY_MIME_TYPE, "")
    return MimeType.IsAudio(mimeType)
}

fun MediaFormat.getCodecId(): String {
    // We ideally just use the mime type name, but there are some required substitutions.
    return when(val mimeType = getStringSafe(KEY_MIME_TYPE, "")) {
        MimeType.AUDIO_AAC -> "aac"

        MimeType.VIDEO_AVC -> "h264"
        MimeType.VIDEO_HEVC -> "hevc"
        else -> {
            val components = mimeType.split("/")
            return components.last()
        }
    }
}

fun MediaFormat.getBitrate() = getIntSafe(KEY_BIT_RATE, 0)

fun MediaFormat.getChannelCount() = getIntSafe(KEY_CHANNEL_COUNT, 0)

fun MediaFormat.getSampleRate() = getIntSafe(KEY_SAMPLE_RATE, 0)

fun MediaFormat.getWidth() = getIntSafe(KEY_WIDTH, 0)

fun MediaFormat.getHeight() = getIntSafe(KEY_HEIGHT, 0)

fun MediaFormat.getExtraData(): ByteBuffer? {
    val buffers = mutableListOf<ByteBuffer>()
    var targetBufferSize = 0

    // A MediaFormat can contain 0-N buffers that represent the codec specific data (aka csd). We
    // need to combine these individual buffers into a single ByteBuffer. Let's start by finding all
    // the associated buffers and how large our final buffer will be.
    var index = 0
    while (true) {
        val buffer = getByteBuffer("csd-$index") ?: break

        buffers.add(buffer)
        targetBufferSize += buffer.capacity()
        index++
    }

    // Check to see if we found at least one non-empty buffer.
    if (targetBufferSize == 0) {
        return null
    }

    // Build the final ByteBuffer and add each original buffer sequentially.
    val extraBuffer = ByteBuffer.allocate(targetBufferSize)
    for (buffer in buffers) {
        extraBuffer.put(buffer)
    }

    extraBuffer.position(0)
    return extraBuffer
}

fun MediaFormat.getSampleSize(): Int {
    return when(getStringSafe(KEY_MIME_TYPE, "")) {
        MimeType.AUDIO_AAC -> 1024
        else -> 0
    }
}

private fun MediaFormat.getStringSafe(key: String, default: String): String {
    return getString(key) ?: default
}

private fun MediaFormat.getIntSafe(key: String, default: Int): Int {
    return runCatching { getInteger(key) }.getOrDefault(default)
}