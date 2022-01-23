/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract.queue

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

/**
 * A [FutureTask] that is has a priority, so that it can be e.g. used in a priority queue with other tasks.
 *
 * This task maintains a FIFO order for priority.
 */
open class ComparableFutureTask<T>(runnable: Runnable?, result: T, private var priority: Long) : FutureTask<T>(runnable, result),
    Comparable<ComparableFutureTask<T>> {
    private val sequenceNumber = sharedSequence.getAndIncrement()

    override fun compareTo(other: ComparableFutureTask<T>): Int {
        var res = priority.compareTo(other.priority)

        if (res == 0) {
            res = if (sequenceNumber < other.sequenceNumber) -1 else 1
        }

        return res
    }

    companion object {
        val sharedSequence = AtomicLong()
    }
}
