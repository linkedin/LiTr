/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;

import java.io.File;
import java.util.Collections;
import java.util.UUID;

public class ExtractAudioTrackFragment extends BaseDemoFragment {

    private static final String TAG = ExtractAudioTrackFragment.class.getSimpleName();

    @Override
    protected void transform(@NonNull Uri sourceVideoUri,
                             @Nullable Uri overlayUri,
                             @NonNull File targetVideoFile,
                             @Nullable MediaFormat targetVideoFormat,
                             @Nullable MediaFormat targetAudioFormat) {
        try {
            String requestId = UUID.randomUUID().toString();

            MediaSource mediaSource = new MediaExtractorMediaSource(getContext().getApplicationContext(), sourceVideoUri);
            Decoder decoder = new MediaCodecDecoder();
            Encoder encoder = new MediaCodecEncoder();

            for (int track = 0; track < mediaSource.getTrackCount(); track++) {
                MediaFormat trackFormat = mediaSource.getTrackFormat(track);
                if (trackFormat.containsKey(MediaFormat.KEY_MIME) && trackFormat.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetVideoFile.getAbsolutePath(),
                                                                        1,
                                                                        mediaSource.getOrientationHint(),
                                                                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                    TrackTransform trackTransform = new TrackTransform.Builder(mediaSource, track, mediaTarget)
                        .setDecoder(decoder)
                        .setEncoder(encoder)
                        .setTargetTrack(0)
                        .setTargetFormat(targetAudioFormat)
                        .build();

                    mediaTransformer.transform(requestId,
                                               Collections.singletonList(trackTransform),
                                               videoTransformationListener,
                                               MediaTransformer.GRANULARITY_DEFAULT);

                    return;
                }
            }
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception thrown when trying to extract audio track", ex);
        }
    }
}
