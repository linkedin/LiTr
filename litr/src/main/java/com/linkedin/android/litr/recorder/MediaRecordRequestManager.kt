package com.linkedin.android.litr.recorder

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Creates media record requests, maintaining their state. This is typically the entry point for media record operations in LiTr.
 */
class MediaRecordRequestManager @JvmOverloads constructor(
    private val context: Context,
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor(),
    private val listener: MediaRecordListener? = null,
    private val listenerLooper: Looper? = null
) {
    private val futureMap = mutableMapOf<String, Future<*>>()
    private val listenerHandler by lazy {
        listenerLooper?.let { Handler(it) }
    }

    fun record(requestId: String, params: List<MediaRecordParameters>) {
        if (futureMap.containsKey(requestId)) {
            throw IllegalArgumentException("Request with ID $requestId already exists")
        }

        val job = MediaRecordJob(requestId, params, rootListener)
        val future = executorService.submit(job)

        futureMap[requestId] = future
    }

    fun stop(requestId: String) {
        futureMap[requestId]?.let {
            if (!it.isCancelled && !it.isDone)
                it.cancel(true)
        }
    }

    fun release() {
        executorService.shutdownNow()
    }

    private val rootListener = object : MediaRecordListener {

        private fun runWithListener(func: (MediaRecordListener) -> Unit) {
            listener?.let { listener ->
                // Post the listener invocation to the handler, if it's set -- otherwise just run on current thread
                listenerHandler?.post { func(listener) } ?: run { func(listener) }
            }
        }

        override fun onStarted(id: String) {
            runWithListener { it.onStarted(id) }
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

    companion object {
        private val TAG: String = MediaRecordRequestManager::class.java.simpleName
    }
}
