/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.transcoder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PassthroughTranscoderShould {
    private static final int SOURCE_TRACK = 1;
    private static final int TARGET_TRACK = 1;

    private static final long DURATION = 42;
    private static final long SAMPLE_TIME = 21;
    private static final int BUFFER_SIZE = 512;

    @Mock private MediaSource mediaSource;
    @Mock private MediaTarget mediaTarget;
    @Mock private MediaCodec.BufferInfo outputBufferInfo;
    @Mock private ByteBuffer outputBuffer;

    private MediaFormat sourceMediaFormat;

    private PassthroughTranscoder passthroughTranscoder;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        sourceMediaFormat = new MediaFormat();
        sourceMediaFormat.setLong(MediaFormat.KEY_DURATION, DURATION);
        when(mediaSource.getTrackFormat(SOURCE_TRACK)).thenReturn(sourceMediaFormat);

        MediaRange mediaRange = new MediaRange(0, Long.MAX_VALUE);
        when(mediaSource.getSelection()).thenReturn(mediaRange);

        passthroughTranscoder = spy(new PassthroughTranscoder(mediaSource, SOURCE_TRACK, mediaTarget, TARGET_TRACK));
        passthroughTranscoder.start();

        passthroughTranscoder.outputBufferInfo = outputBufferInfo;
        passthroughTranscoder.outputBuffer = outputBuffer;
    }

    @Test
    public void doNothingWhenEosReached() {
        passthroughTranscoder.lastResult = TrackTranscoder.RESULT_EOS_REACHED;

        int result = passthroughTranscoder.processNextFrame();

        verify(mediaSource, never()).readSampleData(any(ByteBuffer.class), anyInt());

        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    @Test
    public void addTargetTrackBeforeProcessingFrames() {
        doReturn(TARGET_TRACK).when(mediaTarget).addTrack(any(MediaFormat.class), anyInt());
        doReturn(TARGET_TRACK).when(mediaTarget).addTrack(any(MediaFormat.class), anyInt());

        int result = passthroughTranscoder.processNextFrame();

        verify(mediaTarget).addTrack(sourceMediaFormat, SOURCE_TRACK);
        assertThat(result, is(TrackTranscoder.RESULT_OUTPUT_MEDIA_FORMAT_CHANGED));
        assertThat(passthroughTranscoder.lastResult, is(TrackTranscoder.RESULT_OUTPUT_MEDIA_FORMAT_CHANGED));
    }

    @Test
    public void doNothingWhenOtherTrackIsSelected() {
        passthroughTranscoder.sourceTrack = 0;
        passthroughTranscoder.targetTrack = 0;
        passthroughTranscoder.targetTrackAdded = true;
        doReturn(1).when(mediaSource).getSampleTrackIndex();

        int result = passthroughTranscoder.processNextFrame();

        verify(mediaSource, never()).readSampleData(any(ByteBuffer.class), anyInt());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void writeFrameWhenInputDataIsAvailable() {
        passthroughTranscoder.sourceTrack = 0;
        passthroughTranscoder.targetTrack = 0;
        passthroughTranscoder.duration = DURATION;
        passthroughTranscoder.targetTrackAdded = true;
        int outputFlags = 0;

        doReturn(0).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_SIZE).when(mediaSource).readSampleData(outputBuffer, 0);
        doReturn(SAMPLE_TIME).when(mediaSource).getSampleTime();
        doReturn(outputFlags).when(mediaSource).getSampleFlags();

        int result = passthroughTranscoder.processNextFrame();

        verify(outputBufferInfo).set(0, BUFFER_SIZE, SAMPLE_TIME, outputFlags);
        verify(mediaSource).advance();
        verify(mediaTarget).writeSampleData(0, outputBuffer, outputBufferInfo);

        assertThat(passthroughTranscoder.progress, is((float) SAMPLE_TIME / DURATION));
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
        assertThat(passthroughTranscoder.lastResult, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void writeIFrameWhenInputDataIsAvailable() {
        passthroughTranscoder.sourceTrack = 0;
        passthroughTranscoder.targetTrack = 0;
        passthroughTranscoder.duration = DURATION;
        passthroughTranscoder.targetTrackAdded = true;
        int outputFlags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;

        doReturn(0).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_SIZE).when(mediaSource).readSampleData(outputBuffer, 0);
        doReturn(SAMPLE_TIME).when(mediaSource).getSampleTime();
        doReturn(outputFlags).when(mediaSource).getSampleFlags();

        int result = passthroughTranscoder.processNextFrame();

        verify(outputBufferInfo).set(0, BUFFER_SIZE, SAMPLE_TIME, outputFlags);
        verify(mediaSource).advance();
        verify(mediaTarget).writeSampleData(0, outputBuffer, outputBufferInfo);

        assertThat(passthroughTranscoder.progress, is((float) SAMPLE_TIME / DURATION));
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
        assertThat(passthroughTranscoder.lastResult, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void finishWritingWhenInputDataIsNotAvailable() {
        passthroughTranscoder.sourceTrack = 0;
        passthroughTranscoder.targetTrack = 0;
        passthroughTranscoder.duration = DURATION;
        passthroughTranscoder.targetTrackAdded = true;

        doReturn(0).when(mediaSource).getSampleTrackIndex();
        doReturn(0).when(mediaSource).readSampleData(outputBuffer, 0);

        int result = passthroughTranscoder.processNextFrame();

        verify(mediaSource, never()).advance();
        verify(mediaTarget, never()).writeSampleData(0, outputBuffer, outputBufferInfo);

        assertThat(passthroughTranscoder.progress, is(1.0f));
        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
        assertThat(passthroughTranscoder.lastResult, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    @Test
    public void releaseBufferWhenStopped() {
        // since we cannot verify that final method clear() was called, let's test for its consequences
        ByteBuffer outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        outputBuffer.put((byte) 1);
        passthroughTranscoder.outputBuffer = outputBuffer;

        passthroughTranscoder.stop();

        assertThat(passthroughTranscoder.outputBuffer, is(nullValue()));
        assertThat(outputBuffer.position(), is(0));
    }

    @Test
    public void keepProgressAtZeroWhenDurationIsNotAvailable() {
        passthroughTranscoder.sourceTrack = 0;
        passthroughTranscoder.targetTrack = 0;
        passthroughTranscoder.duration = 0;
        passthroughTranscoder.targetTrackAdded = true;
        int outputFlags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;

        doReturn(0).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_SIZE).when(mediaSource).readSampleData(outputBuffer, 0);
        doReturn(SAMPLE_TIME).when(mediaSource).getSampleTime();
        doReturn(outputFlags).when(mediaSource).getSampleFlags();

        int result = passthroughTranscoder.processNextFrame();

        verify(outputBufferInfo).set(0, BUFFER_SIZE, SAMPLE_TIME, outputFlags);
        verify(mediaSource).advance();
        verify(mediaTarget).writeSampleData(0, outputBuffer, outputBufferInfo);

        assertThat(passthroughTranscoder.progress, is(0f));
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
        assertThat(passthroughTranscoder.lastResult, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }
}