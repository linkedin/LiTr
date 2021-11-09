package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap

interface ThumbnailExtractListener {
    fun onStarted(id: String, timestampsUs: List<Long>)
    fun onExtracted(id: String, index: Int, bitmap: Bitmap?)
    fun onCompleted(id: String)
    fun onCancelled(id: String)
    fun onError(id: String, cause: Throwable?)
}