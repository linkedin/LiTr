package com.linkedin.android.litr.thumbnails

import android.graphics.Point
import com.linkedin.android.litr.codec.Decoder
import com.linkedin.android.litr.io.MediaRange
import com.linkedin.android.litr.io.MediaSource
import com.linkedin.android.litr.io.MediaSourceFactory
import com.linkedin.android.litr.render.GlThumbnailRenderer
import com.linkedin.android.litr.render.Renderer

data class ThumbnailExtractParameters(
    val mediaSourceFactory: MediaSourceFactory,
    val timestampsUs: List<Long>,
    val mediaRange: MediaRange,
    val syncFrameDecoder: Decoder,
    val exactFrameDecoder: Decoder,
    val sourceSize: Point,
    val destSize: Point,
    val renderer: GlThumbnailRenderer
)
