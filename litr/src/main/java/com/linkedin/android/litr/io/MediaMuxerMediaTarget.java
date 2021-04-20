/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.linkedin.android.litr.exception.MediaTargetException;
import com.linkedin.android.litr.utils.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import static com.linkedin.android.litr.exception.MediaTargetException.Error.INVALID_PARAMS;
import static com.linkedin.android.litr.exception.MediaTargetException.Error.IO_FAILUE;

/**
 * An implementation of MediaTarget, which wraps Android's {@link MediaMuxer}
 *
 * Before writing any media samples to MediaMuxer, all tracks must be added and MediaMuxer must be started. Some track
 * transcoders may start writing their output before other track transcoders added their track. This class queues writing
 * media samples until all tracks are created, allowing track transcoders to work independently.
 */

public class MediaMuxerMediaTarget implements MediaTarget {
    private static final String TAG = MediaMuxerMediaTarget.class.getSimpleName();

    @VisibleForTesting LinkedList<MediaSample> queue;
    @VisibleForTesting boolean isStarted;
    @VisibleForTesting MediaMuxer mediaMuxer;

    private MediaFormat[] mediaFormatsToAdd;

    private ParcelFileDescriptor parcelFileDescriptor;
    private String outputFilePath;
    private int numberOfTracksToAdd;
    private int trackCount;

    /**
     * This constructor to support scoped-storage enforcement in Android 10+, by using Uri instead of directly writing to File
     */
    public MediaMuxerMediaTarget(@NonNull Context context, @NonNull Uri outputFileUri,
            @IntRange(from = 1) int trackCount, int orientationHint, int outputFormat) throws MediaTargetException {
        String outputFilePath = FileUtils.getFilePathFromUri(context, outputFileUri);
        try {
            if (outputFilePath == null) {
                outputFilePath = "null";
                throw new IOException();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                parcelFileDescriptor = FileUtils.getParcelFileDescriptor(context, outputFileUri);
                if (parcelFileDescriptor != null) {
                    mediaMuxer = new MediaMuxer(parcelFileDescriptor.getFileDescriptor(), outputFormat);
                }
            }
            init(outputFilePath, trackCount, orientationHint, outputFormat);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new MediaTargetException(INVALID_PARAMS, outputFilePath, outputFormat, illegalArgumentException);
        } catch (IOException ioException) {
            releaseFileDescriptor();
            throw new MediaTargetException(IO_FAILUE, outputFilePath, outputFormat, ioException);
        }
    }

    public MediaMuxerMediaTarget(@NonNull String outputFilePath, @IntRange(from = 1) int trackCount, int orientationHint, int outputFormat) throws MediaTargetException {
        init(outputFilePath, trackCount, orientationHint, outputFormat);
    }

    private void init(@NonNull String outputFilePath, @IntRange(from = 1) int trackCount, int orientationHint,
            int outputFormat) throws MediaTargetException {
        this.outputFilePath = outputFilePath;
        this.trackCount = trackCount;

        try {
            if (mediaMuxer == null) {
                mediaMuxer = new MediaMuxer(outputFilePath,  outputFormat);
            }
            mediaMuxer.setOrientationHint(orientationHint);

            numberOfTracksToAdd = 0;
            isStarted = false;
            queue = new LinkedList<>();
            mediaFormatsToAdd = new MediaFormat[trackCount];
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new MediaTargetException(INVALID_PARAMS, outputFilePath, outputFormat, illegalArgumentException);
        } catch (IOException ioException) {
            throw new MediaTargetException(IO_FAILUE, outputFilePath, outputFormat, ioException);
        }
    }

    @Override
    public int addTrack(@NonNull MediaFormat mediaFormat,  @IntRange(from = 0) int targetTrack) {
        mediaFormatsToAdd[targetTrack] = mediaFormat;
        numberOfTracksToAdd++;

        if (numberOfTracksToAdd == trackCount) {
            Log.d(TAG, "All tracks added, starting MediaMuxer, writing out " + queue.size() + " queued samples");

            for (MediaFormat trackMediaFormat : mediaFormatsToAdd) {
                mediaMuxer.addTrack(trackMediaFormat);
            }

            mediaMuxer.start();
            isStarted = true;

            // write out queued items
            while (!queue.isEmpty()) {
                MediaSample mediaSample = queue.removeFirst();
                mediaMuxer.writeSampleData(mediaSample.targetTrack, mediaSample.buffer, mediaSample.info);
            }
        }

        return targetTrack;
    }

    @Override
    public void writeSampleData(int targetTrack, @NonNull ByteBuffer buffer, @NonNull MediaCodec.BufferInfo info) {
        if (isStarted) {
            if (buffer == null) {
                Log.e(TAG, "Trying to write a null buffer, skipping");
            } else {
                mediaMuxer.writeSampleData(targetTrack, buffer, info);
            }
        } else {
            // media muxer is not started yet, so queue up incoming buffers to write them out later
            MediaSample mediaSample = new MediaSample(targetTrack, buffer, info);
            queue.addLast(mediaSample);
        }
    }

    @Override
    public void release() {
        mediaMuxer.release();
        releaseFileDescriptor();
    }

    @Override
    @NonNull
    public String getOutputFilePath() {
        return outputFilePath;
    }

    private void releaseFileDescriptor() {
        try {
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
                parcelFileDescriptor = null;
            }
        } catch (IOException ignored) {
        }
    }

    private class MediaSample {
        private int targetTrack;
        private ByteBuffer buffer;
        private MediaCodec.BufferInfo info;

        private MediaSample(int targetTrack, ByteBuffer buffer, MediaCodec.BufferInfo info) {
            this.targetTrack = targetTrack;

            this.info = new MediaCodec.BufferInfo();
            this.info.set(0, info.size, info.presentationTimeUs, info.flags);

            // we want to make a deep copy so we can release the incoming buffer back to encoder immediately
            this.buffer = ByteBuffer.allocate(buffer.capacity());
            this.buffer.put(buffer);
            this.buffer.flip();
        }
    }
}
