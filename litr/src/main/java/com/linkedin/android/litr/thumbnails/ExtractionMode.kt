/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.thumbnails

enum class ExtractionMode {
    /**
     * Extract just the sync frames (fastest)
     */
    Fast,

    /**
     * Extract just the closest frames (attempts to find exact video frame, closest to specified timestamp)
     */
    Exact
}
