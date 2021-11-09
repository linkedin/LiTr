package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Provides the entry point for, and management of, thumbnail extraction work.
 */
class VideoThumbnailExtractor @JvmOverloads constructor(
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor(),
    private val listener: ThumbnailExtractListener? = null,
    private val listenerLooper: Looper = Looper.getMainLooper()
) {
    private val futureMap = mutableMapOf<String, Future<*>>()
    private val listenerHandler by lazy {
        Handler(listenerLooper)
    }

    /**
     * Starts a new extract job with the specified parameters.
     */
    fun extract(requestId: String, params: ThumbnailExtractParameters) {
        if (futureMap.containsKey(requestId)) {
            throw IllegalArgumentException("Request with ID $requestId already exists")
        }

        val job = ThumbnailExtractJob(requestId, params, rootListener)
        val future = executorService.submit(job)

        futureMap[requestId] = future
    }

    /**
     * Terminates the specified extract job. If the job was in progress, [ThumbnailExtractListener.onCancelled] will be called for the job.
     * Does not terminate immediately: [ThumbnailExtractListener.onExtracted] may still be called after this method is called.
     */
    fun stop(requestId: String) {
        futureMap[requestId]?.let {
            if (!it.isCancelled && !it.isDone)
                it.cancel(true)
        }
    }

    /**
     * Terminates all extract jobs immediately.
     */
    fun release() {
        executorService.shutdownNow()
    }

    private val rootListener = object : ThumbnailExtractListener {

        private fun runWithListener(func: (ThumbnailExtractListener) -> Unit) {
            listener?.let { listener ->
                // Post the listener invocation to the handler, if it's set -- otherwise just run on current thread
                listenerHandler.post { func(listener) } ?: run { func(listener) }
            }
        }

        override fun onStarted(id: String, timestampsUs: List<Long>) {
            runWithListener { it.onStarted(id, timestampsUs) }
        }

        override fun onExtracted(id: String, index: Int, bitmap: Bitmap) {
            runWithListener { it.onExtracted(id, index, bitmap) }
        }

        override fun onExtractFrameFailed(id: String, index: Int) {
            runWithListener { it.onExtractFrameFailed(id, index) }
        }

        override fun onCompleted(id: String) {
            runWithListener { it.onCompleted(id) }
        }

        override fun onCancelled(id: String) {
            runWithListener { it.onCancelled(id) }
        }

        override fun onError(id: String, cause: Throwable?) {
            runWithListener { it.onError(id, cause) }
        }

    }
}
