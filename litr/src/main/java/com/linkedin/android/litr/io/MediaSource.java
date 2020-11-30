/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io;

import android.media.MediaFormat;
import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Common interface for media source
 */
public interface MediaSource {

    /**
     * Get video orientation hint
     * @return video orientation in degrees, usually in increments of 90 (0, 90, 180, 270)
     */
    int getOrientationHint();

    /**
     * Get number of tracks
     * @return number of tracks
     */
    int getTrackCount();

    /**
     * Get track format
     * @param track track index
     * @return {@link MediaFormat} of specified track
     */
    @NonNull
    MediaFormat getTrackFormat(int track);

    /**
     * Select a track, so that subsequent calls to read data/metadata can use it.
     * @param track track index
     */
    void selectTrack(int track);

    /**
     * Seek to a position
     * @param position position in microseconds
     * @param mode seek mode, SEEK_TO_PREVIOUS_SYNC, SEEK_TO_NEXT_SYNC, or SEEK_TO_CLOSEST_SYNC from {@link android.media.MediaExtractor}
     */
    void seekTo(long position, int mode);

    /**
     * Returns the track index the current sample originates from (or -1 if no more samples are available)
     * @return track index of a current sample
     */
    int getSampleTrackIndex();

    /**
     * Retrieve the current encoded sample and store it in the byte buffer starting at the given offset.
     * @param buffer destination buffer
     * @param offset starting offset
     * @return the sample size (or -1 if no more samples are available)
     */
    int readSampleData(@NonNull ByteBuffer buffer, int offset);

    /**
     * Returns the current sample's presentation time in microseconds. or -1 if no more samples are available.
     * @return sample time in microseconds
     */
    long getSampleTime();

    /**
     * Returns the current sample's flags.
     * @return sample flags, value is either 0 or a combination of SAMPLE_FLAG_SYNC, SAMPLE_FLAG_ENCRYPTED, and SAMPLE_FLAG_PARTIAL_FRAME
     */
    int getSampleFlags();

    /**
     * Advance to next sample
     */
    void advance();

    /**
     * Free up all resources. Make sure to call this when MediaSource is no longer needed.
     */
    void release();

    /**
     * Get total size of media source
     * @return size in bytes, -1 if unknown
     */
    long getSize();

    /**
     * Get media selection. Default selection is entire media.
     */
    @NonNull
    default MediaRange getSelection() {
        return new MediaRange(0, Long.MAX_VALUE);
    }
}
