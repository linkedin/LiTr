package com.linkedin.android.litr.thumbnails

interface ThumbnailExtractListener {
    fun onStarted(id: String)
    fun onExtracted(id: String, frameTimeUs: Long)
    fun onCompleted(id: String)
    fun onCancelled(id: String)
    fun onError(id: String, cause: Throwable?)
}