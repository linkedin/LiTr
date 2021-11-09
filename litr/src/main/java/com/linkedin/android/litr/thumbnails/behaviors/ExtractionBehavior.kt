package com.linkedin.android.litr.thumbnails.behaviors

import android.graphics.Bitmap
import com.linkedin.android.litr.thumbnails.ThumbnailExtractParameters

typealias FrameExtractedListener = (index: Int, bitmap: Bitmap) -> Unit

interface ExtractionBehavior {
    fun extract(params: ThumbnailExtractParameters, listener: FrameExtractedListener): Boolean
    fun release()
}