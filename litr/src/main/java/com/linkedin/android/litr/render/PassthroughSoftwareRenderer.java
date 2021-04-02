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
import com.linkedin.android.litr.resample.AudioResampler;
import com.linkedin.android.litr.resample.DownsampleAudioResampler;
import com.linkedin.android.litr.resample.PassThroughAudioResampler;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * This {@link Renderer} only used with Audio transcoding
 */
public class PassthroughSoftwareRenderer implements Renderer {

    @VisibleForTesting static final long FRAME_WAIT_TIMEOUT = TimeUnit.SECONDS.toMicros(0);

    private static final String TAG = "PassthroughSwRenderer";

    @NonNull public final Encoder encoder;
    private final long frameWaitTimeoutUs;

    @NonNull private AudioResampler audioResampler = new PassThroughAudioResampler();
    private MediaFormat sourceAudioFormat;
    private MediaFormat targetAudioFormat;

    public PassthroughSoftwareRenderer(@NonNull Encoder encoder) {
        this(encoder, FRAME_WAIT_TIMEOUT);
    }

    public PassthroughSoftwareRenderer(@NonNull Encoder encoder, long frameWaitTimeoutUs) {
        this.encoder = encoder;
        this.frameWaitTimeoutUs = frameWaitTimeoutUs;
    }

    @Override
    public void init(@Nullable Surface outputSurface, @Nullable MediaFormat sourceMediaFormat,
            @Nullable MediaFormat targetMediaFormat) {
        onMediaFormatChanged(sourceMediaFormat, targetMediaFormat);
    }

    @Override
    public void onMediaFormatChanged(@Nullable MediaFormat sourceMediaFormat, @Nullable MediaFormat targetMediaFormat) {
        this.sourceAudioFormat = sourceMediaFormat;
        this.targetAudioFormat = targetMediaFormat;
        initAudioResampler();
    }

    private void initAudioResampler() {
        if (sourceAudioFormat == null || targetAudioFormat == null) {
            return;
        }
        int inputSampleRate = audioResampler.getSampleRate(sourceAudioFormat);
        int outputSampleRate = audioResampler.getSampleRate(targetAudioFormat);
        if (inputSampleRate > outputSampleRate) {
            audioResampler = new DownsampleAudioResampler();
        } else {
            audioResampler = new PassThroughAudioResampler();
        }
    }

    @Nullable
    @Override
    public Surface getInputSurface() {
        return null;
    }

    @Override
    public void renderFrame(@Nullable Frame inputFrame, long presentationTimeNs) {
        if (inputFrame == null || inputFrame.buffer == null) {
            Log.e(TAG, "Null or empty input frame provided");
            return;
        }

        ByteBuffer inputBuffer = null;
        boolean isNewInputFrame = true;
        boolean areBytesRemaining;
        do {
            areBytesRemaining = false;
            int tag = encoder.dequeueInputFrame(frameWaitTimeoutUs);
            if (tag >= 0) {
                Frame outputFrame = encoder.getInputFrame(tag);
                if (outputFrame == null) {
                    Log.e(TAG, "No input frame returned by an encoder, dropping a frame");
                    return;
                }
                ByteBuffer outputBuffer = outputFrame.buffer;

                if (isNewInputFrame) {
                    isNewInputFrame = false;
                    inputBuffer = inputFrame.buffer.asReadOnlyBuffer();
                    inputBuffer.rewind();
                }

                int outSize = outputBuffer.remaining();
                int inSize = inputBuffer.remaining();

                // check if need to set a new limit for the inputBuffer to fit the outputBuffer, then restore the old limit after writing the data
                int inputBufferLimit = inputBuffer.limit();
                if (outSize < inSize) {
                    inputBuffer.limit(inputBuffer.position() + outSize);
                }

                // Resampling will change the input size based on the sample rate ratio.
                audioResampler.resample(inputBuffer, outputBuffer, sourceAudioFormat, targetAudioFormat);

                inputBuffer.limit(inputBufferLimit);
                areBytesRemaining = inputBuffer.hasRemaining();

                MediaCodec.BufferInfo bufferInfo = outputFrame.bufferInfo;
                bufferInfo.offset = 0;
                bufferInfo.size = outputBuffer.position();
                bufferInfo.presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(presentationTimeNs);
                bufferInfo.flags = inputFrame.bufferInfo.flags;
                encoder.queueInputFrame(outputFrame);
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
