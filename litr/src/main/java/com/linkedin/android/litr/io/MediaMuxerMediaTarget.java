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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import static com.linkedin.android.litr.exception.MediaTargetException.Error.INVALID_PARAMS;
import static com.linkedin.android.litr.exception.MediaTargetException.Error.IO_FAILUE;
import static com.linkedin.android.litr.exception.MediaTargetException.Error.UNSUPPORTED_URI_TYPE;

/**
 * An implementation of MediaTarget, which wraps Android's {@link MediaMuxer}
 *
 * Before writing any media samples to MediaMuxer, all tracks must be added and MediaMuxer must be started. Some track
 * transcoders may start writing their output before other track transcoders added their track. This class queues writing
 * media samples until all tracks are created, allowing track transcoders to work independently.
 */

public class MediaMuxerMediaTarget implements MediaTarget {
    private static final String TAG = MediaMuxerMediaTarget.class.getSimpleName();

    @VisibleForTesting LinkedList<MediaTargetSample> queue;
    @VisibleForTesting boolean isStarted;
    @VisibleForTesting MediaMuxer mediaMuxer;

    private MediaFormat[] mediaFormatsToAdd;

    private ParcelFileDescriptor parcelFileDescriptor;
    private String outputFilePath;
    private int numberOfTracksToAdd;
    private int trackCount;

    /**
     * Create an instance using input URI. On Android Oreo (API level 26) and above it can be any URI writeable by the
     * provided context, otherwise it must be a "file://" URI. This constructor is especially useful when working with
     * FileProvider created URI to comply with scoped storage enforcement on Android 10+
     */
    public MediaMuxerMediaTarget(@NonNull Context context, @NonNull Uri outputFileUri,
            @IntRange(from = 1) int trackCount, int orientationHint, int outputFormat) throws MediaTargetException {
        try {
            MediaMuxer mediaMuxer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                parcelFileDescriptor = context.getContentResolver().openFileDescriptor(outputFileUri, "rwt");
                if (parcelFileDescriptor != null) {
                    mediaMuxer = new MediaMuxer(parcelFileDescriptor.getFileDescriptor(), outputFormat);
                } else {
                    throw new IOException("Inaccessible URI " + outputFileUri);
                }
            } else if ("file".equalsIgnoreCase(outputFileUri.getScheme()) && outputFileUri.getPath() != null) {
                mediaMuxer = new MediaMuxer(outputFileUri.getPath(), outputFormat);
            } else {
                throw new MediaTargetException(UNSUPPORTED_URI_TYPE, outputFileUri, outputFormat, new Throwable());
            }
            init(mediaMuxer, trackCount, orientationHint);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new MediaTargetException(INVALID_PARAMS, outputFileUri, outputFormat, illegalArgumentException);
        } catch (IOException ioException) {
            releaseFileDescriptor();
            throw new MediaTargetException(IO_FAILUE, outputFileUri, outputFormat, ioException);
        }
    }

    public MediaMuxerMediaTarget(@NonNull String outputFilePath, @IntRange(from = 1) int trackCount,
            int orientationHint, int outputFormat) throws MediaTargetException {
        this.outputFilePath = outputFilePath;
        try {
            MediaMuxer mediaMuxer = new MediaMuxer(outputFilePath, outputFormat);
            init(mediaMuxer, trackCount, orientationHint);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new MediaTargetException(INVALID_PARAMS, outputFilePath, outputFormat, illegalArgumentException);
        } catch (IOException ioException) {
            throw new MediaTargetException(IO_FAILUE, outputFilePath, outputFormat, ioException);
        }
    }

    private void init(@NonNull MediaMuxer mediaMuxer, @IntRange(from = 1) int trackCount, int orientationHint) throws IllegalArgumentException {
        this.trackCount = trackCount;

        this.mediaMuxer = mediaMuxer;
        this.mediaMuxer.setOrientationHint(orientationHint);

        numberOfTracksToAdd = 0;
        isStarted = false;
        queue = new LinkedList<>();
        mediaFormatsToAdd = new MediaFormat[trackCount];
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
                MediaTargetSample mediaSample = queue.removeFirst();
                mediaMuxer.writeSampleData(mediaSample.getTargetTrack(), mediaSample.getBuffer(), mediaSample.getInfo());
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
            MediaTargetSample mediaSample = new MediaTargetSample(targetTrack, buffer, info);
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
        return outputFilePath != null ? outputFilePath : "";
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
}
