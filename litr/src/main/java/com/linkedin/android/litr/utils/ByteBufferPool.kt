/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue

/**
 * A helper thread safe class that manages a [ByteBuffer] pool, to increase buffer reuse.
 * This class is very useful in classes like renderers because they work with sequences of same sized buffers.
 */
class ByteBufferPool(private val isDirect: Boolean = false) {

    private val bufferQueue = LinkedBlockingQueue<ByteBuffer>()

    /**
     * Get a buffer from the pool. If buffer of at least requested capacity is available in the pool,
     * it will be returned. Otherwise, new buffer of requested capacity will be created.
     * Returned buffer will be ready to receive data.
     */
    fun get(capacity: Int): ByteBuffer {
        return bufferQueue.poll()?.let { byteBuffer ->
            if (byteBuffer.capacity() >= capacity) {
                byteBuffer
            } else {
                allocateByteBuffer(capacity)
            }
        } ?: allocateByteBuffer(capacity)
    }

    /**
     * Put a buffer back in the pool. Buffer will be cleared: position will be set to 0, and limit to capacity.
     * Contents of a buffer must be consumed before calling this method.
     */
    fun put(byteBuffer: ByteBuffer) {
        byteBuffer.clear()
        bufferQueue.put(byteBuffer)
    }

    /**
     * Clear the pool, all entries in it will be removed.
     */
    fun clear() {
        bufferQueue.clear()
    }

    private fun allocateByteBuffer(capacity: Int): ByteBuffer {
        return if (isDirect) {
            ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN)
        } else {
            ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
        }
    }
}
