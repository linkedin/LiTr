package com.linkedin.android.litr.thumbnails

import android.graphics.Point
import com.linkedin.android.litr.render.ThumbnailRenderer
import com.linkedin.android.litr.thumbnails.behaviors.ExtractionBehavior

/**
 * Request parameters for thumbnail extraction.
 */
data class ThumbnailExtractParameters(
    /**
     * The behavior to use for extraction.
     */
    val behavior: ExtractionBehavior,
    /**
     * Set of timestamps, in microseconds, for which to extract frames. If the frame can't be extracted for a specified timestamp, the nearest frame may be
     * returned: this frame selection behavior is contained in [behavior].
     */
    val timestampsUs: List<Long>,
    /**
     * The size of the generated thumbnail bitmaps.
     */
    val destSize: Point,
    /**
     * The renderer to use to process each of the thumbnails before they're returned.
     */
    val renderer: ThumbnailRenderer
)

