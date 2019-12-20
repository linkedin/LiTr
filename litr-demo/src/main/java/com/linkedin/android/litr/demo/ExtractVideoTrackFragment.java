/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaCodecInfo;
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
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.GlVideoRenderer;
import com.linkedin.android.litr.render.VideoRenderer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ExtractVideoTrackFragment extends BaseDemoFragment {

    private static final String TAG = ExtractVideoTrackFragment.class.getSimpleName();

    @Override
    protected void transform(@NonNull Uri sourceVideoUri,
                             @Nullable Uri overlayUri,
                             @NonNull File targetVideoFile,
                             @Nullable MediaFormat targetVideoFormat,
                             @Nullable MediaFormat targetAudioFormat) {
        targetVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        int width = targetVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = targetVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);

        String requestId = UUID.randomUUID().toString();

        List<GlFilter> glFilters = null;
        if (overlayUri != null) {
            try {
                Context context = getContext();
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(overlayUri));
                if (bitmap != null) {
                    int videoRotation = getVideoRotation();

                    float overlayWidth = 0.56f;
                    float overlayHeight;
                    if (videoRotation == 90 || videoRotation == 270) {
                        float overlayWidthPixels = overlayWidth * height;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / width;
                    } else {
                        float overlayWidthPixels = overlayWidth * width;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / height;
                    }

                    PointF position = new PointF(0.6f, 0.4f);
                    PointF size = new PointF(overlayWidth, overlayHeight);
                    float rotation = 30;

                    GlFilter filter = createGlFilter(overlayUri, size, position, rotation);
                    if (filter != null) {
                        glFilters = Collections.singletonList(filter);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Failed to extract audio track metadata", ex);
            }
        }

        try {
            MediaSource mediaSource = new MediaExtractorMediaSource(getContext().getApplicationContext(), sourceVideoUri);
            VideoRenderer renderer = new GlVideoRenderer(glFilters);
            Decoder decoder = new MediaCodecDecoder();
            Encoder encoder = new MediaCodecEncoder();

            for (int track = 0; track < mediaSource.getTrackCount(); track++) {
                MediaFormat trackFormat = mediaSource.getTrackFormat(track);
                if (trackFormat.containsKey(MediaFormat.KEY_MIME) && trackFormat.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                    MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetVideoFile.getAbsolutePath(),
                                                                        1,
                                                                        mediaSource.getOrientationHint(),
                                                                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                    TrackTransform trackTransform = new TrackTransform.Builder(mediaSource, track, mediaTarget)
                        .setDecoder(decoder)
                        .setRenderer(renderer)
                        .setEncoder(encoder)
                        .setTargetTrack(0)
                        .setTargetFormat(targetVideoFormat)
                        .build();

                    mediaTransformer.transform(requestId,
                                               Collections.singletonList(trackTransform),
                                               videoTransformationListener,
                                               MediaTransformer.GRANULARITY_DEFAULT);

                    return;
                }
            }
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception thrown when trying to extract video track", ex);
        }
    }
}
