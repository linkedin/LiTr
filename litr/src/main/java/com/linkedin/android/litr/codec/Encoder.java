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
 * Common interface for encoder
 */
public interface Encoder {

    /**
     * Initialize the encoder
     * @param targetFormat {@link MediaFormat} to encode into
     * @throws TrackTranscoderException if cannot be initialized
     */
    void init(@NonNull MediaFormat targetFormat) throws TrackTranscoderException;

    /**
     * Requests a Surface to use as the input to an encoder, in place of input buffers. This may only be called after init() and before start()
     * @return a surface, which will be input to an encoder
     */
    @Nullable
    Surface createInputSurface();

    /**
     * Start the encoder
     * @throws TrackTranscoderException if encoder could not be started
     */
    void start() throws TrackTranscoderException;

    /**
     * Check if encoder is running
     * @return true if encoder is running, false otherwise
     */
    boolean isRunning();

    /**
     * If encoder uses {@link Surface} as an input, this method should be called to signal that the last frame was sent
     */
    void signalEndOfInputStream();

    /**
     * Dequeue input frame from the encoder. If frame is available, non-negative tag is returned.
     * @param timeout The timeout in microseconds, a negative timeout indicates "infinite".
     * @return tag of an input frame
     */
    int dequeueInputFrame(long timeout);

    /**
     * Returns an input frame, to be filled with raw data and metadata.
     * @param tag tag of an earlier dequeued input frame
     * @return a frame to be populated with data
     */
    @Nullable
    Frame getInputFrame(int tag);

    /**
     * Queues a populated input frame back to encoder
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
     * Returns a frame, populated with encoded data and metadata
     * @param tag non-negative frame tag, returned by an earlier dequeueOutputFrame call
     * @return frame with encoded data
     */
    @Nullable
    Frame getOutputFrame(@IntRange(from = 0) int tag);

    /**
     * Release output frame back to encoder
     * @param tag frame tag
     */
    void releaseOutputFrame(int tag);

    /**
     * Returns output format, if any. Usually should be called after dequeueOutputFrame returns INFO_OUTPUT_FORMAT_CHANGED
     * @return new MediaFormat
     */
    @NonNull
    MediaFormat getOutputFormat();

    /**
     * Stop the encoder
     */
    void stop();

    /**
     * Release the encoder and all its resources. Encoder becomes unusable afterwards.
     */
    void release();

    /**
     * Get encoder name. Useful when collecting stats.
     * @return encoder name
     * @throws TrackTranscoderException when getting the name fails
     */
    @NonNull
    String getName() throws TrackTranscoderException;
}
