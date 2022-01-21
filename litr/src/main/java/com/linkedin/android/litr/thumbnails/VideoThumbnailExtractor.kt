/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.linkedin.android.litr.ExperimentalThumbnailsApi
import com.linkedin.android.litr.filter.GlFilter
import com.linkedin.android.litr.render.GlThumbnailRenderer
import com.linkedin.android.litr.render.ThumbnailRenderer
import com.linkedin.android.litr.thumbnails.behaviors.ExtractionBehavior
import com.linkedin.android.litr.thumbnails.behaviors.MediaMetadataExtractionBehavior
import com.linkedin.android.litr.thumbnails.queue.ComparableFutureTask
import com.linkedin.android.litr.thumbnails.queue.PriorityExecutorUtil
import java.util.concurrent.*

/**
 * Provides the entry point for thumbnail extraction.
 *
 * This class uses a single, dedicated thread to schedule jobs. The priority of each job can be specified within [ThumbnailExtractParameters].
 *
 * @param context The application context.
 * @param listenerLooper The looper on which [extract] listener events will be processed.
 * @param extractionBehavior The behavior to use for extracting frames from media.
 */
@ExperimentalThumbnailsApi
class VideoThumbnailExtractor @JvmOverloads constructor(
    context: Context,
    private val listenerLooper: Looper = Looper.getMainLooper(),
    private var extractionBehavior: ExtractionBehavior = MediaMetadataExtractionBehavior(context)
) {

    private val activeJobMap = mutableMapOf<String, ActiveExtractJob>()

    private val listenerHandler by lazy {
        Handler(listenerLooper)
    }

    private val executorService = PriorityExecutorUtil.newSingleThreadPoolPriorityExecutor()

    /**
     * Starts a new thumbnail extract job with the specified parameters. The [listener] will be updated with the job status.
     *
     * @param requestId The ID of this request. Only one request per ID can be active. This ID is used to then refer to the request when calling [stop].
     * @param params Specifies extraction options and other parameters.
     * @param listener The listener to notify about the job status, such as success/error.
     */
    fun extract(requestId: String, params: ThumbnailExtractParameters, listener: ThumbnailExtractListener?) {
        if (activeJobMap.containsKey(requestId)) {
            Log.w(TAG, "Request with ID $requestId already exists")
            return
        }
        val task = ThumbnailExtractJob(requestId, params, extractionBehavior, rootListener)
        val futureTask = ComparableFutureTask(task, null, params.priority)
        executorService.execute(futureTask)
        activeJobMap[requestId] = ActiveExtractJob(futureTask, listener)
    }

    /**
     * Cancels the specified extract job. If the job was in progress, [ThumbnailExtractListener.onCancelled] will be called for the job.
     * Does not terminate immediately: [ThumbnailExtractListener.onExtracted] may still be called after this method is called.
     */
    fun stop(requestId: String) {
        activeJobMap[requestId]?.let {
            if (!it.future.isCancelled && !it.future.isDone) {
                it.future.cancel(true)
            }
        }
    }

    /**
     * Cancels all started extract jobs. [ThumbnailExtractListener.onCancelled] will be called for jobs that have been started.
     */
    fun stopAll() {
        activeJobMap.values.forEach {
            if (!it.future.isCancelled && !it.future.isDone) {
                it.future.cancel(true)
            }
        }
    }

    /**
     * Stops all extract jobs immediately and frees resources.
     */
    fun release() {
        executorService.shutdownNow()
        extractionBehavior.release()
        activeJobMap.clear()
    }

    private fun onCompleteJob(jobId: String) {
        activeJobMap.remove(jobId)
    }

    private val rootListener = object : ThumbnailExtractListener {

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

        private fun runOnListenerHandler(listener: ThumbnailExtractListener?, func: (ThumbnailExtractListener) -> Unit) {
            if (listener != null) {
                listenerHandler.post {
                    func(listener)
                }
            }
        }
    }

    private data class ActiveExtractJob(val future: Future<*>, val listener: ThumbnailExtractListener?)

    companion object {
        private val TAG = VideoThumbnailExtractor::class.qualifiedName
    }
}
