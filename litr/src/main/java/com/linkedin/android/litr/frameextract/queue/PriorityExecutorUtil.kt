/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract.queue

import java.util.concurrent.*

/**
 * Helpers for constructing an [ExecutorService] backed by a [PriorityBlockingQueue] for prioritizing tasks.
 */
internal object PriorityExecutorUtil {
    fun newSingleThreadPoolPriorityExecutor() = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        PriorityBlockingQueue()
    )
}
