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
    private TranscoderUtils() {
    }

    public static long getEstimatedTargetVideoFileSize(@NonNull MediaSource mediaSource,
                                                       @NonNull MediaFormat targetVideoFormat,
                                                       @Nullable MediaFormat targetAudioFormat) {
        long estimatedFileSize = 0;

        int trackCount = mediaSource.getTrackCount();
        List<MediaFormat> trackFormats = new ArrayList<>(trackCount);

        // calculate maximum track duration, we might need it later
        long maxDurationUs = 0;
        for (int track = 0; track < trackCount; track++) {
            MediaFormat mediaFormat = mediaSource.getTrackFormat(track);
            trackFormats.add(mediaFormat);
            long durationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
            maxDurationUs = Math.max(durationUs, maxDurationUs);
        }

        for (MediaFormat trackFormat : trackFormats) {
            String mimeType = getMimeType(trackFormat);
            int bitrate = getBitrate(trackFormat);
            long duration = getDuration(trackFormat);

            if (duration < 0) {
                Log.d(TAG, "Track duration is not available, using maximum duration");
                duration = maxDurationUs;
            }

            if (mimeType == null) {
                Log.e(TAG, "No mime type for the track!");
            } else {
                if (mimeType.startsWith("video")) {
                    // if video track, use transcoding target bitrate
                    bitrate = targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                } else {
                    // everything else is passthrough, so use source bitrate
                    if (mimeType.startsWith("audio") && bitrate < 0) {
                        if (targetAudioFormat != null) {
                            bitrate = targetAudioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                        } else {
                            bitrate = COMMON_AUDIO_BITRATE_KBPS * BITS_IN_KILO;
                        }
                    }
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
