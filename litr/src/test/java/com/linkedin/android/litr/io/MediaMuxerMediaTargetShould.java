/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class MediaMuxerMediaTargetShould {
    private static final String OUTPUT_FILE_PATH = "/path/to/output/file.mp4";
    private static final int TRACK_COUNT = 2;
    private static final int ORIENTATION_HINT = 0;

    private static final int TRACK_VIDEO = 0;
    private static final int TRACK_AUDIO = 1;

    private static final int BUFFER_SIZE = 42;
    private static final long PRESENTATION_TIME = 17;
    private static final int SAMPLE_FLAGS = 0;

    @Mock private MediaMuxer mediaMuxer;

    private MediaMuxerMediaTarget mediaMuxerWrapper;

    private ByteBuffer byteBuffer;
    private MediaCodec.BufferInfo bufferInfo;

    private MediaFormat videoMediaFormat;
    private MediaFormat audioMediaFormat;

    @Captor private ArgumentCaptor<Integer> trackCaptor;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        mediaMuxerWrapper = spy(new MediaMuxerMediaTarget(OUTPUT_FILE_PATH, TRACK_COUNT, ORIENTATION_HINT, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4));
        mediaMuxerWrapper.mediaMuxer = mediaMuxer;

        byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.set(0, BUFFER_SIZE, PRESENTATION_TIME, SAMPLE_FLAGS);

        videoMediaFormat = new MediaFormat();
        audioMediaFormat = new MediaFormat();
    }

    @Test
    public void writeMediaSampleWhenStarted() {
        mediaMuxerWrapper.isStarted = true;

        mediaMuxerWrapper.writeSampleData(TRACK_VIDEO, byteBuffer, bufferInfo);

        verify(mediaMuxer).writeSampleData(TRACK_VIDEO, byteBuffer, bufferInfo);
        assertTrue(mediaMuxerWrapper.queue.isEmpty());
    }

    @Test
    public void queueMediaSampleWhenNotStarted() {
        mediaMuxerWrapper.isStarted = false;

        mediaMuxerWrapper.writeSampleData(TRACK_VIDEO, byteBuffer, bufferInfo);

        verify(mediaMuxer, never()).writeSampleData(anyInt(), any(ByteBuffer.class), any(MediaCodec.BufferInfo.class));
        assertFalse(mediaMuxerWrapper.queue.isEmpty());
    }

    @Test
    public void notAddTrackWhenNotAllTracksAreAdded() {
        doReturn(TRACK_VIDEO).when(mediaMuxer).addTrack(videoMediaFormat);

        mediaMuxerWrapper.addTrack(videoMediaFormat, TRACK_VIDEO);

        verify(mediaMuxer, never()).addTrack(videoMediaFormat);
    }

    @Test
    public void startMuxerAndWriteMediaSampleQueueWhenAllTracksAreAdded() {
        doReturn(TRACK_VIDEO).when(mediaMuxer).addTrack(videoMediaFormat);
        doReturn(TRACK_AUDIO).when(mediaMuxer).addTrack(audioMediaFormat);

        mediaMuxerWrapper.addTrack(videoMediaFormat, TRACK_VIDEO);
        mediaMuxerWrapper.writeSampleData(TRACK_VIDEO, byteBuffer, bufferInfo);
        mediaMuxerWrapper.addTrack(audioMediaFormat, TRACK_AUDIO);

        verify(mediaMuxer).addTrack(videoMediaFormat);
        verify(mediaMuxer).addTrack(audioMediaFormat);
        verify(mediaMuxer).start();
        verify(mediaMuxer).writeSampleData(trackCaptor.capture(), any(ByteBuffer.class), any(MediaCodec.BufferInfo.class));
        assertTrue(mediaMuxerWrapper.isStarted);
        assertThat(trackCaptor.getValue(), is(TRACK_VIDEO));
    }

    @Test
    public void releaseMediaMuxerWhenReleased() {
        mediaMuxerWrapper.release();

        verify(mediaMuxer).release();
    }
}
