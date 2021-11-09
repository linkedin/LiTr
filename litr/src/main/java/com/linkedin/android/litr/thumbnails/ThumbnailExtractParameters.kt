package com.linkedin.android.litr.thumbnails

import android.graphics.Point
import com.linkedin.android.litr.render.GlThumbnailRenderer

data class ThumbnailExtractParameters(
    val behavior: ExtractionBehavior,
    val timestampsUs: List<Long>,
    val destSize: Point,
    val renderer: GlThumbnailRenderer
)

