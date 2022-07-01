/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter.audio

import android.media.MediaFormat
import androidx.annotation.FloatRange
import com.linkedin.android.litr.codec.Frame
import com.linkedin.android.litr.filter.BufferFilter
import kotlin.math.pow

private const val SAMPLE_SIZE = 2 // audio sample size on Android is 2 bytes per channel
private const val BASE = 10.0 // volume is logarithmic, we will use base 10

/**
 * An audio filter that changes the audio track volume:
 * - 1 will keep the volume "as is"
 * - 0 will silence the track
 * - value <1 will lower the volume
 * - value >1 will increase it. One has to be careful with these, since large values may result in distortion.
 */
class VolumeFilter(@FloatRange(from = 0.0) private val volume: Double) : BufferFilter{

    override fun init(mediaFormat: MediaFormat?) {}

    override fun apply(frame: Frame) {
        frame.buffer?.asShortBuffer()?.let { buffer ->
            val sampleCount = frame.bufferInfo.size / SAMPLE_SIZE
            repeat(sampleCount) { index ->
                // replace sample at index with volume adjusted value
                buffer.put(
                    index,
                    (buffer.get(index) * (BASE.pow(volume) -1 ) / (BASE - 1)).toInt().toShort()
                )
            }
        }
    }

    override fun release() {}
}
