/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import android.content.Context;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.exception.MediaSourceException;
import com.linkedin.android.litr.exception.MediaTargetException;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.GlVideoRenderer;
import com.linkedin.android.litr.render.Renderer;
import com.linkedin.android.litr.utils.CodecUtils;
import com.linkedin.android.litr.utils.TranscoderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is the main entry point into LiTr. Using it is very straightforward:
 *  - instantiate with a context (usually, application context)
 *  - transform video/audio, make sure to provide unique tag for each transformation
 *  - listen on each transformation using a listener (callbacks happen on UI thread)
 *  - cancel transformation using its tag
 *  - release when you no longer need it
 */
public class MediaTransformer {
    public static final int GRANULARITY_NONE = 0;
    public static final int GRANULARITY_DEFAULT = 100;

    public static final int DEFAULT_KEY_FRAME_INTERVAL = 5;

    private static final String TAG = MediaTransformer.class.getSimpleName();
    private static final int DEFAULT_FUTURE_MAP_SIZE = 10;

    private final Context context;

    private ExecutorService executorService;
    private Looper looper;

    private Map<String, Future<?>> futureMap;

    /**
     * Instantiate MediaTransformer. Listener callbacks will be done on main UI thread.
     * All transformations will be done on a single thread.
     * @param context context with access to source and target URIs and other resources
     */
    public MediaTransformer(@NonNull Context context) {
        this(context, Looper.getMainLooper(), Executors.newSingleThreadExecutor());
    }

    /**
     * Instantiate MediaTransformer
     * @param context context with access to source and target URIs and other resources
     * @param looper {@link Looper} of a thread to marshal listener callbacks to, null for calling back on an ExecutorService thread.
     * @param executorService {@link ExecutorService} to use for transformation jobs
     */
    public MediaTransformer(@NonNull Context context, @Nullable Looper looper, @Nullable ExecutorService executorService) {
        this.context = context.getApplicationContext();

        futureMap = new HashMap<>(DEFAULT_FUTURE_MAP_SIZE);
        this.looper = looper;
        this.executorService = executorService;
    }

    /**
     * Transform video and audio track(s): change resolution, frame rate, bitrate, etc. Video track transformation
     * uses default hardware accelerated codecs and OpenGL renderer.
     *
     * If overlay(s) are provided, video track(s) will be transcoded with parameters as close to source format as possible.
     *
     * @param requestId client defined unique id for a transformation request. If not unique, {@link IllegalArgumentException} will be thrown.
     * @param inputUri input video {@link Uri}
     * @param outputFilePath Absolute path of output media file
     * @param targetVideoFormat target format parameters for video track(s), null to keep them as is
     * @param targetAudioFormat target format parameters for audio track(s), null to keep them as is
     * @param listener {@link TransformationListener} implementation, to get updates on transformation status/result/progress
     * @param granularity progress reporting granularity. NO_GRANULARITY for per-frame progress reporting,
     *                    or positive integer value for number of times transformation progress should be reported
     * @param filters optional OpenGL filters to apply to video frames
     *
     * @deprecated use the version with {@link TransformationOptions}
     */
    @Deprecated
    public void transform(@NonNull String requestId,
                          @NonNull Uri inputUri,
                          @NonNull String outputFilePath,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity,
                          @Nullable List<GlFilter> filters) {
        TransformationOptions transformationOptions = new TransformationOptions.Builder()
                .setGranularity(granularity)
                .setVideoFilters(filters)
                .build();
        transform(requestId,
                inputUri,
                outputFilePath,
                targetVideoFormat,
                targetAudioFormat,
                listener,
                transformationOptions);
    }

    /**
     * Transform video and audio track(s): change resolution, frame rate, bitrate, etc. Video track transformation
     * uses default hardware accelerated codecs and OpenGL renderer.
     *
     * If overlay(s) are provided, video track(s) will be transcoded with parameters as close to source format as possible.
     *
     * @param requestId client defined unique id for a transformation request. If not unique, {@link IllegalArgumentException} will be thrown.
     * @param inputUri input video {@link Uri}
     * @param outputFilePath Absolute path of output media file
     * @param targetVideoFormat target format parameters for video track(s), null to keep them as is
     * @param targetAudioFormat target format parameters for audio track(s), null to keep them as is
     * @param listener {@link TransformationListener} implementation, to get updates on transformation status/result/progress
     * @param transformationOptions optional instance of {@link TransformationOptions}
     */
    public void transform(@NonNull String requestId,
                          @NonNull Uri inputUri,
                          @NonNull String outputFilePath,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @Nullable TransformationOptions transformationOptions) {

        TransformationOptions options = transformationOptions == null
                ? new TransformationOptions.Builder().build()
                : transformationOptions;

        try {
            MediaSource mediaSource = new MediaExtractorMediaSource(context, inputUri, options.sourceMediaRange);
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(
                    outputFilePath,
                    mediaSource.getTrackCount(),
                    mediaSource.getOrientationHint(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int trackCount = mediaSource.getTrackCount();
            List<TrackTransform> trackTransforms = new ArrayList<>(trackCount);
            for (int track = 0; track < trackCount; track++) {
                MediaFormat sourceMediaFormat = mediaSource.getTrackFormat(track);
                String mimeType = null;
                if (sourceMediaFormat.containsKey(MediaFormat.KEY_MIME)) {
                    mimeType = sourceMediaFormat.getString(MediaFormat.KEY_MIME);
                }

                if (mimeType == null) {
                    Log.e(TAG, "Mime type is null for track " + track);
                    continue;
                }

                Decoder decoder = new MediaCodecDecoder();
                Encoder encoder = new MediaCodecEncoder();

                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource, track, mediaTarget)
                        .setTargetTrack(track);

                if (mimeType.startsWith("video")) {
                    trackTransformBuilder.setDecoder(decoder)
                            .setRenderer(new GlVideoRenderer(options.videoFilters))
                            .setEncoder(encoder)
                            .setTargetFormat(targetVideoFormat);
                } else if (mimeType.startsWith("audio")) {
                    trackTransformBuilder.setDecoder(decoder)
                            .setEncoder(encoder)
                            .setTargetFormat(targetAudioFormat);
                }

                trackTransforms.add(trackTransformBuilder.build());
            }

            transform(requestId, trackTransforms, listener, options.granularity);
        } catch (MediaSourceException | MediaTargetException ex) {
            listener.onError(requestId, ex, null);
        }
    }

    /**
     * Transform audio/video tracks using provided transformation components (source, target, decoder, etc.)
     * It is up to a client to provide component implementations that work together - for example, output of media source
     * should be in format that decoder would accept, or renderer should use OpenGL if decoder and encoder use Surface.
     *
     * If renderer has overlay(s), video track(s) will be transcoded with parameters as close to source format as possible.
     *
     * @param requestId client defined unique id for a transformation request. If not unique, {@link IllegalArgumentException} will be thrown.
     * @param mediaSource {@link MediaSource} to provide input frames
     * @param decoder {@link Decoder} to decode input video frames
     * @param videoRenderer {@link Renderer} to draw (with optional filters) decoder's output frame onto encoder's input frame
     * @param encoder {@link Encoder} to encode output video frames into target format
     * @param mediaTarget {@link MediaTarget} to write/mux output frames
     * @param targetVideoFormat target format parameters for video track(s), null to keep them as is
     * @param targetAudioFormat target format parameters for audio track(s), null to keep them as is
     * @param listener {@link TransformationListener} implementation, to get updates on transformation status/result/progress
     * @param granularity progress reporting granularity. NO_GRANULARITY for per-frame progress reporting,
     *                    or positive integer value for number of times transformation progress should be reported
     * @deprecated use transform(List<TrackTransform>> instead
     */
    @Deprecated
    public void transform(@NonNull String requestId,
                          @NonNull MediaSource mediaSource,
                          @NonNull Decoder decoder,
                          @NonNull Renderer videoRenderer,
                          @NonNull Encoder encoder,
                          @NonNull MediaTarget mediaTarget,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity) {
        if (futureMap.containsKey(requestId)) {
            throw new IllegalArgumentException("Request with id " + requestId + " already exists");
        }

        int trackCount = mediaSource.getTrackCount();
        List<TrackTransform> trackTransforms = new ArrayList<>(trackCount);
        for (int track = 0; track < trackCount; track++) {
            MediaFormat sourceMediaFormat = mediaSource.getTrackFormat(track);
            String mimeType = null;
            if (sourceMediaFormat.containsKey(MediaFormat.KEY_MIME)) {
                mimeType = sourceMediaFormat.getString(MediaFormat.KEY_MIME);
            }

            if (mimeType == null) {
                Log.e(TAG, "Mime type is null for track " + track);
                continue;
            }

            TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource, track, mediaTarget)
                .setTargetTrack(track);

            if (mimeType.startsWith("video")) {
                trackTransformBuilder.setDecoder(decoder)
                                     .setRenderer(videoRenderer)
                                     .setEncoder(encoder)
                                     .setTargetFormat(targetVideoFormat);
            } else if (mimeType.startsWith("audio")) {
                trackTransformBuilder.setDecoder(new MediaCodecDecoder())
                                     .setEncoder(new MediaCodecEncoder())
                                     .setTargetFormat(targetAudioFormat);
            }

            trackTransforms.add(trackTransformBuilder.build());
        }

        transform(requestId, trackTransforms, listener, granularity);
    }

    /**
     * Transform using specific track transformation instructions. This allows things muxing/demuxing tracks, applying
     * different transformations to different tracks, etc.
     *
     * If a track renderer has overlay(s), that track will be transcoded with parameters as close to source format as possible.
     *
     * @param requestId client defined unique id for a transformation request. If not unique, {@link IllegalArgumentException} will be thrown.
     * @param trackTransforms list of track transformation instructions
     * @param listener {@link TransformationListener} implementation, to get updates on transformation status/result/progress
     * @param granularity progress reporting granularity. NO_GRANULARITY for per-frame progress reporting,
     *                    or positive integer value for number of times transformation progress should be reported
     */
    public void transform(@NonNull String requestId,
                          List<TrackTransform> trackTransforms,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity) {
        if (futureMap.containsKey(requestId)) {
            throw new IllegalArgumentException("Request with id " + requestId + " already exists");
        }

        int trackCount = trackTransforms.size();
        for (int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
            TrackTransform trackTransform = trackTransforms.get(trackIndex);
            if (trackTransform.getTargetFormat() == null
                && trackTransform.getRenderer() != null
                && trackTransform.getRenderer().hasFilters()) {
                // target format is null, but track has overlays, which means that we cannot use passthrough transcoder
                // so we transcode the track using source parameters (resolution, bitrate) as a target
                MediaFormat targetFormat = createTargetMediaFormat(trackTransform.getMediaSource(),
                                                                   trackTransform.getSourceTrack());
                TrackTransform updatedTrackTransform = new TrackTransform.Builder(trackTransform.getMediaSource(),
                                                                                  trackTransform.getSourceTrack(),
                                                                                  trackTransform.getMediaTarget())
                    .setTargetTrack(trackTransform.getTargetTrack())
                    .setDecoder(trackTransform.getDecoder())
                    .setEncoder(trackTransform.getEncoder())
                    .setRenderer(trackTransform.getRenderer())
                    .setTargetFormat(targetFormat)
                    .build();

                trackTransforms.set(trackIndex, updatedTrackTransform);
            }
        }

        TransformationJob transformationJob = new TransformationJob(requestId,
                                                                    trackTransforms,
                                                                    granularity,
                                                                    new MarshallingTransformationListener(futureMap, listener, looper));
        Future<?> future = executorService.submit(transformationJob);

        futureMap.put(requestId, future);
    }

    /**
     * Cancel a transformation request.
     * @param requestId unique id of a job to be cancelled
     */
    public void cancel(@NonNull String requestId) {
        Future<?> future = futureMap.get(requestId);
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * Release all resources, stop threads, etc. Transformer will be unusable after this method is called.
     */
    public void release() {
        executorService.shutdownNow();
    }

    /**
     * Estimates target size of a target video based on a provided target format. If no target audio format is specified,
     * uses 320 Kbps bitrate to estimate audio track size, if cannot extract audio bitrate. If track duration is not available,
     * maximum track duration will be used. If track bitrate cannot be extracted, track will not be used to estimate size.
     * @param inputUri {@link Uri} of a source video
     * @param targetVideoFormat video format for a transformation target
     * @param targetAudioFormat audio format for a transformation target
     * @return estimated size of a transcoding target video file in bytes, -1 otherwise
     */
    public long getEstimatedTargetVideoSize(@NonNull Uri inputUri,
                                            @NonNull MediaFormat targetVideoFormat,
                                            @Nullable MediaFormat targetAudioFormat) {
        try {
            MediaSource mediaSource = new MediaExtractorMediaSource(context, inputUri);
            return TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, targetAudioFormat);
        } catch (MediaSourceException ex) {
            return -1;
        }
    }

    @Nullable
    private MediaFormat createTargetMediaFormat(@NonNull MediaSource mediaSource,
                                                int sourceTrackIndex) {
        MediaFormat sourceMediaFormat = mediaSource.getTrackFormat(sourceTrackIndex);
        MediaFormat targetMediaFormat = null;

        String mimeType = null;
        if (sourceMediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            mimeType = sourceMediaFormat.getString(MediaFormat.KEY_MIME);
        }

        if (mimeType != null) {
            if (mimeType.startsWith("video")) {
                targetMediaFormat = MediaFormat.createVideoFormat(mimeType,
                                                                  sourceMediaFormat.getInteger(MediaFormat.KEY_WIDTH),
                                                                  sourceMediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
                int targetBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, sourceTrackIndex);
                targetMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate);

                int targetKeyFrameInterval = DEFAULT_KEY_FRAME_INTERVAL;
                if (sourceMediaFormat.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
                    targetKeyFrameInterval = sourceMediaFormat.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL);
                }
                targetMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, targetKeyFrameInterval);
            } else if (mimeType.startsWith("audio")) {
                targetMediaFormat = MediaFormat.createAudioFormat(mimeType,
                                                                  sourceMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                                                  sourceMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                targetMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, sourceMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE));
            }
        }

        return targetMediaFormat;
    }
}
