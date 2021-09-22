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
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.Frame;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.Renderer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AudioTrackTranscoderShould {

    private static final int AUDIO_TRACK = 0;
    private static final int VIDEO_TRACK = 1;
    private static final int BUFFER_INDEX = 0;
    private static final int BUFFER_SIZE = 42;
    private static final long DURATION = 84;
    private static final long CURRENT_PRESENTATION_TIME = 42L;
    private static final float CURRENT_PROGRESS = 0.5f;

    private static final long SELECTION_START = 16;
    private static final long SELECTION_END = 64;

    @Mock private MediaSource mediaSource;
    @Mock private MediaTarget mediaTarget;
    @Mock private MediaFormat sourceMediaFormat;
    @Mock private MediaCodec.BufferInfo bufferInfo;

    @Mock private Encoder encoder;
    @Mock private Decoder decoder;
    @Mock private Renderer renderer;

    private MediaFormat targetAudioFormat;
    private AudioTrackTranscoder audioTrackTranscoder;

    private Frame sampleFrame;
    private MediaRange fullMediaRange;
    private MediaRange trimmedMediaRange;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        sampleFrame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);
        fullMediaRange = new MediaRange(0, Long.MAX_VALUE);
        trimmedMediaRange = new MediaRange(SELECTION_START, SELECTION_END);

        doReturn(sourceMediaFormat).when(mediaSource).getTrackFormat(anyInt());
        doReturn(true).when(decoder).isRunning();
        doReturn(true).when(encoder).isRunning();

        when(mediaSource.getSelection()).thenReturn(fullMediaRange);

        targetAudioFormat = new MediaFormat();
        targetAudioFormat.setString(MediaFormat.KEY_MIME, "audio/aac");
        targetAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        targetAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        targetAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);

        // setting up and starting test target, which will be used for frame processing testing below
        audioTrackTranscoder = spy(new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder));
        audioTrackTranscoder.start();
    }

    // region: starting/stopping tests
    // they use their own non-shared mocks of CodecFinder and VideoTrackTranscoder, because we don't want to
    // interfere with the "running" state of the class level mocks
    @Test(expected = TrackTranscoderException.class)
    public void notStartWhenCannotFindEncoder() throws Exception {
        doThrow(new TrackTranscoderException(TrackTranscoderException.Error.ENCODER_FORMAT_NOT_FOUND))
            .when(encoder)
            .init(any(MediaFormat.class));

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
        audioTrackTranscoder.start();
    }

    @Test(expected = TrackTranscoderException.class)
    public void notStartWhenCannotFindDecoder() throws Exception {
        doThrow(new TrackTranscoderException(TrackTranscoderException.Error.DECODER_FORMAT_NOT_FOUND))
            .when(encoder)
            .init(any(MediaFormat.class));

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);

        audioTrackTranscoder.start();
    }

    @Test
    public void startWhenEncoderAndDecoderAreStarted() throws Exception {
        Encoder encoder = mock(Encoder.class);
        Decoder decoder = mock(Decoder.class);
        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
        audioTrackTranscoder.start();

        verify(encoder).start();
        verify(decoder).start();
    }

    @Test
    public void stopAndReleaseEncoderWhenStopped() throws Exception {
        Encoder encoder = mock(Encoder.class);
        Decoder decoder = mock(Decoder.class);

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);

        audioTrackTranscoder.stop();

        verify(encoder).stop();
        verify(encoder).release();
    }

    @Test
    public void stopAndReleaseDecoderWhenStopped() throws Exception {
        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);

        audioTrackTranscoder.stop();

        verify(decoder).stop();
        verify(decoder).release();
    }

    // endregion: starting/stopping tests

    // region: extracting & decoding frames

    @Test
    public void notProcessFrameWhenNotStarted() throws Exception {
        // use a method level test target, because we want it to be in the stopped state
        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource, AUDIO_TRACK,
                mediaTarget, AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
        doReturn(false).when(encoder).isRunning();

        int result = audioTrackTranscoder.processNextFrame();

        assertThat(result, is(TrackTranscoder.ERROR_TRANSCODER_NOT_RUNNING));
    }

    @Test
    public void notTryToDecodeFrameWhenOtherTrackIsSelected() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(AUDIO_TRACK + 1).when(mediaSource).getSampleTrackIndex();
        int result = audioTrackTranscoder.processNextFrame();

        verify(decoder, never()).dequeueInputFrame(anyLong());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void tryToDecodeFrameWhenNoTrackIsSelected() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(TrackTranscoder.NO_SELECTED_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(decoder).dequeueInputFrame(anyLong());

        int result = audioTrackTranscoder.processNextFrame();

        verify(decoder).dequeueInputFrame(anyLong());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void tryToDecodeFrameWhenVideoTrackIsSelected() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(AUDIO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(decoder).dequeueInputFrame(anyLong());

        int result = audioTrackTranscoder.processNextFrame();

        verify(decoder).dequeueInputFrame(anyLong());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void extractAndDecodeFrameWhenNotEos() throws  Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(AUDIO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_INDEX).when(decoder).dequeueInputFrame(anyLong());
        doReturn(sampleFrame).when(decoder).getInputFrame(BUFFER_INDEX);
        doReturn(BUFFER_SIZE).when(mediaSource).readSampleData(any(ByteBuffer.class), anyInt());
        doReturn(CURRENT_PRESENTATION_TIME).when(mediaSource).getSampleTime();
        doReturn(0).when(mediaSource).getSampleFlags();

        int result = audioTrackTranscoder.processNextFrame();

        verify(mediaSource).readSampleData(sampleFrame.buffer, 0);
        verify(decoder).queueInputFrame(sampleFrame);
        verify(bufferInfo).set(0, BUFFER_SIZE, CURRENT_PRESENTATION_TIME, 0);

        verify(mediaSource, atLeast(1)).advance();

        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void extractAndDecodeFrameWhenNoBytesRead() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(AUDIO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_INDEX).when(decoder).dequeueInputFrame(anyLong());
        doReturn(sampleFrame).when(decoder).getInputFrame(BUFFER_INDEX);
        doReturn(0).when(mediaSource).readSampleData(any(ByteBuffer.class), anyInt());
        doReturn(CURRENT_PRESENTATION_TIME).when(mediaSource).getSampleTime();
        doReturn(0).when(mediaSource).getSampleFlags();

        int result = audioTrackTranscoder.processNextFrame();

        verify(mediaSource).readSampleData(sampleFrame.buffer, 0);
        verify(decoder).queueInputFrame(sampleFrame);
        verify(bufferInfo).set(0, 0, CURRENT_PRESENTATION_TIME, 0);

        verify(mediaSource, atLeast(1)).advance();

        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void signalDecoderWhenEos() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(AUDIO_TRACK).when(mediaSource).getSampleTrackIndex();
        doReturn(BUFFER_INDEX).when(decoder).dequeueInputFrame(anyLong());
        doReturn(sampleFrame).when(decoder).getInputFrame(BUFFER_INDEX);
        doReturn(-1).when(mediaSource).readSampleData(any(ByteBuffer.class), anyInt());

        int result = audioTrackTranscoder.processNextFrame();

        verify(mediaSource).readSampleData(sampleFrame.buffer, 0);
        verify(decoder).queueInputFrame(sampleFrame);
        verify(bufferInfo).set(0, 0, -1L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    // endregion: extracting & decoding frames

    // region: queueing frames
    @Test
    public void notQueueWhenNoDecodedFrameReceived() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(decoder).dequeueOutputFrame(anyLong());

        int result = audioTrackTranscoder.processNextFrame();

        verify(decoder, never()).releaseOutputFrame(anyInt(), anyBoolean());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void queueAndNotEncodeWhenDecodedFrameReceivedButEncoderHasNoFrame() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = 0;
        bufferInfo.presentationTimeUs = CURRENT_PRESENTATION_TIME;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        doReturn(BUFFER_INDEX).when(decoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(decoder).getOutputFrame(BUFFER_INDEX);
        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(encoder).dequeueInputFrame(anyLong());

        int result = audioTrackTranscoder.processNextFrame();

        verify(renderer).renderFrame(frame, TimeUnit.MICROSECONDS.toNanos(CURRENT_PRESENTATION_TIME));
        verify(encoder, never()).getInputFrame(anyInt());

        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void queueAndEncodeLastFrameWhenDecodedFrameReceivedButEncoderHasFrame() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        bufferInfo.presentationTimeUs = CURRENT_PRESENTATION_TIME;
        bufferInfo.offset = 0;
        bufferInfo.size = BUFFER_SIZE;
        Frame decoderOutputFrame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        doReturn(BUFFER_INDEX).when(decoder).dequeueOutputFrame(anyLong());
        doReturn(decoderOutputFrame).when(decoder).getOutputFrame(BUFFER_INDEX);
        doReturn(BUFFER_INDEX).when(encoder).dequeueInputFrame(anyLong());
        doReturn(sampleFrame).when(encoder).getInputFrame(anyInt());

        int result = audioTrackTranscoder.processNextFrame();

        verify(renderer).renderFrame(decoderOutputFrame, TimeUnit.MICROSECONDS.toNanos(CURRENT_PRESENTATION_TIME));
        verify(decoder).releaseOutputFrame(BUFFER_INDEX, false);

        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    // endregion: resizing decoded frames

    // region: receiving & writing encoded frames

    @Test
    public void notWriteWhenNoEncodedFrameReceived() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;

        doReturn(MediaCodec.INFO_TRY_AGAIN_LATER).when(encoder).dequeueOutputFrame(anyLong());

        int result = audioTrackTranscoder.processNextFrame();

        verify(encoder, never()).releaseOutputFrame(anyInt());
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void addTrackWhenEncoderMediaFormatReceived() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;

        MediaFormat encoderMediaFormat = new MediaFormat();
        doReturn(MediaCodec.INFO_OUTPUT_FORMAT_CHANGED).when(encoder).dequeueOutputFrame(anyLong());
        doReturn(encoderMediaFormat).when(encoder).getOutputFormat();
        doReturn(AUDIO_TRACK).when(mediaTarget).addTrack(any(MediaFormat.class), anyInt());

        int result = audioTrackTranscoder.processNextFrame();

        ArgumentCaptor<MediaFormat> mediaFormatArgumentCaptor = ArgumentCaptor.forClass(MediaFormat.class);
        verify(mediaTarget).addTrack(mediaFormatArgumentCaptor.capture(), eq(AUDIO_TRACK));
        assertThat(mediaFormatArgumentCaptor.getValue(), is(encoderMediaFormat));

        assertThat(audioTrackTranscoder.targetTrack, is(AUDIO_TRACK));
        assertThat(result, is(TrackTranscoder.RESULT_OUTPUT_MEDIA_FORMAT_CHANGED));
    }

    @Test
    public void writeWhenEncodedFrameReceived() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;
        audioTrackTranscoder.targetTrack = AUDIO_TRACK;
        audioTrackTranscoder.duration = DURATION;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = 0;
        bufferInfo.size = BUFFER_SIZE;
        bufferInfo.presentationTimeUs = CURRENT_PRESENTATION_TIME;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        doReturn(BUFFER_INDEX).when(encoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(encoder).getOutputFrame(anyInt());

        int result = audioTrackTranscoder.processNextFrame();

        ArgumentCaptor<Integer> targetTrackArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<ByteBuffer> bufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
        ArgumentCaptor<MediaCodec.BufferInfo> bufferInfoArgumentCaptor = ArgumentCaptor.forClass(MediaCodec.BufferInfo.class);

        verify(mediaTarget).writeSampleData(targetTrackArgumentCaptor.capture(), bufferArgumentCaptor.capture(), bufferInfoArgumentCaptor.capture());
        assertThat(targetTrackArgumentCaptor.getValue(), is(AUDIO_TRACK));
        assertThat(bufferInfoArgumentCaptor.getValue().presentationTimeUs, is(CURRENT_PRESENTATION_TIME));
        assertThat(audioTrackTranscoder.progress, is(CURRENT_PROGRESS));

        verify(encoder).releaseOutputFrame(eq(BUFFER_INDEX));
        assertThat(result, is(TrackTranscoder.RESULT_FRAME_PROCESSED));
    }

    @Test
    public void finishWhenEosReceived() throws Exception {
        audioTrackTranscoder.lastExtractFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastDecodeFrameResult = TrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = TrackTranscoder.RESULT_FRAME_PROCESSED;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        bufferInfo.size = 0;
        bufferInfo.presentationTimeUs = -1L;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        doReturn(BUFFER_INDEX).when(encoder).dequeueOutputFrame(anyLong());
        doReturn(frame).when(encoder).getOutputFrame(anyInt());

        int result = audioTrackTranscoder.processNextFrame();

        assertThat(audioTrackTranscoder.progress, is(1.0f));

        verify(encoder).releaseOutputFrame(eq(BUFFER_INDEX));
        assertThat(result, is(TrackTranscoder.RESULT_EOS_REACHED));
    }

    // endregion: receiving & writing encoded frames

    // region: trimming media

    @Test(expected = IllegalArgumentException.class)
    public void failWhenSelectionEndIsBeforeStart() throws Exception {
        MediaRange selection = new MediaRange(42L, 6L);
        when(mediaSource.getSelection()).thenReturn(selection);

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource,
                AUDIO_TRACK,
                mediaTarget,
                AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
    }

    @Test
    public void adjustDurationToMediaSelection() throws Exception {
        when(sourceMediaFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(sourceMediaFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(DURATION);
        when(mediaSource.getSelection()).thenReturn(trimmedMediaRange);

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource,
                AUDIO_TRACK,
                mediaTarget,
                AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);

        assertThat(audioTrackTranscoder.duration, is(SELECTION_END - SELECTION_START));
    }

    @Test
    public void notRenderFrameBeforeSelectionStart() throws Exception {
        int tag = 1;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = 0;
        bufferInfo.presentationTimeUs = SELECTION_START - 1;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        when(decoder.isRunning()).thenReturn(true);
        when(encoder.isRunning()).thenReturn(true);
        when(mediaSource.getSelection()).thenReturn(trimmedMediaRange);
        when(decoder.dequeueOutputFrame(anyLong())).thenReturn(tag);
        when(decoder.getOutputFrame(tag)).thenReturn(frame);

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource,
                AUDIO_TRACK,
                mediaTarget,
                AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
        audioTrackTranscoder.lastExtractFrameResult = VideoTrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = VideoTrackTranscoder.RESULT_EOS_REACHED;

        audioTrackTranscoder.processNextFrame();

        verify(renderer, never()).renderFrame(any(Frame.class), anyLong());
        verify(decoder).releaseOutputFrame(tag, false);
    }

    @Test
    public void renderFrameWithinSelection() throws Exception {
        int tag = 1;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.flags = 0;
        bufferInfo.presentationTimeUs = CURRENT_PRESENTATION_TIME;
        Frame frame = new Frame(BUFFER_INDEX, ByteBuffer.allocate(BUFFER_SIZE), bufferInfo);

        when(decoder.isRunning()).thenReturn(true);
        when(encoder.isRunning()).thenReturn(true);
        when(mediaSource.getSelection()).thenReturn(trimmedMediaRange);
        when(decoder.dequeueOutputFrame(anyLong())).thenReturn(tag);
        when(decoder.getOutputFrame(tag)).thenReturn(frame);

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource,
                AUDIO_TRACK,
                mediaTarget,
                AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
        audioTrackTranscoder.lastExtractFrameResult = VideoTrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = VideoTrackTranscoder.RESULT_EOS_REACHED;

        audioTrackTranscoder.processNextFrame();

        verify(renderer).renderFrame(frame, TimeUnit.MICROSECONDS.toNanos(CURRENT_PRESENTATION_TIME - SELECTION_START));
        verify(decoder).releaseOutputFrame(tag, false);
    }

    @Test
    public void notDecodeFrameAndAdvanceToOtherTrackAndSendEosWhenFrameAfterSelectionEnd() throws Exception {
        int tag = 1;

        when(decoder.dequeueInputFrame(anyLong())).thenReturn(tag);
        when(decoder.getInputFrame(tag)).thenReturn(sampleFrame);
        when(mediaSource.getSelection()).thenReturn(trimmedMediaRange);
        when(mediaSource.getSampleTime()).thenReturn(SELECTION_END + 1);
        when(mediaSource.getSampleFlags()).thenReturn(0);
        when(mediaSource.readSampleData(sampleFrame.buffer, 0)).thenReturn(BUFFER_SIZE);
        when(mediaSource.getSampleTrackIndex())
                .thenReturn(AUDIO_TRACK)
                .thenReturn(AUDIO_TRACK)
                .thenReturn(VIDEO_TRACK);

        AudioTrackTranscoder audioTrackTranscoder = new AudioTrackTranscoder(
                mediaSource,
                AUDIO_TRACK,
                mediaTarget,
                AUDIO_TRACK,
                targetAudioFormat,
                renderer,
                decoder,
                encoder);
        audioTrackTranscoder.lastDecodeFrameResult = VideoTrackTranscoder.RESULT_EOS_REACHED;
        audioTrackTranscoder.lastEncodeFrameResult = VideoTrackTranscoder.RESULT_EOS_REACHED;

        audioTrackTranscoder.processNextFrame();

        verify(decoder).queueInputFrame(sampleFrame);
        verify(sampleFrame.bufferInfo).set(0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        assertThat(audioTrackTranscoder.lastExtractFrameResult, is(VideoTrackTranscoder.RESULT_EOS_REACHED));
    }

    // endregion: trimming media
}
