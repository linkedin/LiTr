package com.linkedin.android.litr.thumbnails

import android.graphics.Point
import com.linkedin.android.litr.render.ThumbnailRenderer

/**
 * Request parameters for thumbnail extraction.
 */
data class ThumbnailExtractParameters(
    /**
     * The behavior to use for extraction.
     */
    val behavior: ExtractionBehavior,


    val timestampsUs: List<Long>,
    val destSize: Point,
    val renderer: ThumbnailRenderer
)

