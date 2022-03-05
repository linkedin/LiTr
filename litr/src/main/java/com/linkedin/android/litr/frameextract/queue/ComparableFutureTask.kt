/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract.queue

import com.linkedin.android.litr.ExperimentalFrameExtractorApi
import com.linkedin.android.litr.frameextract.FrameExtractJob
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

/**
 * A [FutureTask] that is has a priority, so that it can be e.g. used in a priority queue with other tasks.
 *
 * This task maintains a FIFO order for priority.
 */
@ExperimentalFrameExtractorApi
internal class ComparableFutureTask<T>(private val job: FrameExtractJob?, result: T, private var priority: Long) : FutureTask<T>(job, result),
    Comparable<ComparableFutureTask<T>> {
    private val sequenceNumber = sharedSequence.getAndIncrement()

    val isStarted: Boolean
        get() = job?.isStarted ?: false

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
