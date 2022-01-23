/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.frameextract

import android.graphics.Point
import android.net.Uri
import com.linkedin.android.litr.ExperimentalFrameExtractorApi
import com.linkedin.android.litr.render.SingleFrameRenderer

/**
 * Request parameters for frame extraction.
 */
@ExperimentalFrameExtractorApi
data class FrameExtractParameters @JvmOverloads constructor(
        /**
         * The URI of the media from which to extract the frame.
         */
        val mediaUri: Uri,
        /**
         * The timestamp, in microseconds, for which to extract the frame. If the frame can't be extracted for a specified timestamp, the nearest frame may be
         * returned: this frame selection behavior is defined in [mode].
         */
        val timestampUs: Long,
        /**
         * The renderer to use to process each of the frames before they're returned.
         *
         * One instance of the renderer may be shared between requests, as long as the dimensions of the video frames (for media specified in [mediaUri]) and
         * the [destSize] remain the same.
         */
        val renderer: SingleFrameRenderer,
        /**
         * A hint to the [ExtractionBehavior] about what approach to use for extracting video frames.
         */
        val mode: FrameExtractMode = FrameExtractMode.Fast,
        /**
         * Optional size of the generated frame Bitmap. The video frame will be resized and scaled to fill this [destSize], preserving the video frame's
         * original aspect ratio.
         */
        val destSize: Point? = null,
        /**
         * The optional priority of the request. Lower value indicates higher priority. Requests with equal priority will be handled in FIFO order.
         */
        val priority: Long = 0L
)

