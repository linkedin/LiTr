/*
 * Copyright 2019 LinkedIn Corporation
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
 * Simple implementation of [AudioResampler] that doesn't resample.
 * It simply creates a duplicate of a source [Frame]
 */

internal class PassthroughResampler : AudioResampler {

    override fun resample(frame: Frame): Frame {
        return frame.buffer?.let { inputBuffer ->
            val buffer = ByteBuffer.allocate(inputBuffer.limit())
            buffer.put(inputBuffer)
            buffer.flip()

            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.set(
                0,
                frame.bufferInfo.size,
                frame.bufferInfo.presentationTimeUs,
                frame.bufferInfo.flags
            )

            Frame(frame.tag, buffer, bufferInfo)
        } ?: throw IllegalArgumentException("Frame doesn't have a buffer, cannot resize!")
    }

    override fun release() {}
}