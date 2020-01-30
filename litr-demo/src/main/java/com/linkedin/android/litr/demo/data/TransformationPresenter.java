/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.GlVideoRenderer;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TransformationPresenter {

    private static final String TAG = TransformationPresenter.class.getSimpleName();

    private final Context context;
    private final MediaTransformer mediaTransformer;

    public TransformationPresenter(@NonNull Context context,
                                   @NonNull MediaTransformer mediaTransformer) {
        this.context = context;
        this.mediaTransformer = mediaTransformer;
    }

    public void startTransformation(@NonNull SourceMedia sourceMedia,
                                    @NonNull TargetMedia targetMedia,
                                    @NonNull TransformationState transformationState) {
        if (targetMedia.getIncludedTrackCount() < 1) {
            return;
        }

        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                                                                                             transformationState.requestId,
                                                                                             transformationState);

        try {
            int videoRotation = 0;
            for (MediaTrackFormat trackFormat : sourceMedia.tracks) {
                if (trackFormat.mimeType.startsWith("video")) {
                    videoRotation = ((VideoTrackFormat) trackFormat).rotation;
                    break;
                }
            }
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetMedia.targetFile.getPath(),
                                                                targetMedia.getIncludedTrackCount(),
                                                                videoRotation,
                                                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            List<TrackTransform> trackTransforms = new ArrayList<>(targetMedia.tracks.size());
            MediaSource mediaSource = new MediaExtractorMediaSource(context, sourceMedia.uri);

            for (TargetTrack targetTrack : targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue;
                }
                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource,
                                                                                          targetTrack.sourceTrackIndex,
                                                                                          mediaTarget)
                    .setTargetTrack(trackTransforms.size())
                    .setTargetFormat(targetTrack.shouldTranscode ? createMediaFormat(targetTrack) : null)
                    .setEncoder(new MediaCodecEncoder())
                    .setDecoder(new MediaCodecDecoder());
                if (targetTrack.format instanceof VideoTrackFormat) {
                    trackTransformBuilder.setRenderer(new GlVideoRenderer(createGlFilters(sourceMedia, (TargetVideoTrack) targetTrack)));
                }

                trackTransforms.add(trackTransformBuilder.build());
            }

            mediaTransformer.transform(transformationState.requestId,
                                       trackTransforms,
                                       transformationListener,
                                       MediaTransformer.GRANULARITY_DEFAULT);
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception when trying to perform track operation", ex);
        }
    }

    public void cancelTransformation(@NonNull String requestId) {
        mediaTransformer.cancel(requestId);
    }

    public void play(@Nullable File targetFile) {
        if (targetFile != null && targetFile.exists()) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            Uri videoUri = FileProvider.getUriForFile(context,
                                                      context.getPackageName() + ".provider",
                                                      targetFile);
            playIntent.setDataAndType(videoUri, "video/*");
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(playIntent);
        }
    }

    @Nullable
    private List<GlFilter> createGlFilters(@NonNull SourceMedia sourceMedia, @Nullable TargetVideoTrack targetTrack) {
        List<GlFilter> glFilters = null;
        if (targetTrack != null && targetTrack.overlay != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(targetTrack.overlay));
                if (bitmap != null) {
                    float overlayWidth = 0.56f;
                    float overlayHeight;
                    VideoTrackFormat sourceVideoTrackFormat = (VideoTrackFormat) sourceMedia.tracks.get(targetTrack.sourceTrackIndex);
                    if (sourceVideoTrackFormat.rotation == 90 || sourceVideoTrackFormat.rotation == 270) {
                        float overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.height;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceVideoTrackFormat.width;
                    } else {
                        float overlayWidthPixels = overlayWidth * sourceVideoTrackFormat.width;
                        float overlayHeightPixels = overlayWidthPixels * bitmap.getHeight() / bitmap.getWidth();
                        overlayHeight = overlayHeightPixels / sourceVideoTrackFormat.height;
                    }

                    PointF position = new PointF(0.6f, 0.4f);
                    PointF size = new PointF(overlayWidth, overlayHeight);
                    float rotation = 30;

                    GlFilter filter = TransformationUtil.createGlFilter(context,
                                                                        targetTrack.overlay,
                                                                        size,
                                                                        position,
                                                                        rotation);
                    if (filter != null) {
                        glFilters = Collections.singletonList(filter);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Failed to extract audio track metadata: " + ex);
            }
        }

        return glFilters;
    }

    @Nullable
    private MediaFormat createMediaFormat(@Nullable TargetTrack targetTrack) {
        MediaFormat mediaFormat = null;
        if (targetTrack != null && targetTrack.format != null) {
            mediaFormat = new MediaFormat();
            if (targetTrack.format.mimeType.startsWith("video")) {
                VideoTrackFormat trackFormat = (VideoTrackFormat) targetTrack.format;
                mediaFormat.setString(MediaFormat.KEY_MIME, "video/avc");
                mediaFormat.setInteger(MediaFormat.KEY_WIDTH, trackFormat.width);
                mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, trackFormat.height);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, trackFormat.keyFrameInterval);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, trackFormat.frameRate);
            } else if (targetTrack.format.mimeType.startsWith("audio")) {
                AudioTrackFormat trackFormat = (AudioTrackFormat) targetTrack.format;
                mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
                mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, trackFormat.channelCount);
                mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, trackFormat.samplingRate);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
            }
        }

        return mediaFormat;
    }
}
