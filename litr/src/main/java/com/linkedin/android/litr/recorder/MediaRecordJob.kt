package com.linkedin.android.litr.recorder

import android.text.TextUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.linkedin.android.litr.exception.TrackTranscoderException
import com.linkedin.android.litr.transcoder.TrackTranscoder
import java.io.File

class MediaRecordJob @JvmOverloads constructor(
    val jobId: String,
    val params: List<MediaRecordParameters>,
    private val listener: MediaRecordListener?,
    private val recorderFactory: MediaRecorderFactory = MediaRecorderFactory()
) : Runnable {
    private val recorders = mutableListOf<MediaRecorder>()

    override fun run() {
        try {
            record()
        } catch (e: RuntimeException) {
            Log.e(TAG, "MediaRecordJob error", e)
            when (e.cause) {
                is InterruptedException -> stop()
                else -> error(e)
            }
        }
    }

    private fun record() {
        createTrackRecorders()
        startTrackRecorders()

        listener?.onStarted(jobId)

        var completed: Boolean

        do {
            completed = processNextFrame()

            if (Thread.interrupted()) {
                completed = true
            }
        } while (!completed)

        release(completed)

    }


    @VisibleForTesting
    @Throws(TrackTranscoderException::class)
    fun processNextFrame(): Boolean {
        var completed = true

        recorders.forEach { recorder ->
            val result = recorder.processNextFrame()
            completed = completed and (result == TrackTranscoder.RESULT_EOS_REACHED)
        }

        return completed
    }

    private fun startTrackRecorders() {
        recorders.forEach { it.start() }
    }

    private fun createTrackRecorders() {
        recorders.clear()
        params.forEach {
            recorders.add(recorderFactory.create(it))
        }
    }

    fun stop() {
        release(true)
    }

    fun error(cause: Throwable?) {
        release(false)
        listener?.onError(jobId, cause)
    }

    fun release(success: Boolean) {
        recorders.forEach { it.stop() }

        params.forEach { param ->
            param.reader.stop()
            param.mediaTarget.release()
            if (!success) {
                deleteOutputFile(param.mediaTarget.outputFilePath)
            }
        }

        if (success) {
            listener?.onCompleted(jobId)
        }
    }

    @VisibleForTesting
    fun deleteOutputFile(outputFilePath: String?): Boolean {
        if (TextUtils.isEmpty(outputFilePath)) {
            return false
        }
        return outputFilePath?.let { File(it) }?.delete() ?: false
    }

    companion object {
        private val TAG: String = MediaRecordJob::class.java.simpleName
    }
}