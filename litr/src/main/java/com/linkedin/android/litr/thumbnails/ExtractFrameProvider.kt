package com.linkedin.android.litr.thumbnails

import com.linkedin.android.litr.io.MediaRange

interface ExtractFrameProvider {
    fun shouldExtract(presentationTimeUs: Long): Boolean
    fun didExtract(presentationTimeUs: Long)
    val mediaRange: MediaRange
}