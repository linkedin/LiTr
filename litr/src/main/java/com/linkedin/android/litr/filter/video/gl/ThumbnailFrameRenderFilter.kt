package com.linkedin.android.litr.filter.video.gl

import com.linkedin.android.litr.filter.Transform

class ThumbnailFrameRenderFilter
/**
 * Create frame render filter with source video frame, then scale, then position and then rotate the bitmap around its center as specified.
 * No pixel data is modified.
 * @param transform [Transform] that defines positioning of source video frame within target video frame
 */
/**
 * Create most basic filter, which scales source frame to fit target frame, with no pixel modification.
 */
@JvmOverloads constructor(transform: Transform? = null) : VideoFrameRenderFilter(
    DEFAULT_VERTEX_SHADER,
    DEFAULT_FRAGMENT_SHADER,
    null,
    transform
)
