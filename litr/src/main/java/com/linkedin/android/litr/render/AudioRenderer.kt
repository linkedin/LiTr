/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.linkedin.android.litr.codec.Encoder
import com.linkedin.android.litr.codec.Frame
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

private const val BYTES_PER_SAMPLE = 2
private const val FRAME_WAIT_TIMEOUT: Long = 0L

private const val TAG = "AudioRenderer"

class AudioRenderer(private val encoder: Encoder) : Renderer {

    private var sourceMediaFormat: MediaFormat? = null
    private var targetMediaFormat: MediaFormat? = null
    private var sampleDurationUs: Float = 0f
    private var channelCount = 2
    private var shouldResample = false

    private var presentationTimeNs: Long = 0

    private var released: AtomicBoolean = AtomicBoolean(false)

    private val renderQueue = LinkedBlockingDeque<Frame>()

    private val renderThread = RenderThread()

    override fun init(outputSurface: Surface?, sourceMediaFormat: MediaFormat?, targetMediaFormat: MediaFormat?) {
        onMediaFormatChanged(sourceMediaFormat, targetMediaFormat)
        released.set(false)
        renderThread.start()
        presentationTimeNs = 0

        shouldResample = false
        if (sourceMediaFormat?.containsKey(MediaFormat.KEY_SAMPLE_RATE) == true &&
            targetMediaFormat?.containsKey(MediaFormat.KEY_SAMPLE_RATE) == true &&
            sourceMediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            val sourceSampleRate = sourceMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val targetSampleRate = targetMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            shouldResample = sourceSampleRate != targetSampleRate
            if (shouldResample) {
                initAudioResampler(channelCount, sourceSampleRate, targetSampleRate)
            }
        }
    }

    override fun onMediaFormatChanged(sourceMediaFormat: MediaFormat?, targetMediaFormat: MediaFormat?) {
        this.sourceMediaFormat = sourceMediaFormat
        this.targetMediaFormat = targetMediaFormat

        if (targetMediaFormat?.containsKey(MediaFormat.KEY_SAMPLE_RATE) == true) {
            sampleDurationUs = 1_000_000f / targetMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        }

        if (targetMediaFormat?.containsKey(MediaFormat.KEY_CHANNEL_COUNT) == true) {
            channelCount = targetMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        }
    }

    override fun getInputSurface(): Surface? {
        return null
    }

    override fun renderFrame(inputFrame: Frame?, presentationTimeNs: Long) {
        if (!released.get() && inputFrame?.buffer != null) {
            val buffer = ByteBuffer.allocate(inputFrame.buffer.limit())
            buffer.put(inputFrame.buffer)
            buffer.flip()
            val bufferInfo = MediaCodec.BufferInfo()

            if (shouldResample) {
                val inputBuffer = buffer.asShortBuffer()
                val numSamples = inputFrame.bufferInfo.size / (BYTES_PER_SAMPLE * channelCount)
                val sourceBuffer = ShortArray(numSamples * channelCount)
                repeat(numSamples * channelCount) { index ->
                    sourceBuffer[index] = inputBuffer.get(index)
                }

                val targetBuffer = ShortArray(numSamples * channelCount)

                // resample in Java
//                val factor = 2
//                val resampledNumSamples = numSamples / factor
//
//
//                for (index in 0 until resampledNumSamples) {
//                    for (channel in 0 until channelCount) {
//                        targetBuffer[index * channelCount + channel] = sourceBuffer[index * factor * channelCount + channel]
//                    }
//                }

                // resample in JNI
                val resampledNumSamples = resample(sourceBuffer, sourceBuffer.size, targetBuffer, targetBuffer.size)

//                val newBuffer = ShortArray(resampledNumSamples * channelCount)
//                for (index in 0 until resampledNumSamples * channelCount) {
//                    newBuffer[index] = targetBuffer[index].toInt().toShort()
//                }

                //Log.d(TAG, "Max source ${sourceBuffer.maxOrNull()} target ${targetBuffer.maxOrNull()}")

                val newSize = resampledNumSamples * BYTES_PER_SAMPLE * channelCount

                val outByteBuffer = ByteBuffer.allocate(newSize)
                outByteBuffer.asShortBuffer().put(targetBuffer, 0, resampledNumSamples * channelCount)

                bufferInfo.size = newSize
                bufferInfo.offset = 0
                bufferInfo.presentationTimeUs = this.presentationTimeNs
                bufferInfo.flags = inputFrame.bufferInfo.flags

                this.presentationTimeNs += (resampledNumSamples * sampleDurationUs).toLong()

                //Log.d(TAG, "Resampled $numSamples to $resampledNumSamples")

                renderQueue.add(Frame(inputFrame.tag, outByteBuffer, bufferInfo))
            } else {
                bufferInfo.set(
                    0,
                    inputFrame.bufferInfo.size,
                    inputFrame.bufferInfo.presentationTimeUs,
                    inputFrame.bufferInfo.flags
                )

                renderQueue.add(Frame(inputFrame.tag, buffer, bufferInfo))
            }
        }
    }

    override fun release() {
        released.set(true)
        if (shouldResample) {
            releaseAudioResampler()
        }
    }

    override fun hasFilters(): Boolean {
        return false
    }

    private inner class RenderThread : Thread() {
        override fun run() {
            while (!released.get()) {
                renderQueue.peekFirst()?.let { inputFrame ->
                    val tag = encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT)
                    when {
                        tag >= 0 -> renderFrame(tag, inputFrame)
                        tag == MediaCodec.INFO_TRY_AGAIN_LATER -> {} // do nothing, will try later
                        else -> Log.e(TAG, "Unhandled value $tag when receiving decoded input frame")
                    }
                }
            }
            renderQueue.clear()
        }

        private fun renderFrame(tag: Int, inputFrame: Frame) {
            encoder.getInputFrame(tag)?.let { outputFrame ->
                if (outputFrame.buffer != null && inputFrame.buffer != null) {
                    outputFrame.bufferInfo.offset = 0
                    outputFrame.bufferInfo.flags = inputFrame.bufferInfo.flags
                    outputFrame.bufferInfo.presentationTimeUs =
                        inputFrame.bufferInfo.presentationTimeUs +
                            ((inputFrame.buffer.position() / (channelCount * BYTES_PER_SAMPLE)) * sampleDurationUs).toLong()

                    if (outputFrame.buffer.limit() >= inputFrame.buffer.remaining()) {
                        // if remaining input bytes fit output buffer, use them all and discard the output buffer
                        outputFrame.bufferInfo.size = inputFrame.buffer.remaining()
                        renderQueue.removeFirst()
                    } else {
                        // otherwise, fill the output buffer and clear its EOS flag
                        outputFrame.bufferInfo.size = outputFrame.buffer.limit()
                        outputFrame.bufferInfo.flags = outputFrame.bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()
                    }

                    outputFrame.buffer.put(
                        inputFrame.buffer.array(),
                        inputFrame.buffer.position(),
                        outputFrame.bufferInfo.size)

                    // advance input buffer position by number of bytes copied
                    inputFrame.buffer.position(inputFrame.buffer.position() + outputFrame.bufferInfo.size)

                    encoder.queueInputFrame(outputFrame)
                }
            }
        }
    }

    private external fun initAudioResampler(channelCount: Int, sourceSampleRate: Int, targetSampleRate: Int)

    private external fun resample(sourceBuffer: ShortArray, sourceBufferSize: Int, targetBuffer: ShortArray, targetBufferSize: Int): Int

    private external fun releaseAudioResampler()

    companion object {
        init {
            System.loadLibrary("litr-jni")
        }
    }
}