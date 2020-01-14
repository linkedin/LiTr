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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;
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
import com.linkedin.android.litr.render.VideoRenderer;
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
    private ProgressHandler handler;

    private Map<String, Future<?>> futureMap;

    /**
     * Instantiate MediaTransformer
     * @param context context with access to source and target URIs and other resources
     */
    public MediaTransformer(@NonNull Context context) {
        this.context = context.getApplicationContext();

        futureMap = new HashMap<>(DEFAULT_FUTURE_MAP_SIZE);
        handler = new ProgressHandler(Looper.getMainLooper(), futureMap);
        executorService = Executors.newSingleThreadExecutor();
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
     */
    public void transform(@NonNull String requestId,
                          @NonNull Uri inputUri,
                          @NonNull String outputFilePath,
                          @Nullable MediaFormat targetVideoFormat,
                          @Nullable MediaFormat targetAudioFormat,
                          @NonNull TransformationListener listener,
                          @IntRange(from = GRANULARITY_NONE) int granularity,
                          @Nullable List<GlFilter> filters) {
        try {
            MediaSource mediaSource = new MediaExtractorMediaSource(context, inputUri);
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(outputFilePath,
                                                                mediaSource.getTrackCount(),
                                                                mediaSource.getOrientationHint(),
                                                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            VideoRenderer renderer = new GlVideoRenderer(filters);
            Decoder decoder = new MediaCodecDecoder();
            Encoder encoder = new MediaCodecEncoder();
            transform(requestId,
                      mediaSource,
                      decoder,
                      renderer,
                      encoder,
                      mediaTarget,
                      targetVideoFormat,
                      targetAudioFormat,
                      listener,
                      granularity);
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
     * @param decoder {@link Decoder} to decode input frames
     * @param videoRenderer {@link VideoRenderer} to draw (with optional filters) decoder's output frame onto encoder's input frame
     * @param encoder {@link Encoder} to encode output frames into target format
     * @param mediaTarget {@link MediaTarget} to write/mux output frames
     * @param targetVideoFormat target format parameters for video track(s), null to keep them as is
     * @param targetAudioFormat target format parameters for audio track(s), null to keep them as is
     * @param listener {@link TransformationListener} implementation, to get updates on transformation status/result/progress
     * @param granularity progress reporting granularity. NO_GRANULARITY for per-frame progress reporting,
     *                    or positive integer value for number of times transformation progress should be reported
     */
    public void transform(@NonNull String requestId,
                          @NonNull MediaSource mediaSource,
                          @NonNull Decoder decoder,
                          @NonNull VideoRenderer videoRenderer,
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
                trackTransformBuilder.setDecoder(decoder)
                                     .setEncoder(encoder)
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

                trackTransforms.remove(trackIndex);
                trackTransforms.add(trackIndex, updatedTrackTransform);
            }
        }

        TransformationJob transformationJob = new TransformationJob(requestId,
                                                                    trackTransforms,
                                                                    listener,
                                                                    granularity,
                                                                    handler);
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
                int targetBitrate;
                if (sourceMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    targetBitrate = sourceMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                } else {
                    targetBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, sourceTrackIndex);
                }
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

    static class ProgressHandler extends Handler {
        private static final int EVENT_STARTED = 0;
        private static final int EVENT_COMPLETED = 1;
        private static final int EVENT_ERROR = 2;
        private static final int EVENT_PROGRESS = 3;
        private static final int EVENT_CANCELLED = 4;

        private static final String KEY_JOB_ID = "jobId";
        private static final String KEY_PROGRESS = "progress";
        private static final String KEY_THROWABLE = "throwable";

        private final Map<String, Future<?>> futureMap;

        private Bundle data = new Bundle();

        // we define these as member variables to prevent frequent object creation
        // however, we should revisit this when (if) we implement thread pool with multiple worker threads
        private TransformationListener listener;
        private List<TrackTransformationInfo> trackTransformationInfos;

        private ProgressHandler(Looper mainLooper, @NonNull Map<String, Future<?>> futureMap) {
            super(mainLooper);
            this.futureMap = futureMap;
        }

        @Override
        public void handleMessage(@NonNull Message message) {
            if (listener == null) {
                // client is not interested in knowing the result of transcoding. Strange, but possible.
                return;
            }

            Bundle data = message.getData();
            String jobId = data.getString(KEY_JOB_ID);
            if (jobId == null) {
                throw new IllegalArgumentException("Handler message doesn't contain an id!");
            }
            switch (message.what) {
                case EVENT_STARTED: {
                    listener.onStarted(jobId);
                    break;
                }
                case EVENT_COMPLETED: {
                    listener.onCompleted(jobId, trackTransformationInfos);
                    break;
                }
                case EVENT_CANCELLED: {
                    listener.onCancelled(jobId, trackTransformationInfos);
                    break;
                }
                case EVENT_ERROR: {
                    Throwable cause = (Throwable) data.getSerializable(KEY_THROWABLE);
                    listener.onError(jobId, cause, trackTransformationInfos);
                    break;
                }
                case EVENT_PROGRESS: {
                    float progress = data.getFloat(KEY_PROGRESS);
                    listener.onProgress(jobId, progress);
                    break;
                }
                default:
                    Log.e(TAG, "Unknown event received: " + message.what);
            }
        }

        void onStarted(@NonNull final TransformationListener listener,
                       @NonNull String jobId) {
            this.listener = listener;
            this.trackTransformationInfos = null;

            Message msg = Message.obtain(this, EVENT_STARTED);
            data.putString(KEY_JOB_ID, jobId);
            msg.setData(data);
            msg.sendToTarget();
        }

        void onCompleted(@NonNull final TransformationListener listener,
                         @NonNull String jobId,
                         @NonNull final List<TrackTransformationInfo> trackTransformationInfos) {
            futureMap.remove(jobId);

            this.listener = listener;
            this.trackTransformationInfos = trackTransformationInfos;

            Message msg = Message.obtain(this, EVENT_COMPLETED);
            data.putString(KEY_JOB_ID, jobId);
            msg.setData(data);
            msg.sendToTarget();
        }

        void onCancelled(@NonNull final TransformationListener listener,
                         @NonNull final String jobId,
                         @NonNull final List<TrackTransformationInfo> trackTransformationInfos) {
            futureMap.remove(jobId);

            this.listener = listener;
            this.trackTransformationInfos = trackTransformationInfos;

            Message msg = Message.obtain(this, EVENT_CANCELLED);
            data.putString(KEY_JOB_ID, jobId);
            msg.setData(data);
            msg.sendToTarget();
        }

        void onError(@NonNull final TransformationListener listener,
                     @NonNull final String jobId,
                     @Nullable final Throwable cause,
                     @NonNull final List<TrackTransformationInfo> trackTransformationInfos) {
            futureMap.remove(jobId);

            this.listener = listener;
            this.trackTransformationInfos = trackTransformationInfos;

            Message msg = Message.obtain(this, EVENT_ERROR);
            data.putString(KEY_JOB_ID, jobId);
            data.putSerializable(KEY_THROWABLE, cause);
            msg.setData(data);
            msg.sendToTarget();
        }

        void onProgress(@NonNull final TransformationListener listener,
                        @NonNull String jobId,
                        float progress) {
            this.listener = listener;
            this.trackTransformationInfos = null;

            Message msg = Message.obtain(this, EVENT_PROGRESS);
            data.putString(KEY_JOB_ID, jobId);
            data.putFloat(KEY_PROGRESS, progress);
            msg.setData(data);
            msg.sendToTarget();
        }
    }
}
