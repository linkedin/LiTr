package com.linkedin.android.litr.demo.camera

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraHandler(var multiTargetCameraThread: MultiTargetCameraThread?, looper: Looper) : Handler(looper) {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun startPreview(width: Int, height: Int) {
        sendMessage(obtainMessage(CameraMessage.StartPreview.what, width, height))
    }

    fun stopPreview() {
        lock.withLock {
            sendEmptyMessage(CameraMessage.StopPreview.what)
        }
    }

    override fun handleMessage(msg: Message) {

        when (CameraMessage.fromInt(msg.what)) {
            CameraMessage.StartPreview -> {
                multiTargetCameraThread?.startPreview(msg.arg1, msg.arg2)
            }
            CameraMessage.StopPreview -> {
                multiTargetCameraThread?.stopPreview()
                lock.withLock {
                    condition.signalAll()
                }
                try {
                    Looper.myLooper()?.quit()
                    multiTargetCameraThread?.let { removeCallbacks(it) }

                    CameraMessage.values().forEach {
                        removeMessages(it.what)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Exception while stopping preview", ex)
                }

                multiTargetCameraThread = null
            }
        }
    }

    enum class CameraMessage(val what: Int) {
        StartPreview(1),
        StopPreview(2);

        companion object {
            fun fromInt(value: Int) = values().first { it.what == value }
        }
    }

    companion object {
        private val TAG = CameraHandler::class.qualifiedName
    }
}