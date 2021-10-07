package com.linkedin.android.litr.recorder.readers

import android.graphics.SurfaceTexture
import android.view.Surface

interface SurfaceTrackReader : MediaTrackReader {
    fun drawFrame(surface: Surface, presentationTimeNs: Long)
}