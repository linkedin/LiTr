/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.filter;

import androidx.annotation.NonNull;

/**
 * Interface for filters that implement rendering source video frame onto target video frame.
 * In addition to target frame geometry (size, rotation, aspect ratio, etc.) frame renderer
 * should also have access to source video frame texture.
 */
public interface GlFrameRenderFilter extends GlFilter {

    /**
     * Initialize texture associated with {@link android.graphics.SurfaceTexture} if input video frames
     * @param textureHandle texture handle of input video texture
     * @param transformMatrix transform matrix of input video texture
     */
    void initInputFrameTexture(int textureHandle, @NonNull float[] transformMatrix);
}
