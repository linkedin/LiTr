/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.utils.CodecUtils;

import java.nio.ByteBuffer;

public class MediaCodecEncoder implements Encoder {

    private MediaCodec mediaCodec;

    private boolean isReleased = true;
    private boolean isRunning;

    private final MediaCodec.BufferInfo encoderOutputBufferInfo = new MediaCodec.BufferInfo();

    @Override
    public void init(@NonNull MediaFormat targetFormat) throws TrackTranscoderException {
        // unless specified otherwise, we use default color format for the surface
        if (!targetFormat.containsKey(MediaFormat.KEY_COLOR_FORMAT)) {
            targetFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        }

        mediaCodec = CodecUtils.getAndConfigureCodec(
                targetFormat,
                null,
                true,
                TrackTranscoderException.Error.ENCODER_NOT_FOUND,
                TrackTranscoderException.Error.ENCODER_FORMAT_NOT_FOUND,
                TrackTranscoderException.Error.ENCODER_CONFIGURATION_ERROR);
        isReleased = mediaCodec == null;
    }

    @Override
    @NonNull
    public Surface createInputSurface() {
        return mediaCodec.createInputSurface();
    }

    @Override
    public void start() throws TrackTranscoderException {
        try {
            startEncoder();
        } catch (Exception codecException) {
            throw new TrackTranscoderException(TrackTranscoderException.Error.INTERNAL_CODEC_ERROR, codecException);
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int dequeueInputFrame(long timeout) {
        return mediaCodec.dequeueInputBuffer(timeout);
    }

    @Override
    @Nullable
    public Frame getInputFrame(@IntRange(from = 0) int tag) {
        if (tag >= 0) {
            ByteBuffer inputBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = mediaCodec.getInputBuffer(tag);
            } else {
                ByteBuffer[] encoderInputBuffers = mediaCodec.getInputBuffers();
                inputBuffer = encoderInputBuffers[tag];
            }
            return new Frame(tag, inputBuffer, null);
        }
        return null;
    }

    @Override
    public void queueInputFrame(@NonNull Frame frame) {
        mediaCodec.queueInputBuffer(frame.tag,
                                    frame.bufferInfo.offset,
                                    frame.bufferInfo.size,
                                    frame.bufferInfo.presentationTimeUs,
                                    frame.bufferInfo.flags);
    }

    @Override
    public void signalEndOfInputStream() {
        mediaCodec.signalEndOfInputStream();
    }

    @Override
    public int dequeueOutputFrame(long timeout) {
        return mediaCodec.dequeueOutputBuffer(encoderOutputBufferInfo, timeout);
    }

    @Override
    @Nullable
    public Frame getOutputFrame(@IntRange(from = 0) int tag) {
        if (tag >= 0) {
            ByteBuffer buffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                buffer = mediaCodec.getOutputBuffer(tag);
            } else {
                ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
                buffer = encoderOutputBuffers[tag];
            }
            return new Frame(tag, buffer, encoderOutputBufferInfo);
        }
        return null;
    }

    @Override
    public void releaseOutputFrame(@IntRange(from = 0) int tag) {
        mediaCodec.releaseOutputBuffer(tag, false);
    }

    @Override
    @NonNull
    public MediaFormat getOutputFormat() {
        return mediaCodec.getOutputFormat();
    }

    @Override
    public void stop() {
        if (isRunning) {
            mediaCodec.stop();
            isRunning = false;
        }
    }

    @Override
    public void release() {
        if (!isReleased) {
            mediaCodec.release();
            isReleased = true;
        }
    }

    @Override
    @NonNull
    public String getName() throws TrackTranscoderException {
        try {
            return mediaCodec.getName();
        } catch (IllegalStateException e) {
            throw new TrackTranscoderException(TrackTranscoderException.Error.CODEC_IN_RELEASED_STATE, e);
        }
    }

    private void startEncoder() {
        if (!isRunning) {
            mediaCodec.start();
            isRunning = true;
        }
    }
}
