/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io;

import android.media.MediaCodec;
import android.media.MediaFormat;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Common interface for MediaTarget
 */
public interface MediaTarget {

    /**
     * Adds a track with the specified format.
     * @param mediaFormat The media format for the track. This must not be an empty MediaFormat.
     * @param targetTrack target track index
     * @return index of a newly added track
     */
    int addTrack(@NonNull MediaFormat mediaFormat, @IntRange(from = 0) int targetTrack);

    /**
     * Writes an encoded sample into the muxer.
     * @param targetTrack target track index
     * @param buffer encoded data
     * @param info metadata
     */
    void writeSampleData(int targetTrack, @NonNull ByteBuffer buffer, @NonNull MediaCodec.BufferInfo info);

    /**
     * Release all resources. Make sure to call this when MediaTarget is no longer needed
     */
    void release();

    /**
     * Get output file path
     * @return output file path
     */
    @NonNull
    String getOutputFilePath();
}
