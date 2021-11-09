package com.linkedin.android.litr.thumbnails

import android.graphics.Bitmap

typealias FrameExtractedListener = (index: Int, bitmap: Bitmap) -> Unit

interface ExtractionBehavior {
    fun extract(params: ThumbnailExtractParameters, listener: FrameExtractedListener): Boolean
    fun release()
}