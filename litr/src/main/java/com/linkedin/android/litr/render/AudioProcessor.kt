/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render

import com.linkedin.android.litr.codec.Frame

/**
 * Common interface for all audio processors
 */
interface AudioProcessor {

    /**
     * Process (resample, mix channels, etc.) an audio frame from source to target format.
     * @param sourceFrame input frame that needs to be processed
     * @param targetFrame output frame, which will have a pre-allocated buffer to put processed data into
     */
    fun processFrame(sourceFrame: Frame, targetFrame: Frame)

    /**
     * Release processor. After this method is called, processor can no longer be used.
     * New instance of [AudioProcessor] must be initialized if necessary.
     */
    fun release()
}
