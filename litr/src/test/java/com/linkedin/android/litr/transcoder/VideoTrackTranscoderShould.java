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
import android.view.Surface;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.Frame;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.GlVideoRenderer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class VideoTrackTranscoderShould {
    private static final int VIDEO_TRACK = 0;
    private static final int BUFFER_INDEX = 0;
    private static final int BUFFER_SIZE = 42;
    private static final float DURATION = 84f;
    private static final long CURRENT_PRESENTATION_TIME = 42L;
    private static final float CURRENT_PROGRESS = 0.5f;

    @Mock private MediaSource mediaSource;
    @Mock private MediaTarget mediaTarget;
    @Mock private MediaFormat sourceMediaFormat;
    @Mock private Surface surface;
    @Mock private MediaCodec.BufferInfo bufferInfo;

    @Mock private Encoder encoder;
    @Mock private Decoder decoder;
    @Mock private GlVideoRenderer renderer;

    private MediaFormat targetVideoFormat;
    private VideoTrackTranscoder videoTrackTranscoder;

    private ByteBuffer[] sampleByteBuffers;
    private Frame sampleFrame;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        sampleByteBuffers = new ByteBuffer[] {buffer};
        sampleFrame = new Frame(BUFFER_INDEX, buffer, bufferInfo);

        doReturn(sourceMediaFormat).when(mediaSource).getTrackFormat(anyInt());
        doReturn(surface).when(encoder).createInputSurface();
        doReturn(surface).when(renderer).getInputSurface();
        doReturn(true).when(decoder).isRunning();
        doReturn(true).when(encoder).isRunning();

        targetVideoFormat = new MediaFormat();
        targetVideoFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        targetVideoFormat.setInteger(MediaFormat.KEY_WIDTH, 1280);
        targetVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, 720);
        targetVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 4000000);
        targetVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);

        // setting up and starting test target, which will be used for frame processing testing below
        videoTrackTranscoder = spy(new VideoTrackTranscoder(mediaSource,
                                                            VIDEO_TRACK,
                                                            mediaTarget,
                                                            targetVideoFormat,
                                                            renderer,
                                                            decoder,
                                                            encoder));
        videoTrackTranscoder.start();
    }

    // region: starting/stopping tests
    // they use their own non-shared mocks of CodecFinder and VideoTrackTranscoder, because we don't want to
    // interfere with the "running" state of the class level mocks
    @Test(expected = TrackTranscoderException.class)
    public void notStartWhenCannotFindEncoder() throws Exception {
        doThrow(new TrackTranscoderException(TrackTranscoderException.Error.ENCODER_FORMAT_NOT_FOUND))
                .when(encoder)
                .init(any(MediaFormat.class));
        VideoTrackTranscoder videoTrackTranscoder = spy(new VideoTrackTranscoder(mediaSource,
                                                                                 VIDEO_TRACK,
                                                                                 mediaTarget,
                                                                                 targetVideoFormat,
                                                                                 renderer,
                                                                                 decoder,
                                                                                 encoder));
        videoTrackTranscoder.start();
    }

   @Test(expected = TrackTranscoderException.class)
    public void notStartWhenCannotFindDecoder() throws Exception {
        Decoder decoder = mock(Decoder.class);
        doThrow(new TrackTranscoderException(TrackTranscoderException.Error.DECODER_FORMAT_NOT_FOUND))
               .when(decoder)
               .init(any(MediaFormat.class), any(Surface.class));

       VideoTrackTranscoder videoTrackTranscoder = new VideoTrackTranscoder(mediaSource,
                                                                            VIDEO_TRACK,
                                                                            mediaTarget,
                                                                            targetVideoFormat,
                                                                            renderer,
                                                                            decoder,
                                                                            encoder);

        //videoTrackTranscoder.start();
    }

    @Test
    public void startWhenEncoderAndDecoderAreStarted() throws Exception {
        GlVideoRenderer renderer = mock(GlVideoRenderer.class);
        Decoder decoder = mock(Decoder.class);
        Encoder encoder = mock(Encoder.class);
        doReturn(surface).when(encoder).createInputSurface();

        VideoTrackTranscoder videoTrackTranscoder = new VideoTrackTranscoder(mediaSource,
                                                                             VIDEO_TRACK,
                                                                             mediaTarget,
                                                                             targetVideoFormat,
                                                                             renderer,
                                                                             decoder,
                                                                             encoder);
        videoTrackTranscoder.start();

        verify(encoder, never()).stop();
        verify(decoder, never()).stop();
        verify(encoder).start();
        verify(decoder).start();
        verify(renderer).init(surface, 0);
    }

    @Test
    public void stopAndReleaseEncoderAndDecoderWhenStopped() throws Exception {
        VideoTrackTranscoder videoTrackTranscoder = spy(new VideoTrackTranscoder(mediaSource,
                                                                                 VIDEO_TRACK,
                                                                                 mediaTarget,
                                                                                 targetVideoFormat,
                                                                                 renderer,
                                                                                 decoder,
                                                                                 encoder));
        videoTrackTranscoder.stop();

        verify(decoder).stop();
        verify(decoder).release();
        verify(encoder).stop();
        verify(encoder).release();
        verify(renderer).release();
    }

    // endregion: starting/stopping tests

    // region: extracting & decoding frames

    @Test
    public void notProcessFrameWhenNotStarted() throws Exception {
        GlVideoRenderer renderer = mock(GlVideoRenderer.class);
        Decoder decoder = mock(Decoder.class);
        Encoder encoder = mock(Encoder.class);
        doReturn(surface).when(encoder).createInputSurface();

        // use a method level test target, because we want it to be in the stopped state
        VideoTrackTranscoder videoTrackTranscoder = new VideoTrackTranscoder(mediaSource,
                                                                             VIDEO_TRACK,
                                                                             mediaTarget,
                                                                             targetVideoFormat,
                                                                             renderer,
                                                                             decoder,
                                                                             encoder);
        int result = videoTrackTranscoder.processNextFrame();

        assertThat(result, is(TrackTranscoder.ERROR_TRANSCODER_NOT_RUNNING));
    }

    @Test
    public void notTryToDecodeFrameWhenOtherTrackIsSelected() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(VIDEO_TRACK + 1).when(mediaSource).getSampleTrackIndex();
        int result = videoTrackTranscoder.processNextFrame();

        verify(decoder, never()).dequeueInputFrame(anyLong());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void tryToDecodeFrameWhenNoTrackIsSelected() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(TrackTranscoder.NO_SELECTED_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(decoder).dequeueInputFrame(anyLong());

        int result = videoTrackTranscoder.processNextFrame();

        verify(decoder).dequeueInputFrame(anyLong());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void tryToDecodeFrameWhenVideoTrackIsSelected() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(VIDEO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(decoder).dequeueInputFrame(anyLong());

        int result = videoTrackTranscoder.processNextFrame();

        verify(decoder).dequeueInputFrame(anyLong());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void extractAndDecodeFrameWhenNotEos() throws  Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(VIDEO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_INDEX).when(decoder).dequeueInputFrame(anyLong());
        doReturn(sampleFrame).when(decoder).getInputFrame(BUFFER_INDEX);
        doReturn(BUFFER_SIZE).when(mediaSource).readSampleData(any(ByteBuffer.class), anyInt());
        doReturn(CURRENT_PRESENTATION_TIME).when(mediaSource).getSampleTime();
        doReturn(0).when(mediaSource).getSampleFlags();

        int result = videoTrackTranscoder.processNextFrame();

        verify(mediaSource).readSampleData(eq(sampleFrame.buffer), eq(0));
        verify(decoder).queueInputFrame(sampleFrame);
        verify(bufferInfo).set(0, BUFFER_SIZE, CURRENT_PRESENTATION_TIME, 0);

        verify(mediaSource, atLeast(1)).advance();

        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void signalDecoderWhenEos() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(VIDEO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_INDEX).when(decoder).dequeueInputFrame(anyLong());
        doReturn(sampleFrame).when(decoder).getInputFrame(BUFFER_INDEX);
        doReturn(0).when(mediaSource).readSampleData(any(ByteBuffer.class), anyInt());

        int result = videoTrackTranscoder.processNextFrame();

        verify(mediaSource).readSampleData(eq(sampleFrame.buffer), eq(0));
        verify(decoder).queueInputFrame(sampleFrame);
        verify(bufferInfo).set(0, 0, -1L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    // endregion: extracting & decoding frames

    // region: resizing frames
    @Test
    public void notResizeWhenNoDecodedFrameReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(decoder).dequeueOutputFrame(anyLong());

        int result = videoTrackTranscoder.processNextFrame();

        verify(decoder, never()).releaseOutputFrame(anyInt(), anyBoolean());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void resizeWhenDecodedFrameReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = 0;
        bufferInfo.presentationTimeUs = CURRENT_PRESENTATION_TIME;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);
        doReturn(BUFFER_INDEX).when(decoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(decoder).getOutputFrame(BUFFER_INDEX);

        int result = videoTrackTranscoder.processNextFrame();

        verify(decoder).releaseOutputFrame(BUFFER_INDEX, true);

        ArgumentCaptor<Long> presentationTimeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(renderer).renderFrame((Frame) isNull(), presentationTimeCaptor.capture());
        assertThat(presentationTimeCaptor.getValue(), is(CURRENT_PRESENTATION_TIME * 1000L));

        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void signalEncoderWhenEosReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        bufferInfo.presentationTimeUs = -1;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);
        doReturn(BUFFER_INDEX).when(decoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(decoder).getOutputFrame(BUFFER_INDEX);

        int result = videoTrackTranscoder.processNextFrame();

        verify(decoder).releaseOutputFrame(BUFFER_INDEX, false);

        verify(encoder).signalEndOfInputStream();
        assertThat(videoTrackTranscoder.lastDecodeFrameResult, is(TrackTranscoder.RESULT_EOS_REACHED));
        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));

    }

    // endregion: resizing decoded frames

    // region: receiving & writing encoded frames

    @Test
    public void notWriteWhenNoEncodedFrameReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;

        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(encoder).dequeueOutputFrame(anyLong());

        int result = videoTrackTranscoder.processNextFrame();

        verify(encoder, never()).releaseOutputFrame(anyInt());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void addTrackWhenEncoderMediaFormatReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.targetTrack = TrackTranscoder.NO_SELECTED_TRACK;

        MediaFormat encoderMediaFormat = new MediaFormat();
        doReturn(MediaCodec.INFO_OUTPUT_FORMAT_CHANGED).when(encoder).dequeueOutputFrame(anyLong());
        doReturn(encoderMediaFormat).when(encoder).getOutputFormat();
        doReturn(VIDEO_TRACK).when(mediaTarget).addTrack(any(MediaFormat.class), anyInt());

        int result = videoTrackTranscoder.processNextFrame();

        ArgumentCaptor<MediaFormat> mediaFormatArgumentCaptor = ArgumentCaptor.forClass(MediaFormat.class);
        verify(mediaTarget).addTrack(mediaFormatArgumentCaptor.capture(), eq(VIDEO_TRACK));
        assertThat(mediaFormatArgumentCaptor.getValue(), is(encoderMediaFormat));

        assertThat(videoTrackTranscoder.targetTrack, is(VIDEO_TRACK));
        assertThat(result, is(TrackTranscoder.RESULT_OUTPUT_MEDIA_FORMAT_CHANGED));
    }

    @Test
    public void writeWhenEncodedFrameReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        videoTrackTranscoder.targetTrack = VIDEO_TRACK;
        videoTrackTranscoder.duration = DURATION;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = 0;
        bufferInfo.size = BUFFER_SIZE;
        bufferInfo.presentationTimeUs = CURRENT_PRESENTATION_TIME;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        doReturn(BUFFER_INDEX).when(encoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(encoder).getOutputFrame(BUFFER_INDEX);

        int result = videoTrackTranscoder.processNextFrame();

        ArgumentCaptor<Integer> targetTrackArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
        ArgumentCaptor<MediaCodec.BufferInfo> bufferInfoArgumentCaptor = ArgumentCaptor.forClass(MediaCodec.BufferInfo.class);

        verify(mediaTarget).writeSampleData(targetTrackArgumentCaptor.capture(), bufferArgumentCaptor.capture(), bufferInfoArgumentCaptor.capture());
        assertThat(targetTrackArgumentCaptor.getValue(), is(VIDEO_TRACK));
        assertThat(bufferArgumentCaptor.getValue(), is(sampleByteBuffers[BUFFER_INDEX]));
        assertThat(bufferInfoArgumentCaptor.getValue().presentationTimeUs, is(CURRENT_PRESENTATION_TIME));
        assertThat(videoTrackTranscoder.progress, is(CURRENT_PROGRESS));

        verify(encoder).releaseOutputFrame(eq(BUFFER_INDEX));
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void finishWhenEosReceived() throws Exception {
        videoTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        videoTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        bufferInfo.size = 0;
        bufferInfo.presentationTimeUs = -1L;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        doReturn(BUFFER_INDEX).when(encoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(encoder).getOutputFrame(BUFFER_INDEX);

        int result = videoTrackTranscoder.processNextFrame();

        assertThat(videoTrackTranscoder.progress, is(1.0f));

        verify(encoder).releaseOutputFrame(eq(BUFFER_INDEX));
        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    // endregion: receiving & writing encoded frames
}
