/*
 * Copyright 2023 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 *
 * Author: Ian Bird
 */
package com.linkedin.android.litr.io

import android.media.MediaCodec
import java.nio.ByteBuffer

/**
 * If a {@link MediaTarget} needs to temporarily queue up samples, this class can provide a deep
 * copy of the sample to allow the original to be returned (e.g. to the encoder).
 */
class MediaTargetSample(
    val targetTrack: Int,
    buffer: ByteBuffer,
    info: MediaCodec.BufferInfo
) {
    val buffer: ByteBuffer = ByteBuffer.allocate(buffer.capacity())
    val info : MediaCodec.BufferInfo = MediaCodec.BufferInfo()

    init {
        this.info.set(0, info.size, info.presentationTimeUs, info.flags)

        // We want to make a deep copy so that we can release the incoming buffer back to the
        // encoder immediately.
        this.buffer.put(buffer)
        this.buffer.flip()
    }
}
