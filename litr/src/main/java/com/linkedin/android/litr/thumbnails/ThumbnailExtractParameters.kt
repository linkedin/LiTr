package com.linkedin.android.litr.thumbnails

import android.graphics.Point
import com.linkedin.android.litr.codec.Decoder
import com.linkedin.android.litr.io.MediaSource
import com.linkedin.android.litr.render.GlThumbnailRenderer
import com.linkedin.android.litr.render.Renderer

data class ThumbnailExtractParameters(
    val frameProvider: ExtractFrameProvider,
    val decoder: Decoder,
    val sourceSize: Point,
    val mediaSource: MediaSource,
    val renderer: GlThumbnailRenderer
)
