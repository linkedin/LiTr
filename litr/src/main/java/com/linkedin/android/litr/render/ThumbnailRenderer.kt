package com.linkedin.android.litr.render

import android.graphics.Bitmap

interface ThumbnailRenderer {
    fun init(width: Int, height: Int)
    fun renderFrame(input: Bitmap?, presentationTimeNs: Long): Bitmap?
}