/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.codec;

import android.media.MediaFormat;
import android.view.Surface;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.exception.TrackTranscoderException;

/**
 * Common interface for decoder
 */
public interface Decoder {

    /**
     * Initialize the decoder
     * @param mediaFormat {@link MediaFormat} of a track
     * @param surface optional {@link Surface} to decode onto
     * @throws TrackTranscoderException if decoder cannot be initialized
     */
    void init(@NonNull MediaFormat mediaFormat, @Nullable Surface surface) throws TrackTranscoderException;

    /**
     * Start the decoder, making it ready to accept encoded frames
     * @throws TrackTranscoderException if decoder cannot be started
     */
    void start() throws TrackTranscoderException;

    /**
     * Check if decoder is running
     * @return true if decoder is running, false otherwise
     */
    boolean isRunning();

    /**
     * Dequeue input frame from the decoder. If frame is available, non-negative tag is returned.
     * @param timeout The timeout in microseconds, a negative timeout indicates "infinite".
     * @return tag of an input frame
     */
    int dequeueInputFrame(long timeout);

    /**
     * Returns an input frame, to be filled with encoded data and metadata.
     * @param tag tag of an earlier dequeued input frame
     * @return a frame to be populated with data
     */
    @Nullable
    Frame getInputFrame(@IntRange(from = 0) int tag);

    /**
     * Queues a populated input frame back to decoder
     * @param frame frame populated with data and metadata
     */
    void queueInputFrame(@NonNull Frame frame);

    /**
     * Dequeue an output frame. If frame is available, non-negative tag is returned. Otherwise, tag is negative.
     * If new {@link MediaFormat} ia available, tag will be equal to INFO_OUTPUT_FORMAT_CHANGED
     * @param timeout The timeout in microseconds, a negative timeout indicates "infinite".
     * @return frame tag if frame is available, or return code otherwise
     */
    int dequeueOutputFrame(long timeout);

    /**
     * Returns a frame, populated with decoded data and metadata
     * @param tag non-negative frame tag, returned by an earlier dequeueOutputFrame call
     * @return frame with decoded data
     */
    @Nullable
    Frame getOutputFrame(@IntRange(from = 0) int tag);

    /**
     * Release output frame back to decoder
     * @param tag frame tag
     * @param render flag to indicate if frame should be rendered onto a Surface
     */
    void releaseOutputFrame(@IntRange(from = 0) int tag, boolean render);

    /**
     * Returns output format, if any. Usually should be called after dequeueOutputFrame returns INFO_OUTPUT_FORMAT_CHANGED
     * @return new MediaFormat
     */
    @NonNull
    MediaFormat getOutputFormat();

    /**
     * Stop the decoder
     */
    void stop();

    /**
     * Release the decoder, along with all its resources. Decoder cannot be used afterwards.
     */
    void release();

    /**
     * Get decoder name. Useful when collecting stats.
     * @return decoder name
     * @throws TrackTranscoderException when getting the name fails
     */
    @NonNull
    String getName() throws TrackTranscoderException;
}
