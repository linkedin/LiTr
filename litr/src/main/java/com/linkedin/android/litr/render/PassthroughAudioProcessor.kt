/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import com.linkedin.android.litr.codec.Frame
import java.lang.IllegalArgumentException

/**
 * Simple implementation of [AudioProcessor] that duplicates a source frame into target frame.
 */

internal class PassthroughAudioProcessor : AudioProcessor {

    override fun processFrame(sourceFrame: Frame, targetFrame: Frame) {
        if (sourceFrame.buffer != null && targetFrame.buffer != null) {
            targetFrame.buffer.put(sourceFrame.buffer)
            targetFrame.buffer.flip()

            targetFrame.bufferInfo.apply {
                offset = 0
                size = sourceFrame.bufferInfo.size
                presentationTimeUs = sourceFrame.bufferInfo.presentationTimeUs
                flags = sourceFrame.bufferInfo.flags
            }
        } else {
            throw IllegalArgumentException("Source or target frame doesn't have a buffer, cannot process it!")
        }
    }

    override fun release() {}
}