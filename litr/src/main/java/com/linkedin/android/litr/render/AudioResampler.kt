/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import com.linkedin.android.litr.codec.Frame

/**
 * Common interface for all audio resamplers
 */
interface AudioResampler {

    /**
     * Resample an audio frame
     * @param frame input frame that needs to be resampled
     * @return resampled frame
     */
    fun resample(frame: Frame): Frame

    /**
     * Release resample. After this method is called, resamples can no longer be used.
     * New instance of resampler must be initialized if necessary.
     */
    fun release()
}
