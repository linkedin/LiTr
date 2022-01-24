/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import android.graphics.Bitmap
import com.linkedin.android.litr.ExperimentalFrameExtractorApi

@ExperimentalFrameExtractorApi
interface SingleFrameRenderer {
    fun renderFrame(input: Bitmap?, presentationTimeNs: Long): Bitmap?
}
