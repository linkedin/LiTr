/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

private const val DEFAULT_BUFFER_POOL_SIZE = 2

/**
 * Implementation of [Decoder] that produces frames identical to ones it consumes, preserving the order.
 * If running in hardware accelerated mode, with non-null [Surface], triggers a GL draw call.
 */
class PassthroughDecoder(
    inputBufferCapacity: Int
) : Decoder {

    // pool of unused frames
    private val availableFrames = mutableSetOf<Frame>()
    // pool of frames a client dequeued to populate with encoded input data
    private val dequeuedInputFrames = mutableMapOf<Int, Frame>()
    // queue of frames being decoded
    private val decodeQueue = mutableListOf<Int>()
    // pool of decoded frames
    private val decodedFrames = mutableMapOf<Int, Frame>()

    private lateinit var mediaFormat: MediaFormat
    private var surface: Surface? = null

    private var isRunning = false

    init {
        for (tag in 0 until DEFAULT_BUFFER_POOL_SIZE) {
            availableFrames.add(Frame(tag, ByteBuffer.allocate(inputBufferCapacity), MediaCodec.BufferInfo()))
        }
    }

    override fun init(mediaFormat: MediaFormat, surface: Surface?) {
        this.mediaFormat = mediaFormat
        this.surface = surface
    }

    override fun start() {
        isRunning = true
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun dequeueInputFrame(timeout: Long): Int {
        return availableFrames.firstOrNull()?.let { availableFrame ->
            availableFrames.remove(availableFrame)
            dequeuedInputFrames[availableFrame.tag] = availableFrame
            availableFrame.tag
        } ?: MediaCodec.INFO_TRY_AGAIN_LATER
    }

    override fun getInputFrame(tag: Int): Frame? {
        return dequeuedInputFrames[tag]
    }

    override fun queueInputFrame(frame: Frame) {
        dequeuedInputFrames.remove(frame.tag)
        decodeQueue.add(frame.tag)
        decodedFrames[frame.tag] = frame
    }

    override fun dequeueOutputFrame(timeout: Long): Int {
        return if (decodeQueue.isNotEmpty()) decodeQueue.removeAt(0) else MediaCodec.INFO_TRY_AGAIN_LATER
    }

    override fun getOutputFrame(tag: Int): Frame? {
        return decodedFrames[tag]
    }

    override fun releaseOutputFrame(tag: Int, render: Boolean) {
        surface?.let { surface ->
            if (render) {
                val canvas = surface.lockCanvas(null)
                surface.unlockCanvasAndPost(canvas)
            }
        }
        decodedFrames.remove(tag)?.let {
            availableFrames.add(it)
        }
    }

    override fun getOutputFormat(): MediaFormat {
        return mediaFormat
    }

    override fun stop() {
        isRunning = false
    }

    override fun release() {
    }

    override fun getName(): String {
        return "PassthroughDecoder"
    }
}