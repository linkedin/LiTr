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
import java.nio.ByteOrder
import kotlin.math.ceil

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

    override fun processFrame(frame: Frame): Frame {
        return frame.buffer?.let { sourceBuffer ->
            val sourceSampleCount = frame.bufferInfo.size / (BYTES_PER_SAMPLE * sourceChannelCount)
            val estimatedTargetSampleCount = ceil(sourceSampleCount * samplingRatio).toInt()
            val targetBuffer =
                ByteBuffer.allocateDirect(estimatedTargetSampleCount * targetChannelCount * BYTES_PER_SAMPLE)
                    .order(ByteOrder.LITTLE_ENDIAN)

            val targetSampleCount = processAudioFrame(sourceBuffer, sourceSampleCount, targetBuffer)

            val targetBufferSize = targetSampleCount * BYTES_PER_SAMPLE * targetChannelCount
            targetBuffer.limit(targetBufferSize)

            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.size = targetBufferSize
            bufferInfo.offset = 0
            bufferInfo.presentationTimeUs = presentationTimeNs
            bufferInfo.flags = frame.bufferInfo.flags

            this.presentationTimeNs += (targetSampleCount * sampleDurationUs).toLong()

            Frame(frame.tag, targetBuffer, bufferInfo)
        } ?: throw IllegalArgumentException("Frame doesn't have a buffer, cannot process it!")
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
