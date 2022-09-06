/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import static com.linkedin.android.litr.transcoder.TrackTranscoder.RESULT_EOS_REACHED;
import static junit.framework.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.linkedin.android.litr.analytics.TrackTransformationInfo;
import com.linkedin.android.litr.analytics.TransformationStatsCollector;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.exception.InsufficientDiskSpaceException;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.Renderer;
import com.linkedin.android.litr.transcoder.PassthroughTranscoder;
import com.linkedin.android.litr.transcoder.TrackTranscoder;
import com.linkedin.android.litr.transcoder.TrackTranscoderFactory;
import com.linkedin.android.litr.transcoder.VideoTrackTranscoder;
import com.linkedin.android.litr.utils.DiskUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TransformationJobShould {
    private static final String JOB_ID = "42";
    private static final int MAX_PROGRESS = 1;

    private static final int SOURCE_TRACK_VIDEO = 0;
    private static final int SOURCE_TRACK_AUDIO = 1;

    private static final String OUTPUT_FILE_PATH = "/some/temp/file.mp4";

    @Mock private MediaFormat targetVideoFormat;
    @Mock private MediaFormat sourceVideoFormat;
    @Mock private MediaFormat sourceAudioFormat;
    @Mock private MediaFormat targetAudioFormat;
    @Mock private MarshallingTransformationListener marshallingTransformationListener;


    @Mock private MediaSource mediaSource;
    @Mock private Decoder decoder;
    @Mock private Renderer renderer;
    @Mock private Encoder encoder;
    @Mock private MediaTarget mediaTarget;
    @Mock private VideoTrackTranscoder videoTrackTranscoder;
    @Mock private PassthroughTranscoder audioTrackTranscoder;
    @Mock private TrackTranscoderFactory trackTranscoderFactory;
    @Mock private DiskUtil diskUtil;
    @Mock private TransformationStatsCollector statsCollector;

    @Captor private ArgumentCaptor<List<TrackTransformationInfo>> trackTransformationInfosCaptor;

    private List<TrackTransform> trackTransforms;

    private TransformationJob transformationJob;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(OUTPUT_FILE_PATH).when(mediaTarget).getOutputFilePath();

        doReturn(2).when(mediaSource).getTrackCount();

        doReturn(sourceVideoFormat).when(mediaSource).getTrackFormat(0);
        doReturn(sourceAudioFormat).when(mediaSource).getTrackFormat(1);

        when(mediaSource.getSelection()).thenReturn(new MediaRange(0, Long.MAX_VALUE));

        doReturn("video/avc").when(sourceVideoFormat).getString(MediaFormat.KEY_MIME);
        doReturn("audio/aac").when(sourceAudioFormat).getString(MediaFormat.KEY_MIME);

        when(sourceVideoFormat.containsKey(MediaFormat.KEY_MIME)).thenReturn(true);
        when(sourceVideoFormat.getString(MediaFormat.KEY_MIME)).thenReturn("video/avc");
        when(sourceVideoFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(sourceVideoFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(120000000L);
        when(sourceAudioFormat.containsKey(MediaFormat.KEY_MIME)).thenReturn(true);
        when(sourceAudioFormat.getString(MediaFormat.KEY_MIME)).thenReturn("audio/mp4a-latm");
        when(sourceAudioFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(sourceAudioFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(60000000L);
        when(diskUtil.getAvailableDiskSpaceInDataDirectory()).thenReturn(1000000000L);
        when(targetVideoFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(6 * 1024 * 1024);

        doReturn(videoTrackTranscoder)
                .when(trackTranscoderFactory)
                .create(SOURCE_TRACK_VIDEO,
                        SOURCE_TRACK_VIDEO,
                        mediaSource,
                        decoder,
                        renderer,
                        encoder,
                        mediaTarget,
                        targetVideoFormat);


        doReturn(audioTrackTranscoder)
                .when(trackTranscoderFactory)
                .create(SOURCE_TRACK_AUDIO,
                        SOURCE_TRACK_AUDIO,
                        mediaSource,
                        decoder,
                        null,
                        encoder,
                        mediaTarget,
                        targetAudioFormat);

        doReturn(RESULT_EOS_REACHED).when(videoTrackTranscoder).processNextFrame();
        doReturn(RESULT_EOS_REACHED).when(audioTrackTranscoder).processNextFrame();

        TrackTransform videoTrackTransform = new TrackTransform.Builder(mediaSource, SOURCE_TRACK_VIDEO, mediaTarget)
            .setDecoder(decoder)
            .setRenderer(renderer)
            .setEncoder(encoder)
            .setTargetTrack(SOURCE_TRACK_VIDEO)
            .setTargetFormat(targetVideoFormat)
            .build();

        TrackTransform audioTrackTransform = new TrackTransform.Builder(mediaSource, SOURCE_TRACK_AUDIO, mediaTarget)
            .setDecoder(decoder)
            .setEncoder(encoder)
            .setTargetTrack(SOURCE_TRACK_AUDIO)
            .setTargetFormat(targetAudioFormat)
            .build();

        trackTransforms = new ArrayList<>(2);
        trackTransforms.add(videoTrackTransform);
        trackTransforms.add(audioTrackTransform);

        transformationJob = spy(new TransformationJob(JOB_ID,
                                                      trackTransforms,
                                                      MAX_PROGRESS,
                                                      marshallingTransformationListener));
        transformationJob.trackTranscoderFactory = trackTranscoderFactory;
        transformationJob.diskUtil = diskUtil;
        transformationJob.statsCollector = statsCollector;
    }

    @Test
    public void notProduceErrorWhenNoTransformExceptions() throws Exception {
        transformationJob.run();

        verify(transformationJob).transform();
        verify(transformationJob, never()).cancel();
        verify(transformationJob, never()).error(nullable(Throwable.class));
    }

    @Test
    public void cancelWhenJobIsInterrupted() throws Exception {
        InterruptedException interruptedException = new InterruptedException("Job is cancelled");
        RuntimeException exception = new RuntimeException("Thread is interrupted", interruptedException);
        doThrow(exception).when(transformationJob).processNextFrame();

        transformationJob.run();

        verify(transformationJob).transform();
        verify(transformationJob).cancel();
    }

    @Test
    public void reportErrorWhenTransformExceptionOccurs() throws Exception {
        Throwable cause = new Throwable("Some cause");
        RuntimeException exception = new RuntimeException("Thread is interrupted", cause);
        doThrow(exception).when(transformationJob).processNextFrame();

        transformationJob.run();

        verify(transformationJob).transform();
        verify(transformationJob).error(exception);
    }

    @Test
    public void transformWhenNoErrors() throws Exception {
        transformationJob.transform();

        verify(marshallingTransformationListener).onCompleted(eq(JOB_ID), ArgumentMatchers.<TrackTransformationInfo>anyList());
        verify(statsCollector).addSourceTrack(sourceVideoFormat);
        verify(statsCollector).addSourceTrack(sourceAudioFormat);
    }

    @Test(expected = InsufficientDiskSpaceException.class)
    public void reportErrorWhenNotEnoughSpace() throws Exception {
        when(sourceVideoFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(120000000L);
        when(sourceAudioFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(60000000L);
        when(diskUtil.getAvailableDiskSpaceInDataDirectory()).thenReturn(1000000L);

        transformationJob.transform();
    }

    @Test
    public void notCreateTrackTranscodersWhenNoTracksAreFound() {
        try {
            TransformationJob transformationJob = new TransformationJob(JOB_ID,
                                                                        Collections.<TrackTransform>emptyList(),
                                                                        MAX_PROGRESS,
                                                                        marshallingTransformationListener);
            transformationJob.createTrackTranscoders();
        } catch (TrackTranscoderException e) {
            assertThat(e.getError(), is(TrackTranscoderException.Error.NO_TRACKS_FOUND));
        }
    }

    @Test
    public void createTrackTranscodersWhenTracksAreFound() throws Exception {
        doReturn(videoTrackTranscoder).when(trackTranscoderFactory).create(SOURCE_TRACK_VIDEO,
                                                                           SOURCE_TRACK_VIDEO,
                                                                           mediaSource,
                                                                           decoder,
                                                                           renderer,
                                                                           encoder,
                                                                           mediaTarget,
                                                                           targetVideoFormat);
        transformationJob.createTrackTranscoders();

        verify(statsCollector).setTrackCodecs(0, videoTrackTranscoder.getDecoderName(), videoTrackTranscoder.getDecoderName());
    }

    @Test
    public void notStartWhenNotAllTrackTranscodersStart() throws Exception {
        doThrow(new TrackTranscoderException(TrackTranscoderException.Error.INTERNAL_CODEC_ERROR)).when(videoTrackTranscoder).start();
        try{
            transformationJob.transform();
        } catch (TrackTranscoderException e) {
            assertThat(e.getError(), is(TrackTranscoderException.Error.INTERNAL_CODEC_ERROR));
            verify(videoTrackTranscoder).start();
            verify(audioTrackTranscoder, never()).start();
        }
    }

    @Test
    public void cleanUpWhenNotAllTrackTranscodersStart() throws Exception {

        doThrow(new TrackTranscoderException(TrackTranscoderException.Error.INTERNAL_CODEC_ERROR)).when(audioTrackTranscoder).start();

        try{
            transformationJob.transform();
        } catch (TrackTranscoderException e) {
            assertThat(e.getError(), is(TrackTranscoderException.Error.INTERNAL_CODEC_ERROR));
            verify(videoTrackTranscoder).start();
            verify(audioTrackTranscoder).start();

            // TODO verify error gets called when exception is thrown in TransformerJobShould
            transformationJob.error(e);
            verify(videoTrackTranscoder).stop();
            verify(audioTrackTranscoder).stop();
            verify(mediaSource).release();
            verify(mediaTarget).release();
        }
    }


    @Test
    public void completeWhenAllTrackTranscodersReachEos() throws Exception {
        doReturn(TrackTranscoder.RESULT_EOS_REACHED).when(videoTrackTranscoder).processNextFrame();
        doReturn(TrackTranscoder.RESULT_EOS_REACHED).when(audioTrackTranscoder).processNextFrame();
        doReturn(1.0f).when(videoTrackTranscoder).getProgress();
        doReturn(1.0f).when(audioTrackTranscoder).getProgress();

        loadTrackTranscoders();
        boolean completed = transformationJob.processNextFrame();

        verify(marshallingTransformationListener).onProgress(JOB_ID, 1.0f);
        assertTrue(completed);
        verify(statsCollector).increaseTrackProcessingDuration(eq(0), anyLong());
        verify(statsCollector).increaseTrackProcessingDuration(eq(1), anyLong());
    }

    @Test
    public void processWhenTracksAreStillProcessing() throws Exception {
        doReturn(TrackTranscoder.RESULT_EOS_REACHED).when(videoTrackTranscoder).processNextFrame();
        doReturn(TrackTranscoder.RESULT_FRAME_PROCESSED).when(audioTrackTranscoder).processNextFrame();
        doReturn(1.0f).when(videoTrackTranscoder).getProgress();
        doReturn(0.5f).when(audioTrackTranscoder).getProgress();
        transformationJob.granularity = MediaTransformer.GRANULARITY_NONE;
        transformationJob.lastProgress = 0;

        loadTrackTranscoders();

        boolean completed = transformationJob.processNextFrame();

        verify(marshallingTransformationListener).onProgress(JOB_ID, 0.75f);
        assertFalse(completed);
        assertThat(transformationJob.lastProgress, is(0.75f));
        verify(statsCollector).increaseTrackProcessingDuration(eq(0), anyLong());
        verify(statsCollector).increaseTrackProcessingDuration(eq(1), anyLong());
    }

    @Test
    public void notReportProgressWhenSmallerThanGranularity() throws Exception {
        transformationJob.granularity = 5; // increments of 0.2
        transformationJob.lastProgress = 0.2f;

        float currentProgress = 0.25f; // difference with last progress is 0.05 which is below granularity

        doReturn(TrackTranscoder.RESULT_FRAME_PROCESSED).when(videoTrackTranscoder).processNextFrame();
        doReturn(TrackTranscoder.RESULT_FRAME_PROCESSED).when(audioTrackTranscoder).processNextFrame();
        doReturn(currentProgress).when(videoTrackTranscoder).getProgress();
        doReturn(currentProgress).when(audioTrackTranscoder).getProgress();

        loadTrackTranscoders();

        transformationJob.processNextFrame();

        verify(marshallingTransformationListener, never()).onProgress(anyString(), anyFloat());
        verify(statsCollector).increaseTrackProcessingDuration(eq(0), anyLong());
        verify(statsCollector).increaseTrackProcessingDuration(eq(1), anyLong());
    }

    @Test
    public void reportProgressWhenGreaterThanGranularity() throws Exception {
        transformationJob.granularity = 5; // increments of 0.2
        transformationJob.lastProgress = 0.2f;

        float currentProgress = 0.45f; // difference with last progress is 0.05 which is below granularity

        doReturn(TrackTranscoder.RESULT_FRAME_PROCESSED).when(videoTrackTranscoder).processNextFrame();
        doReturn(TrackTranscoder.RESULT_FRAME_PROCESSED).when(audioTrackTranscoder).processNextFrame();
        doReturn(currentProgress).when(videoTrackTranscoder).getProgress();
        doReturn(currentProgress).when(audioTrackTranscoder).getProgress();

        loadTrackTranscoders();

        transformationJob.processNextFrame();

        verify(marshallingTransformationListener).onProgress(JOB_ID, currentProgress);
        verify(statsCollector).increaseTrackProcessingDuration(eq(0), anyLong());
        verify(statsCollector).increaseTrackProcessingDuration(eq(1), anyLong());
    }

    @Test
    public void stopTranscodersAndCleanupWhenReleasing() {
        loadTrackTranscoders();

        transformationJob.release(true);

        verify(videoTrackTranscoder).stop();
        verify(audioTrackTranscoder).stop();
        verify(mediaSource).release();
        verify(mediaTarget).release();
        verify(marshallingTransformationListener).onCompleted(eq(JOB_ID), ArgumentMatchers.<TrackTransformationInfo>anyList());
        verify(statsCollector).setTargetFormat(0, videoTrackTranscoder.getTargetMediaFormat());
        verify(statsCollector).setTargetFormat(1, audioTrackTranscoder.getTargetMediaFormat());
        verify(statsCollector).getStats();
    }

    @Test
    public void reportErrorAndReleaseWhenError() {
        loadTrackTranscoders();

        TrackTranscoderException exception = new TrackTranscoderException(TrackTranscoderException.Error.CODEC_IN_RELEASED_STATE);
        transformationJob.error(exception);

        verify(marshallingTransformationListener).onError(eq(JOB_ID), eq(exception), ArgumentMatchers.<TrackTransformationInfo>anyList());
        verify(statsCollector).setTargetFormat(0, videoTrackTranscoder.getTargetMediaFormat());
        verify(statsCollector).setTargetFormat(1, audioTrackTranscoder.getTargetMediaFormat());
        verify(statsCollector).getStats();
    }

    @Test
    public void reportErrorAndReleaseWhenCancelling() {
        loadTrackTranscoders();
        List<TrackTransformationInfo> trackTransformationInfos = new ArrayList<>();
        when(statsCollector.getStats()).thenReturn(trackTransformationInfos);

        transformationJob.cancel();

        verify(marshallingTransformationListener).onCancelled(eq(JOB_ID), trackTransformationInfosCaptor.capture());
        verify(statsCollector).setTargetFormat(0, videoTrackTranscoder.getTargetMediaFormat());
        verify(statsCollector).setTargetFormat(1, audioTrackTranscoder.getTargetMediaFormat());
        assertThat(trackTransformationInfosCaptor.getValue(), is(trackTransformationInfos));
    }

    @Test
    public void seekToMediaRangeStartWhenTransforming() {
        long trimStart = TimeUnit.SECONDS.toMicros(42);
        MediaRange mediaRange = new MediaRange(trimStart, Long.MAX_VALUE);
        when(mediaSource.getSelection()).thenReturn(mediaRange);

        transformationJob.run();

        verify(mediaSource, times(2)).seekTo(trimStart, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
    }

    private void loadTrackTranscoders() {
        transformationJob.trackTranscoders = new ArrayList<>();
        transformationJob.trackTranscoders.add(videoTrackTranscoder);
        transformationJob.trackTranscoders.add(audioTrackTranscoder);
    }
}
