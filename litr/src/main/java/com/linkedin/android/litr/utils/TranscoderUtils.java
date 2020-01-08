/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.media.MediaFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.io.MediaSource;

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
