/*
 * Copyright 2022 LinkedIn Corporation
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val DEFAULT_BUFFER_POOL_SIZE = 2

/**
 * Implementation of [Encoder] that passes input frames to output consumer without modifying them.
 * Works only in ByteBuffer mode.
 * @param inputBufferCapacity capacity of encoder's input buffers
 */
class PassthroughBufferEncoder(
    inputBufferCapacity: Int
) : Encoder {

    // pool of unused frames
    private val availableFrames = LinkedBlockingQueue<Frame>()
    // pool of frames a client dequeued to populate with raw input data
    private val dequeuedInputFrames = ConcurrentHashMap<Int, Frame>()
    // queue of frames being encoded
    private val encodeQueue = LinkedBlockingQueue<Int>()
    // pool of encoded frames
    private val encodedFrames = ConcurrentHashMap<Int, Frame>()

    private lateinit var mediaFormat: MediaFormat

    private var isRunning = false
    private var mediaFormatChanged = false

    init {
        for (tag in 0 until DEFAULT_BUFFER_POOL_SIZE) {
            availableFrames.add(Frame(tag, ByteBuffer.allocate(inputBufferCapacity), MediaCodec.BufferInfo()))
        }
    }

    override fun init(targetFormat: MediaFormat) {
        this.mediaFormat = targetFormat
    }

    override fun createInputSurface(): Surface? {
        return null
    }

    override fun start() {
        isRunning = true
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun signalEndOfInputStream() {}

    override fun dequeueInputFrame(timeout: Long): Int {
        // since this method modifies multiple data structures, we have to make it synchronous
        synchronized(this) {
            return availableFrames.firstOrNull()?.let { availableFrame ->
                availableFrames.remove(availableFrame)
                dequeuedInputFrames[availableFrame.tag] = availableFrame
                availableFrame.tag
            } ?: MediaCodec.INFO_TRY_AGAIN_LATER
        }
    }

    override fun getInputFrame(tag: Int): Frame? {
        return dequeuedInputFrames[tag]
    }

    override fun queueInputFrame(frame: Frame) {
        // since this method modifies multiple data structures, we have to make it synchronous
        synchronized(this) {
            frame.buffer?.flip()
            dequeuedInputFrames.remove(frame.tag)
            encodedFrames[frame.tag] = frame
            encodeQueue.add(frame.tag)
        }
    }

    override fun dequeueOutputFrame(timeout: Long): Int {
        if (!mediaFormatChanged) {
            mediaFormatChanged = true
            return MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
        }
        return encodeQueue.poll(timeout, TimeUnit.MICROSECONDS) ?: MediaCodec.INFO_TRY_AGAIN_LATER
    }

    override fun getOutputFrame(tag: Int): Frame? {
        return encodedFrames[tag]
    }

    override fun releaseOutputFrame(tag: Int) {
        synchronized(this) {
            encodedFrames.remove(tag)?.let {
                it.buffer?.clear()
                availableFrames.add(it)
            }
        }
    }

    override fun getOutputFormat(): MediaFormat {
        return mediaFormat
    }

    override fun stop() {
        isRunning = false
    }

    override fun release() {
        availableFrames.clear()
        dequeuedInputFrames.clear()
        encodeQueue.clear()
        encodedFrames.clear()
    }

    override fun getName(): String {
        return "PassthroughEncoder"
    }
}