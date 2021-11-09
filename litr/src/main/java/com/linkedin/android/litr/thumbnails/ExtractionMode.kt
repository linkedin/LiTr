package com.linkedin.android.litr.thumbnails

enum class ExtractionMode {
    /**
     * Extract just the sync frames (fastest)
     */
    SyncFrameOnly,

    /**
     * Extract just the closest frames (attempts to find exact video frame, closest to specified timestamp)
     */
    ExactFrameOnly,

    /**
     * Extract sync frames, then extract exact frames. Listener will be notified twice -- once for sync frame, then for exact frame, if available.
     */
    TwoPass
}