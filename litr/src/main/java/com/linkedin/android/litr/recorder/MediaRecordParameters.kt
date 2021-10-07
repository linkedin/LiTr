package com.linkedin.android.litr.recorder

import android.media.MediaFormat
import com.linkedin.android.litr.codec.Encoder
import com.linkedin.android.litr.io.MediaTarget
import com.linkedin.android.litr.recorder.readers.MediaTrackReader
import com.linkedin.android.litr.render.Renderer

data class MediaRecordParameters(
    val reader: MediaTrackReader,
    val sourceFormat: MediaFormat,
    val targetTrack: Int,
    val targetFormat: MediaFormat,
    val mediaTarget: MediaTarget,
    val encoder: Encoder,
    val renderer: Renderer
)