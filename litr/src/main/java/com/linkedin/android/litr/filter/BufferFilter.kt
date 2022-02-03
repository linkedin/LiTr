package com.linkedin.android.litr.filter

import com.linkedin.android.litr.codec.Frame

/**
 * A filter used with a renderer operating in buffer mode, like [AudioRenderer]
 */
interface BufferFilter {

    /**
     * Apply a filter to a [Frame]. Frame.bufferInfo will provide necessary metadata.
     * Frame.buffer is expected to be modified. Buffer contents are in target format.
     * For example, for audio buffer those will be Short (2 byte LE) values with target
     * sample rate and channel count.
     */
    fun apply(frame: Frame)
}
