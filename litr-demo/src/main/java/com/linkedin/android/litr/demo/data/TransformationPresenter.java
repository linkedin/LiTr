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
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.TransformationOptions;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.codec.PassthroughDecoder;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.io.MockVideoMediaSource;
import com.linkedin.android.litr.render.GlVideoRenderer;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TransformationPresenter {

    private static final String TAG = TransformationPresenter.class.getSimpleName();

    private static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? MediaFormat.KEY_ROTATION
            : "rotation-degrees";

    private final Context context;
    private final MediaTransformer mediaTransformer;

    public TransformationPresenter(@NonNull Context context,
                                   @NonNull MediaTransformer mediaTransformer) {
        this.context = context;
        this.mediaTransformer = mediaTransformer;
    }

    public void startTransformation(@NonNull SourceMedia sourceMedia,
                                    @NonNull TargetMedia targetMedia,
                                    @NonNull TrimConfig trimConfig,
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


            MediaRange mediaRange = trimConfig.enabled
                    ? new MediaRange(
                            TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(0) * 1000)),
                            TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(1) * 1000)))
                    : new MediaRange(0, Long.MAX_VALUE);
            MediaSource mediaSource = new MediaExtractorMediaSource(context, sourceMedia.uri, mediaRange);

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
                    trackTransformBuilder.setRenderer(new GlVideoRenderer(createGlFilters(sourceMedia,
                            (TargetVideoTrack) targetTrack,
                            0.56f,
                            new PointF(0.6f, 0.4f),
                            30)));
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

    public void startVideoOverlayTransformation(@NonNull SourceMedia sourceMedia,
                                                @NonNull TargetMedia targetMedia,
                                                @NonNull TargetVideoConfiguration targetVideoConfiguration,
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
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetMedia.targetFile.getPath(),
                    targetMedia.getIncludedTrackCount(),
                    targetVideoConfiguration.rotation,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            List<TrackTransform> trackTransforms = new ArrayList<>(targetMedia.tracks.size());
            MediaSource mediaSource = new MediaExtractorMediaSource(context, sourceMedia.uri);

            for (TargetTrack targetTrack : targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue;
                }
                MediaFormat mediaFormat = createMediaFormat(targetTrack);
                if (mediaFormat != null && targetTrack.format instanceof VideoTrackFormat) {
                    mediaFormat.setInteger(KEY_ROTATION, targetVideoConfiguration.rotation);
                }
                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource,
                        targetTrack.sourceTrackIndex,
                        mediaTarget)
                        .setTargetTrack(trackTransforms.size())
                        .setTargetFormat(mediaFormat)
                        .setEncoder(new MediaCodecEncoder())
                        .setDecoder(new MediaCodecDecoder());
                if (targetTrack.format instanceof VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    List<GlFilter> filters = new ArrayList<>();
                    if (targetMedia.backgroundImageUri != null) {
                        GlFilter backgroundImageFilter = TransformationUtil.createGlFilter(context,
                                targetMedia.backgroundImageUri,
                                new PointF(1, 1),
                                new PointF(0.5f, 0.5f),
                                0);
                        filters.add(backgroundImageFilter);
                    }

                    Transform transform = new Transform(new PointF(0.25f, 0.25f), new PointF(0.65f, 0.55f), 30);
                    GlFrameRenderFilter frameRenderFilter = new DefaultVideoFrameRenderFilter(transform);
                    filters.add(frameRenderFilter);

                    trackTransformBuilder.setRenderer(new GlVideoRenderer(filters));
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

    public void squareCenterCrop(@NonNull SourceMedia sourceMedia,
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
                MediaFormat mediaFormat;
                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource,
                        targetTrack.sourceTrackIndex,
                        mediaTarget)
                        .setTargetTrack(trackTransforms.size())
                        .setEncoder(new MediaCodecEncoder())
                        .setDecoder(new MediaCodecDecoder());
                if (targetTrack.format instanceof VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    List<GlFilter> filters = new ArrayList<>();
                    if (targetMedia.backgroundImageUri != null) {
                        GlFilter backgroundImageFilter = TransformationUtil.createGlFilter(context,
                                targetMedia.backgroundImageUri,
                                new PointF(1, 1),
                                new PointF(0.5f, 0.5f),
                                0);
                        filters.add(backgroundImageFilter);
                    }

                    int width = ((VideoTrackFormat) targetTrack.format).width;
                    int height = ((VideoTrackFormat) targetTrack.format).height;
                    Transform transform;

                    if (videoRotation == 0 || videoRotation == 180) {
                        // landscape
                        transform = new Transform(new PointF((float) width / height, 1.0f), new PointF(0.5f, 0.5f), 0);
                    } else {
                        // portrait
                        transform = new Transform(new PointF(1.0f, (float) width / height), new PointF(0.5f, 0.5f), 0);
                    }

                    GlFrameRenderFilter frameRenderFilter = new DefaultVideoFrameRenderFilter(transform);
                    filters.add(frameRenderFilter);

                    trackTransformBuilder.setRenderer(new GlVideoRenderer(filters));

                    // hack to make video square, should be done more elegantly in prod code
                    ((VideoTrackFormat) targetTrack.format).width = TargetMedia.DEFAULT_VIDEO_HEIGHT;
                }
                mediaFormat = createMediaFormat(targetTrack);
                trackTransformBuilder.setTargetFormat(mediaFormat);
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

    public void applyWatermark(@NonNull SourceMedia sourceMedia,
                               @NonNull TargetMedia targetMedia,
                               @NonNull TrimConfig trimConfig,
                               @NonNull TransformationState transformationState) {
        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                transformationState.requestId,
                transformationState);

        List<GlFilter> watermarkImageFilter = null;
        for (TargetTrack targetTrack : targetMedia.tracks) {
            if (targetTrack instanceof TargetVideoTrack) {
                watermarkImageFilter = createGlFilters(
                        sourceMedia,
                        (TargetVideoTrack) targetTrack,
                        0.2f,
                        new PointF(0.8f, 0.8f),
                        0);
                break;
            }
        }

        MediaRange mediaRange = trimConfig.enabled
                ? new MediaRange(
                TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(0) * 1000)),
                TimeUnit.MILLISECONDS.toMicros((long) (trimConfig.range.get(1) * 1000)))
                : new MediaRange(0, Long.MAX_VALUE);
        TransformationOptions transformationOptions = new TransformationOptions.Builder()
                .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
                .setVideoFilters(watermarkImageFilter)
                .setSourceMediaRange(mediaRange)
                .build();

        mediaTransformer.transform(
                transformationState.requestId,
                sourceMedia.uri,
                targetMedia.targetFile.getPath(),
                null,
                null,
                transformationListener,
                transformationOptions);
    }

    public void applyFilter(@NonNull SourceMedia sourceMedia,
                            @NonNull TargetMedia targetMedia,
                            @NonNull TransformationState transformationState) {
        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(context,
                transformationState.requestId,
                transformationState);

        TransformationOptions transformationOptions = new TransformationOptions.Builder()
                .setGranularity(MediaTransformer.GRANULARITY_DEFAULT)
                .setVideoFilters(Collections.singletonList(targetMedia.filter))
                .build();

        mediaTransformer.transform(
                transformationState.requestId,
                sourceMedia.uri,
                targetMedia.targetFile.getPath(),
                null,
                null,
                transformationListener,
                transformationOptions);
    }

    public void createEmptyVideo(@NonNull SourceMedia sourceMedia,
                                 @NonNull TargetMedia targetMedia,
                                 @NonNull TransformationState transformationState) {
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

            if (sourceMedia.tracks.isEmpty()) {
                throw new IllegalArgumentException("No source tracks!");
            }

            MediaTrackFormat sourceTrackFormat = sourceMedia.tracks.get(0);
            MediaSource mediaSource =
                    new MockVideoMediaSource(createVideoMediaFormat((VideoTrackFormat) sourceTrackFormat));

            for (TargetTrack targetTrack : targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue;
                }
                MediaFormat mediaFormat = createMediaFormat(targetTrack);
                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource,
                        targetTrack.sourceTrackIndex,
                        mediaTarget)
                        .setTargetTrack(trackTransforms.size())
                        .setTargetFormat(mediaFormat)
                        .setEncoder(new MediaCodecEncoder())
                        .setDecoder(new PassthroughDecoder(1));
                if (targetTrack.format instanceof VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    List<GlFilter> filters = new ArrayList<>();
                    if (targetMedia.backgroundImageUri != null) {
                        GlFilter backgroundImageFilter = TransformationUtil.createGlFilter(context,
                                targetMedia.backgroundImageUri,
                                new PointF(1, 1),
                                new PointF(0.5f, 0.5f),
                                0);
                        filters.add(backgroundImageFilter);
                    }

                    if (((TargetVideoTrack) targetTrack).overlay != null) {
                        List<GlFilter> overlayFilters = createGlFilters(sourceMedia, (TargetVideoTrack) targetTrack, 0.3f, new PointF(0.25f, 0.25f), 30f);
                        if (overlayFilters != null) {
                            filters.addAll(overlayFilters);
                        }
                    }

                    trackTransformBuilder.setRenderer(new GlVideoRenderer(filters));
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
    private List<GlFilter> createGlFilters(@NonNull SourceMedia sourceMedia,
                                           @Nullable TargetVideoTrack targetTrack,
                                           float overlayWidth,
                                           @NonNull PointF position,
                                           float rotation) {
        List<GlFilter> glFilters = null;
        if (targetTrack != null && targetTrack.overlay != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(targetTrack.overlay));
                if (bitmap != null) {
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

                    PointF size = new PointF(overlayWidth, overlayHeight);

                    GlFilter filter = TransformationUtil.createGlFilter(context,
                                                                        targetTrack.overlay,
                                                                        size,
                                                                        position,
                                                                        rotation);
                    if (filter != null) {
                        glFilters = new ArrayList<>();
                        glFilters.add(filter);
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
                mediaFormat = createVideoMediaFormat((VideoTrackFormat) targetTrack.format);
            } else if (targetTrack.format.mimeType.startsWith("audio")) {
                mediaFormat = createAudioMediaFormat((AudioTrackFormat) targetTrack.format);
            }
        }

        return mediaFormat;
    }

    @NonNull
    private MediaFormat createVideoMediaFormat(@NonNull VideoTrackFormat trackFormat) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, trackFormat.width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, trackFormat.height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, trackFormat.keyFrameInterval);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, trackFormat.frameRate);
        mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);
        mediaFormat.setInteger(KEY_ROTATION, trackFormat.rotation);

        return mediaFormat;
    }

    @NonNull
    private MediaFormat createAudioMediaFormat(@NonNull AudioTrackFormat trackFormat) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, trackFormat.channelCount);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, trackFormat.samplingRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
        mediaFormat.setLong(MediaFormat.KEY_DURATION, trackFormat.duration);

        return mediaFormat;
    }
}
