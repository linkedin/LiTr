/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.linkedin.android.litr.analytics.TransformationStatsCollector;
import com.linkedin.android.litr.exception.InsufficientDiskSpaceException;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.exception.TrackTranscoderException;
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

    @VisibleForTesting List<TrackTranscoder> trackTranscoders;
    @VisibleForTesting float lastProgress;
    @VisibleForTesting int granularity;

    @VisibleForTesting TrackTranscoderFactory trackTranscoderFactory;
    @VisibleForTesting DiskUtil diskUtil;

    @VisibleForTesting TransformationStatsCollector statsCollector;

    private final List<TrackTransform> trackTransforms;

    private final String jobId;
    private final TransformationListener listener;
    private final MediaTransformer.ProgressHandler handler;

    TransformationJob(@NonNull String jobId,
                      List<TrackTransform> trackTransforms,
                      @NonNull TransformationListener listener,
                      @IntRange(from = GRANULARITY_NONE) int granularity,
                      @NonNull MediaTransformer.ProgressHandler handler) {

        this.jobId = jobId;
        this.trackTransforms = trackTransforms;
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
        initStatsCollector();
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
    void initStatsCollector() {
        // TODO modify TrackTransformationInfo to report muxing/demuxing and different media sources/targets
        for (TrackTransform trackTransform : trackTransforms) {
            statsCollector.addSourceTrack(trackTransform.getMediaSource().getTrackFormat(trackTransform.getSourceTrack()));
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
            TranscoderUtils.getEstimatedTargetFileSize(trackTransforms);
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
        int trackCount = trackTransforms.size();
        trackTranscoders = new ArrayList<>(trackCount);

        if (trackCount < 1) {
            throw new TrackTranscoderException(TrackTranscoderException.Error.NO_TRACKS_FOUND);
        }

        for (int track = 0; track < trackCount; track++) {
            TrackTransform trackTransform = trackTransforms.get(track);

            TrackTranscoder trackTranscoder = trackTranscoderFactory.create(track,
                                                                            trackTransform.getMediaSource(),
                                                                            trackTransform.getDecoder(),
                                                                            trackTransform.getRenderer(),
                                                                            trackTransform.getEncoder(),
                                                                            trackTransform.getMediaTarget(),
                                                                            trackTransform.getTargetFormat());
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

        for (TrackTransform trackTransform : trackTransforms) {
            trackTransform.getMediaSource().release();
            trackTransform.getMediaTarget().release();

            String outputFilePath = trackTransform.getMediaTarget().getOutputFilePath();

            if (success) {
                handler.onCompleted(listener, jobId, statsCollector.getStats());
            } else {
                deleteOutputFile(outputFilePath);
            }
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
