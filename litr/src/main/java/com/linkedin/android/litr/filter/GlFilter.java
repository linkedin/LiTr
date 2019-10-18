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
 * Interface to allow clients modify individual frames as they are being transcoded. This type of filters are used
 * in Surface based encoding, so modifications must be written in OpenGL.
 */
public interface GlFilter {

    /**
     * Initialize filter. This will be called during renderer initialization.
     * Before calling this, renderer will configure its MVP matrix and pass copies of it to filters, so they can use it to
     * configure their own MVP matrices. Model matrix in renderer's MVP matrix is usually an identity matrix. View matrix
     * is configured to have its camera angle match video's. Using renderer's MVP matrix ensures that filters don't have to
     * worry about handling video rotation and can just do model transformations.
     *
     * @param mvpMatrix MVP matrix configured by the renderer, usually
     * @param mvpMatrixOffset offset into MVP matrix
     */
    void init(@NonNull float[] mvpMatrix, int mvpMatrixOffset);

    /**
     * Apply GL rendering to a frame
     * @param presentationTimeNs presentation time of a frame, in nanoseconds
     */
    void apply(long presentationTimeNs);
}
