package com.linkedin.android.litr.io

import android.media.MediaMetadataRetriever
import com.linkedin.android.litr.exception.MediaSourceException

interface MediaSourceFactory {
    @Throws(MediaSourceException::class)
    fun createMediaSource(): MediaSource
    fun getRetriever(): MediaMetadataRetriever
}