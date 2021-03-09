/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.render;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.Frame;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class PassthroughSoftwareRenderer implements Renderer {

    @VisibleForTesting static final long FRAME_WAIT_TIMEOUT = TimeUnit.SECONDS.toMicros(10);

    private static final String TAG = "PassthroughSwRenderer";

    @NonNull public final Encoder encoder;

    private byte[] inputByteBuffer = null;

    public PassthroughSoftwareRenderer(@NonNull Encoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void init(@Nullable Surface outputSurface, @Nullable MediaFormat sourceMediaFormat, @Nullable MediaFormat targetMediaFormat) {}

    @Nullable
    @Override
    public Surface getInputSurface() {
        return null;
    }

    @Override
    public void renderFrame(@Nullable Frame frame, long presentationTimeNs) {
        if (frame == null || frame.buffer == null) {
            Log.e(TAG, "Null or empty input frame provided");
            return;
        }

        ByteBuffer inputBuffer = null;
        int capacity;
        boolean isNewInputFrame = true;
        boolean areBytesRemaining;
        do {
            areBytesRemaining = false;
            int tag = encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT);
            if (tag >= 0) {
                Frame outputFrame = encoder.getInputFrame(tag);
                if (outputFrame == null) {
                    Log.e(TAG, "No input frame returned by an encoder, dropping a frame");
                    return;
                }

                if (isNewInputFrame) {
                    isNewInputFrame = false;
                    inputBuffer = frame.buffer.asReadOnlyBuffer();
                    inputBuffer.rewind();
                }

                if (outputFrame.buffer.remaining() < inputBuffer.remaining()) {
                    capacity = outputFrame.buffer.remaining();
                    inputByteBuffer = new byte[capacity];
                    inputBuffer.get(inputByteBuffer, 0, capacity);

                    outputFrame.buffer.put(inputByteBuffer);
                } else {
                    capacity = inputBuffer.remaining();
                    outputFrame.buffer.put(inputBuffer);
                }

                outputFrame.bufferInfo.set(
                        0,
                        capacity,
                        TimeUnit.NANOSECONDS.toMicros(presentationTimeNs),
                        frame.bufferInfo.flags);
                encoder.queueInputFrame(outputFrame);

                areBytesRemaining = inputBuffer.hasRemaining();
            } else {
                switch (tag) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.e(TAG, "Encoder input frame timeout, dropping a frame");
                        break;
                    default:
                        Log.e(TAG, "Unhandled value " + tag + " when receiving decoded input frame");
                        break;
                }
            }
        }
        while (areBytesRemaining);
    }

    @Override
    public void release() {}

    @Override
    public boolean hasFilters() {
        return false;
    }
}
