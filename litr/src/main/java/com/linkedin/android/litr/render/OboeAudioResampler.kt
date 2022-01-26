package com.linkedin.android.litr.render

import android.media.MediaCodec
import com.linkedin.android.litr.codec.Frame
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

private const val BYTES_PER_SAMPLE = 2

/**
 * Implementation of audio resampler that uses Oboe library
 */
class OboeAudioResampler(
    private val channelCount: Int,
    sourceSampleRate: Int,
    targetSampleRate: Int
) : AudioResampler {

    private val samplingRatio: Double
    private var sampleDurationUs: Double
    private var presentationTimeNs: Long

    init {
        initResampler(channelCount, sourceSampleRate, targetSampleRate)
        samplingRatio = targetSampleRate.toDouble() / sourceSampleRate
        sampleDurationUs = 1_000_000.0 / targetSampleRate
        presentationTimeNs = 0
    }

    override fun resample(frame: Frame): Frame {
        return frame.buffer?.let { sourceBuffer ->
            val sourceSampleCount = frame.bufferInfo.size / (BYTES_PER_SAMPLE * channelCount)
            val estimatedTargetSampleCount = ceil(sourceSampleCount * samplingRatio).toInt()
            val targetBuffer =
                ByteBuffer.allocateDirect(estimatedTargetSampleCount * channelCount * BYTES_PER_SAMPLE)
                    .order(ByteOrder.LITTLE_ENDIAN)

            val targetSampleCount = resample(sourceBuffer, sourceSampleCount, targetBuffer)

            val targetBufferSize = targetSampleCount * BYTES_PER_SAMPLE * channelCount
            targetBuffer.limit(targetBufferSize)

            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.size = targetBufferSize
            bufferInfo.offset = 0
            bufferInfo.presentationTimeUs = presentationTimeNs
            bufferInfo.flags = frame.bufferInfo.flags

            this.presentationTimeNs += (targetSampleCount * sampleDurationUs).toLong()

            Frame(frame.tag, targetBuffer, bufferInfo)
        } ?: throw IllegalArgumentException("Frame doesn't have a buffer, cannot resize!")
    }

    override fun release() {
        releaseResampler()
    }

    private external fun initResampler(channelCount: Int, sourceSampleRate: Int, targetSampleRate: Int)

    private external fun resample(sourceBuffer: ByteBuffer, sampleCount: Int, targetBuffer: ByteBuffer): Int

    private external fun releaseResampler()

    companion object {
        init {
            System.loadLibrary("litr-jni")
        }
    }
}
