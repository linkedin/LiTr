/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.linkedin.android.litr.analytics.TransformationStatsCollector;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.exception.InsufficientDiskSpaceException;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.VideoRenderer;
import com.linkedin.android.litr.transcoder.TrackTranscoder;
import com.linkedin.android.litr.transcoder.TrackTranscoderFactory;
import com.linkedin.android.litr.utils.DiskUtil;
import com.linkedin.android.litr.utils.TranscoderUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.linkedin.android.litr.MediaTransformer.GRANULARITY_NONE;

class TransformationJob implements Runnable {

    private static final String TAG = TransformationJob.class.getSimpleName();

    private static final float DEFAULT_SIZE_PADDING = 0.10f; // 10% padding

    @VisibleForTesting List<MediaFormat> trackFormats;
    @VisibleForTesting List<TrackTranscoder> trackTranscoders;
    @VisibleForTesting float lastProgress;
    @VisibleForTesting int granularity;

    @VisibleForTesting MediaSource mediaSource;
    @VisibleForTesting MediaTarget mediaTarget;

    @VisibleForTesting TrackTranscoderFactory trackTranscoderFactory;
    @VisibleForTesting DiskUtil diskUtil;

    @VisibleForTesting TransformationStatsCollector statsCollector;

    private final String jobId;
    private final Decoder decoder;
    private final VideoRenderer videoRenderer;
    private final Encoder encoder;
    private final TransformationListener listener;
    private final MediaTransformer.ProgressHandler handler;
    private final MediaFormat targetVideoFormat;
    private final MediaFormat targetAudioFormat;

    TransformationJob(@NonNull String jobId,
                      @NonNull MediaSource mediaSource,
                      @NonNull Decoder decoder,
                      @NonNull VideoRenderer videoRenderer,
                      @NonNull Encoder encoder,
                      @NonNull MediaTarget mediaTarget,
                      @Nullable MediaFormat targetVideoFormat,
                      @Nullable MediaFormat targetAudioFormat,
                      @NonNull TransformationListener listener,
                      @IntRange(from = GRANULARITY_NONE) int granularity,
                      @NonNull MediaTransformer.ProgressHandler handler) {

        this.jobId = jobId;
        this.mediaSource = mediaSource;
        this.decoder = decoder;
        this.videoRenderer = videoRenderer;
        this.encoder = encoder;
        this.mediaTarget = mediaTarget;
        this.targetVideoFormat = targetVideoFormat;
        this.targetAudioFormat = targetAudioFormat;
        this.listener = listener;
        this.granularity = granularity;
        this.handler = handler;

        lastProgress = 0;

        trackTranscoderFactory = new TrackTranscoderFactory();
        diskUtil = new DiskUtil();
        statsCollector = new TransformationStatsCollector();
    }

    @Override
    public void run() {
        try {
            transform();
        } catch (RuntimeException e) {
            Log.e(TAG, "Transformation job error", e);
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                cancel();
            } else {
                error(e);
            }
        } catch (MediaTransformationException exception) {
            Log.e(TAG, "Transformation job error", exception);
            exception.setJobId(jobId);
            error(exception);
        }
    }

    @VisibleForTesting
    void transform() throws MediaTransformationException {
        loadTrackFormats();
        verifyAvailableDiskSpace();
        createTrackTranscoders();
        startTrackTranscoders();

        boolean completed = false;

        handler.onStarted(listener, jobId);
        lastProgress = 0;

        // process a frame from active track transcoder, until EoS (end of stream) is reached on each track
        do {
            completed = processNextFrame();

            if (Thread.interrupted()) {
                completed = false;
                cancel();
                break;
            }
        } while (!completed);

        release(completed);
    }

    @VisibleForTesting
    void cancel() {
        release(false);
        handler.onCancelled(listener, jobId, statsCollector.getStats());
    }

    @VisibleForTesting
    protected void error(@Nullable Throwable cause) {
        release(false);
        handler.onError(listener, jobId, cause, statsCollector.getStats());
    }

    @VisibleForTesting
    void loadTrackFormats() {
        int trackCount = mediaSource.getTrackCount();
        trackFormats = new ArrayList<>(trackCount);
        for (int track = 0; track < trackCount; track++) {
            MediaFormat mediaFormat = mediaSource.getTrackFormat(track);
            trackFormats.add(mediaFormat);
            statsCollector.addSourceTrack(mediaFormat);
        }
    }

    /**
     * This method estimates the file size of a target video and
     * checks if the device has sufficient disk space.
     * If disabled it logs an error, otherwise throws {@link InsufficientDiskSpaceException}.
     * Currently MediaMuxer can only handle one audio and video track each, it
     * drops all other tracks. This method assumes that the target has only
     * 2 tracks.
     */
    @VisibleForTesting
    void verifyAvailableDiskSpace() throws InsufficientDiskSpaceException {
        long estimatedFileSizeInBytes =
            TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, targetAudioFormat);
        long estimatedFileSizeInBytesAfterPadding =
            (long) (estimatedFileSizeInBytes * (1 + DEFAULT_SIZE_PADDING));

        long availableDiskSpaceInBytes = diskUtil.getAvailableDiskSpaceInDataDirectory();

        if (availableDiskSpaceInBytes != DiskUtil.FREE_DISK_SPACE_CHECK_FAILED
            && availableDiskSpaceInBytes < estimatedFileSizeInBytesAfterPadding) {
            throw new InsufficientDiskSpaceException(estimatedFileSizeInBytes, availableDiskSpaceInBytes);
        }
    }

    @VisibleForTesting
    void createTrackTranscoders() throws TrackTranscoderException {
        int trackCount = mediaSource.getTrackCount();
        trackTranscoders = new ArrayList<>(trackCount);

        if (trackCount < 1) {
            throw new TrackTranscoderException(TrackTranscoderException.Error.NO_TRACKS_FOUND);
        }

        for (int track = 0; track < trackCount; track++) {
            MediaFormat mediaFormat = mediaSource.getTrackFormat(track);
            TrackTranscoder trackTranscoder = trackTranscoderFactory.create(track,
                                                                            mediaFormat,
                                                                            mediaSource,
                                                                            decoder,
                                                                            videoRenderer,
                                                                            encoder,
                                                                            mediaTarget,
                                                                            targetVideoFormat,
                                                                            targetAudioFormat);
            trackTranscoders.add(trackTranscoder);
            statsCollector.setTrackCodecs(track, trackTranscoder.getDecoderName(), trackTranscoder.getEncoderName());
        }
    }

    @VisibleForTesting
    void startTrackTranscoders() throws TrackTranscoderException {
        for (TrackTranscoder trackTranscoder : trackTranscoders) {
            trackTranscoder.start();
        }
    }

    @VisibleForTesting
    boolean processNextFrame() throws TrackTranscoderException {
        boolean completed = true;

        for (int track = 0; track < trackTranscoders.size(); track++) {
            TrackTranscoder trackTranscoder = trackTranscoders.get(track);

            long frameStartTime = System.currentTimeMillis();
            int result = trackTranscoder.processNextFrame();
            completed &= result == TrackTranscoder.RESULT_EOS_REACHED;

            statsCollector.increaseTrackProcessingDuration(track, System.currentTimeMillis() - frameStartTime);
        }

        float totalProgress = 0;
        for (TrackTranscoder trackTranscoder : trackTranscoders) {
            totalProgress += trackTranscoder.getProgress();
        }
        totalProgress /= trackTranscoders.size();

        if ((granularity == GRANULARITY_NONE && totalProgress != lastProgress)
            || (granularity != GRANULARITY_NONE && totalProgress >= lastProgress + 1.0f / granularity)) {
            handler.onProgress(listener, jobId, totalProgress);
            lastProgress = totalProgress;
        }

        return completed;
    }

    @VisibleForTesting
    void release(boolean success) {
        for (int track = 0; track < trackTranscoders.size(); track++) {
            TrackTranscoder trackTranscoder = trackTranscoders.get(track);
            trackTranscoder.stop();
            statsCollector.setTargetFormat(track, trackTranscoder.getTargetMediaFormat());
        }

        mediaSource.release();
        mediaTarget.release();

        String outputFilePath = mediaTarget.getOutputFilePath();

        if (success) {
            handler.onCompleted(listener, jobId, statsCollector.getStats());
        } else {
            deleteOutputFile(outputFilePath);
        }
    }

    @VisibleForTesting
    boolean deleteOutputFile(String outputFilePath) {
        if (TextUtils.isEmpty(outputFilePath)) {
            return false;
        }

        File outputFile = new File(outputFilePath);
        return outputFile.delete();
    }
}
