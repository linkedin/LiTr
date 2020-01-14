/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.io.MediaSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class TranscoderUtils {

    @VisibleForTesting
    static final int COMMON_AUDIO_BITRATE_KBPS = 320;

    private static final String TAG = TranscoderUtils.class.getSimpleName();

    private static final int BITS_IN_KILO = 1000;
    private static final int BITS_IN_BYTE = 8;

    // Private constructor to prevent unintended instantiation of the TranscoderUtils class.
    private TranscoderUtils() {}

    /**
     * Estimate total target file size(s) for track transformation
     * @param trackTransforms track transformations
     * @return estimated size, zero if estimation fails
     */
    public static long getEstimatedTargetFileSize(@NonNull List<TrackTransform> trackTransforms) {
        long estimatedFileSize = 0;

        // calculate maximum track duration, we might need it later
        long maxDurationUs = 0;
        for (TrackTransform trackTransform : trackTransforms) {
            MediaFormat trackFormat = trackTransform.getMediaSource().getTrackFormat(trackTransform.getSourceTrack());
            long durationUs = trackFormat.getLong(MediaFormat.KEY_DURATION);
            maxDurationUs = Math.max(durationUs, maxDurationUs);
        }

        for (TrackTransform trackTransform : trackTransforms) {
            MediaFormat sourceTrackFormat = trackTransform.getMediaSource().getTrackFormat(trackTransform.getSourceTrack());
            int bitrate = getBitrate(sourceTrackFormat);
            long duration = getDuration(sourceTrackFormat);

            if (duration < 0) {
                Log.d(TAG, "Track duration is not available, using maximum duration");
                duration = maxDurationUs;
            }

            String mimeType = getMimeType(sourceTrackFormat);
            if (mimeType != null) {
                if (trackTransform.getTargetFormat() != null) {
                    bitrate = trackTransform.getTargetFormat().getInteger(MediaFormat.KEY_BIT_RATE);
                } else if (mimeType.startsWith("audio") && bitrate < 0) {
                    bitrate = COMMON_AUDIO_BITRATE_KBPS * BITS_IN_KILO;
                }
            }

            if (bitrate < 0) {
                Log.d(TAG, "Bitrate is not available, cannot use that track to estimate sie");
                bitrate = 0;
            }

            estimatedFileSize += bitrate * TimeUnit.MICROSECONDS.toSeconds(duration);
        }

        estimatedFileSize /= BITS_IN_BYTE;

        return estimatedFileSize;
    }

    /**
     * Estimate target file size for a video with one video and one audio track
     * @param mediaSource source video
     * @param targetVideoFormat target video format
     * @param targetAudioFormat target audio format, null if not transformed
     * @return estimated size in bytes, zero if estimation fails
     */
    public static long getEstimatedTargetVideoFileSize(@NonNull MediaSource mediaSource,
                                                       @NonNull MediaFormat targetVideoFormat,
                                                       @Nullable MediaFormat targetAudioFormat) {
        List<TrackTransform> trackTransforms = new ArrayList<>(mediaSource.getTrackCount());
        for (int track = 0; track < mediaSource.getTrackCount(); track++) {
            MediaFormat sourceMediaFormat = mediaSource.getTrackFormat(track);
            // we are passing null for MediaTarget here, because MediaTarget is not used when estimating target size
            TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource, track, null);
            if (sourceMediaFormat.containsKey(MediaFormat.KEY_MIME)) {
                String mimeType = sourceMediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video")) {
                    trackTransformBuilder.setTargetFormat(targetVideoFormat);
                } else if (mimeType.startsWith("audio")) {
                    trackTransformBuilder.setTargetFormat(targetAudioFormat);
                }
            }
            trackTransforms.add(trackTransformBuilder.build());
        }

        return getEstimatedTargetFileSize(trackTransforms);
    }

    /**
     * Estimates video track bitrate. On many devices bitrate value is not specified in {@link MediaFormat} for video track.
     * Since not all data required for accurate estimation is available, this method makes several assumptions:
     *  - if multiple video tracks are present, average per-pixel bitrate is assumed to be the same for all tracks
     *  - if both bitrate and duration are not specified for a track, its size is not accounted for
     * @param mediaSource {@link MediaSource} which contains the video track
     * @param trackIndex index of video track
     * @return estimated bitrate in bits per second
     */
    public static int estimateVideoTrackBitrate(@NonNull MediaSource mediaSource, int trackIndex) {
        long unallocatedSize = mediaSource.getSize();
        long totalPixels = 0;
        int trackCount = mediaSource.getTrackCount();
        for (int track = 0; track < trackCount; track++) {
            MediaFormat trackFormat = mediaSource.getTrackFormat(track);
            if (trackFormat.containsKey(MediaFormat.KEY_MIME)) {
                if (trackFormat.containsKey(MediaFormat.KEY_BIT_RATE) && trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    int bitrate = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                    long duration = trackFormat.getLong(MediaFormat.KEY_DURATION);
                    unallocatedSize -= bitrate * TimeUnit.MICROSECONDS.toSeconds(duration);
                } else {
                    String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                    if (mimeType.startsWith("video")) {
                        totalPixels += trackFormat.getInteger(MediaFormat.KEY_WIDTH)
                            * trackFormat.getInteger(MediaFormat.KEY_HEIGHT)
                            * TimeUnit.MICROSECONDS.toSeconds(trackFormat.getLong(MediaFormat.KEY_DURATION));
                    }
                }
            }
        }

        MediaFormat videoTrackFormat = mediaSource.getTrackFormat(trackIndex);
        long trackPixels = videoTrackFormat.getInteger(MediaFormat.KEY_WIDTH)
            * videoTrackFormat.getInteger(MediaFormat.KEY_HEIGHT)
            * TimeUnit.MICROSECONDS.toSeconds(videoTrackFormat.getLong(MediaFormat.KEY_DURATION));

        long trackSize = unallocatedSize * trackPixels / totalPixels;

        return (int) (trackSize * 8 / TimeUnit.MICROSECONDS.toSeconds(videoTrackFormat.getLong(MediaFormat.KEY_DURATION)));
    }

    /**
     * Get size of data abstracted by uri
     * @param context context to access uri
     * @param uri uri
     * @return size in bytes, -1 if unknown
     */
    public static long getSize(@NonNull Context context, @NonNull Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            AssetFileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                long size = fileDescriptor != null ? fileDescriptor.getParcelFileDescriptor().getStatSize() : 0;
                return size < 0 ? -1 : size;
            } catch (FileNotFoundException | IllegalStateException e) {
                Log.e(TAG, "Unable to extract length from targetFile: " + uri, e);
                return -1;
            } finally {
                if (fileDescriptor != null) {
                    try {
                        fileDescriptor.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close file descriptor from targetFile: " + uri, e);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) && uri.getPath() != null) {
            File file = new File(uri.getPath());
            return file.length();
        } else {
            return -1;
        }
    }

    @Nullable
    private static String getMimeType(@NonNull MediaFormat trackFormat) {
        String mimeType = null;
        if (trackFormat.containsKey(MediaFormat.KEY_MIME)) {
            mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
        }
        return mimeType;
    }

    private static int getBitrate(@NonNull MediaFormat trackFormat) {
        int bitrate = -1;
        if (trackFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            bitrate = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        }
        return bitrate;
    }

    private static long getDuration(@NonNull MediaFormat trackFormat) {
        long duration = -1;
        if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
            duration = trackFormat.getLong(MediaFormat.KEY_DURATION);
        }
        return duration;
    }
}
