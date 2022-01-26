package com.linkedin.android.litr.render

import com.linkedin.android.litr.codec.Frame

/**
 * Common interface for all audio resamplers
 */
interface AudioResampler {

    /**
     * Resample an audio frame
     * @param input frame that needs to be resampled
     * @return resampled frame
     */
    fun resample(frame: Frame): Frame

    /**
     * Release resample. After this method is called, resamples can no longer be used.
     * New instance of resampler must be initialized if necessary.
     */
    fun release()
}
