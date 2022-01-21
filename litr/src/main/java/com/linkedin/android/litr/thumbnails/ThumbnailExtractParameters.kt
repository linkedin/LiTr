/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails

import android.graphics.Point
import android.net.Uri
import com.linkedin.android.litr.render.ThumbnailRenderer

/**
 * Request parameters for thumbnail extraction.
 */
data class ThumbnailExtractParameters @JvmOverloads constructor(
    val mediaUri: Uri,
    /**
     * The timestamp, in microseconds, for which to extract the frame. If the frame can't be extracted for a specified timestamp, the nearest frame may be
     * returned: this frame selection behavior is defined in [mode].
     */
    val timestampUs: Long,
    /**
     * The renderer to use to process each of the thumbnails before they're returned.
     */
    val renderer: ThumbnailRenderer,
    /**
     * A hint to the [ExtractionBehavior] about what approach to use for extracting video frames.
     */
    val mode: ExtractionMode = ExtractionMode.Fast,
    /**
     * Optional size of the generated thumbnail Bitmap.
     */
    val destSize: Point? = null,
    /**
     * The optional priority of the request. Lower value indicates higher priority. Requests with equal priority will be handled in FIFO order.
     */
    val priority: Long = 0L
)

