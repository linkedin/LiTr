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
import java.nio.ByteBuffer

private const val BYTES_PER_SAMPLE = 2

/**
 * Implementation of audio processor that uses Oboe library
 */
internal class OboeAudioProcessor(
    private val sourceChannelCount: Int,
    sourceSampleRate: Int,
    private val targetChannelCount: Int,
    targetSampleRate: Int
) : AudioProcessor {

    private val samplingRatio: Double
    private var sampleDurationUs: Double
    private var presentationTimeNs: Long

    init {
        initProcessor(sourceChannelCount, sourceSampleRate, targetChannelCount, targetSampleRate)
        samplingRatio = targetSampleRate.toDouble() / sourceSampleRate
        sampleDurationUs = 1_000_000.0 / targetSampleRate
        presentationTimeNs = 0
    }

    override fun processFrame(sourceFrame: Frame, targetFrame: Frame) {
        if (sourceFrame.buffer != null && targetFrame.buffer != null) {
            val sourceSampleCount = sourceFrame.bufferInfo.size / (BYTES_PER_SAMPLE * sourceChannelCount)
            val targetSampleCount = processAudioFrame(sourceFrame.buffer, sourceSampleCount, targetFrame.buffer)

            val targetBufferSize = targetSampleCount * BYTES_PER_SAMPLE * targetChannelCount
            targetFrame.buffer.rewind()
            targetFrame.buffer.limit(targetBufferSize)
            targetFrame.bufferInfo.set(
                0,
                targetBufferSize,
                presentationTimeNs,
                sourceFrame.bufferInfo.flags
            )

            this.presentationTimeNs += (targetSampleCount * sampleDurationUs).toLong()
        } else {
            throw IllegalArgumentException("Source or target frame doesn't have a buffer, cannot process it!")
        }
    }

    override fun release() {
        releaseProcessor()
    }

    private external fun initProcessor(sourceChannelCount: Int, sourceSampleRate: Int, targetChannelCount: Int, targetSampleRate: Int)

    private external fun processAudioFrame(sourceBuffer: ByteBuffer, sampleCount: Int, targetBuffer: ByteBuffer): Int

    private external fun releaseProcessor()

    companion object {
        init {
            System.loadLibrary("litr-jni")
        }
    }
}
