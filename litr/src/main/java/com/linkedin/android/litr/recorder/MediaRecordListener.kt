package com.linkedin.android.litr.recorder

interface MediaRecordListener {
    fun onStarted(id: String)
    fun onCompleted(id: String)
    fun onCancelled(id: String)
    fun onError(id: String, cause: Throwable?)
}