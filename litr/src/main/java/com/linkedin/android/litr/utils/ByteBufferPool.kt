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

internal class ByteBufferPool(private val isDirect: Boolean = false) {

    private val bufferStack = LinkedBlockingQueue<ByteBuffer>()

    fun get(capacity: Int): ByteBuffer {
        return bufferStack.poll()?.let { byteBuffer ->
            if (byteBuffer.capacity() >= capacity) {
                byteBuffer
            } else {
                allocateByteBuffer(capacity)
            }
        } ?: allocateByteBuffer(capacity)
    }

    fun put(byteBuffer: ByteBuffer) {
        byteBuffer.clear()
        bufferStack.put(byteBuffer)
    }

    fun release() {
        bufferStack.clear()
    }

    private fun allocateByteBuffer(capacity: Int): ByteBuffer {
        return if (isDirect) {
            ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN)
        } else {
            ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
        }
    }
}
