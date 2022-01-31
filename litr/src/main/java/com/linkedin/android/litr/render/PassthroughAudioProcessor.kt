/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import android.media.MediaCodec
import com.linkedin.android.litr.codec.Frame
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

/**
 * Simple implementation of [AudioProcessor] that duplicates a source frame into target frame.
 */

internal class PassthroughAudioProcessor : AudioProcessor {

    override fun processFrame(frame: Frame): Frame {
        return frame.buffer?.let { inputBuffer ->
            val buffer = ByteBuffer.allocate(inputBuffer.limit())
            buffer.put(inputBuffer)
            buffer.flip()

            val bufferInfo = MediaCodec.BufferInfo().apply {
                offset = 0
                size = frame.bufferInfo.size
                presentationTimeUs = frame.bufferInfo.presentationTimeUs
                flags = frame.bufferInfo.flags
            }

            Frame(frame.tag, buffer, bufferInfo)
        } ?: throw IllegalArgumentException("Frame doesn't have a buffer, cannot process it!")
    }

    override fun release() {}
}