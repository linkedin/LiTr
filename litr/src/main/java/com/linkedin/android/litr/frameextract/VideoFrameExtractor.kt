/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.linkedin.android.litr.ExperimentalFrameExtractorApi
import com.linkedin.android.litr.frameextract.behaviors.FrameExtractBehavior
import com.linkedin.android.litr.frameextract.behaviors.MediaMetadataExtractBehavior
import com.linkedin.android.litr.frameextract.queue.ComparableFutureTask
import com.linkedin.android.litr.frameextract.queue.PriorityExecutorUtil

/**
 * Provides the entry point for single frame extraction.
 *
 * This class uses a single, dedicated thread to schedule jobs. The priority of each job can be specified within [FrameExtractParameters].
 *
 * @param context The application context.
 * @param listenerLooper The looper on which [extract] listener events will be processed.
 * @param extractBehavior The behavior to use for extracting frames from media.
 */
@ExperimentalFrameExtractorApi
class VideoFrameExtractor @JvmOverloads constructor(
    context: Context,
    private val listenerLooper: Looper = Looper.getMainLooper(),
    private var extractBehavior: FrameExtractBehavior = MediaMetadataExtractBehavior(context)
) {

    private val activeJobMap = mutableMapOf<String, ActiveExtractJob>()

    private val listenerHandler by lazy {
        Handler(listenerLooper)
    }

    private val executorService = PriorityExecutorUtil.newSingleThreadPoolPriorityExecutor()

    /**
     * Starts a new frame extract job with the specified parameters. The [listener] will be updated with the job status.
     *
     * @param requestId The ID of this request. Only one request per ID can be active. This ID is used to then refer to the request when calling [stop].
     * @param params Specifies extraction options and other parameters.
     * @param listener The listener to notify about the job status, such as success/error.
     */
    fun extract(requestId: String, params: FrameExtractParameters, listener: FrameExtractListener?) {
        if (activeJobMap.containsKey(requestId)) {
            Log.w(TAG, "Request with ID $requestId already exists")
            return
        }
        val task = FrameExtractJob(requestId, params, extractBehavior, rootListener)
        val futureTask = ComparableFutureTask(task, null, params.priority)
        executorService.execute(futureTask)
        activeJobMap[requestId] = ActiveExtractJob(futureTask, listener)
    }

    /**
     * Cancels the specified extract job. If the job was in progress, [FrameExtractListener.onCancelled] will be called for the job.
     * Does not terminate immediately: [FrameExtractListener.onExtracted] may still be called after this method is called.
     */
    fun stop(requestId: String) {
        activeJobMap[requestId]?.let {
            if (!it.future.isCancelled && !it.future.isDone) {
                it.future.cancel(true)
            }
            if (!it.future.isStarted) {
                // If the job hasn't started, it won't probably even start, but it will remain in the activeJobMap,
                // we must remove it from there.
                activeJobMap.remove(requestId)
            }
        }
    }

    /**
     * Cancels all started extract jobs. [FrameExtractListener.onCancelled] will be called for jobs that have been started.
     */
    fun stopAll() {
        val iterator = activeJobMap.iterator()
        while (iterator.hasNext()) {
            val job = iterator.next().value
            if (!job.future.isCancelled && !job.future.isDone) {
                job.future.cancel(true)
            }
            if (!job.future.isStarted) {
                // If the job hasn't started, it won't probably even start, but it will remain in the activeJobMap,
                // we must remove it from there.
                iterator.remove()
            }
        }
    }

    /**
     * Stops all extract jobs immediately and frees resources.
     */
    fun release() {
        executorService.shutdownNow()
        extractBehavior.release()
        activeJobMap.clear()
    }

    private fun onCompleteJob(jobId: String) {
        activeJobMap.remove(jobId)
    }

    private val rootListener = object : FrameExtractListener {

        override fun onStarted(id: String, timestampUs: Long) {
            runOnListenerHandler(activeJobMap[id]?.listener) { it.onStarted(id, timestampUs) }
        }

        override fun onExtracted(id: String, timestampUs: Long, bitmap: Bitmap) {
            runOnListenerHandler(activeJobMap[id]?.listener) {
                onCompleteJob(id)
                it.onExtracted(id, timestampUs, bitmap)
            }
        }

        override fun onCancelled(id: String, timestampUs: Long) {
            runOnListenerHandler(activeJobMap[id]?.listener) {
                onCompleteJob(id)
                it.onCancelled(id, timestampUs)
            }
        }

        override fun onError(id: String, timestampUs: Long, cause: Throwable?) {
            runOnListenerHandler(activeJobMap[id]?.listener) {
                onCompleteJob(id)
                it.onError(id, timestampUs, cause)
            }
        }

        private fun runOnListenerHandler(listener: FrameExtractListener?, func: (FrameExtractListener) -> Unit) {
            if (listener != null) {
                listenerHandler.post {
                    func(listener)
                }
            }
        }
    }

    private data class ActiveExtractJob(val future: ComparableFutureTask<*>, val listener: FrameExtractListener?)

    companion object {
        private const val TAG = "VideoThumbnailExtractor"
    }
}
