package com.linkedin.android.litr.render;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.codec.Frame;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.linkedin.android.litr.render.PassthroughSoftwareRenderer.FRAME_WAIT_TIMEOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PassthroughSoftwareRendererShould {

    private static final long FRAME_PRESENTATION_TIME_NS = 42000L;
    private static final int FRAME_TAG = 1;
    private static final int FRAME_SIZE = 128;
    private static final int FRAME_OFFSET = 0;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 2;

    @Mock private Encoder encoder;
    @Mock private MediaFormat sourceMediaFormat;
    @Mock private MediaFormat targetAudioFormat;

    private PassthroughSoftwareRenderer renderer;

    private Frame frame;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.offset = FRAME_OFFSET;
        bufferInfo.size = FRAME_SIZE;
        bufferInfo.presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(FRAME_PRESENTATION_TIME_NS);
        bufferInfo.flags = 0;
        // NOTE: the below line of code didn't update the bufferInfo when run the tests, so fallback to the code above
        //bufferInfo.set(FRAME_OFFSET, FRAME_SIZE, FRAME_PRESENTATION_TIME, 0);

        ByteBuffer inputBuffer = ByteBuffer.allocate(FRAME_SIZE);
        for (int index = 0; index < FRAME_SIZE; index++) {
            inputBuffer.put((byte) index);
        }

        frame = new Frame(
                0,
                inputBuffer,
                bufferInfo);

        when(sourceMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(SAMPLE_RATE);
        when(sourceMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(CHANNELS);
        when(targetAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(SAMPLE_RATE);
        when(targetAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(CHANNELS);

        renderer = new PassthroughSoftwareRenderer(encoder, FRAME_WAIT_TIMEOUT);
        renderer.init(null, sourceMediaFormat, targetAudioFormat);
    }

    @Test
    public void notUseInputSurface() {
        assertNull(renderer.getInputSurface());
    }

    @Test
    public void notRenderWhenNoFrameProvided() {
        renderer.renderFrame(null, FRAME_PRESENTATION_TIME_NS);

        verify(encoder, never()).dequeueInputFrame(anyLong());
    }

    @Test
    public void notRenderWhenFrameHasNullBuffer() {
        Frame frame = new Frame(0, null, null);

        renderer.renderFrame(frame, FRAME_PRESENTATION_TIME_NS);

        verify(encoder, never()).dequeueInputFrame(anyLong());
    }

    @Test
    public void dropFrameWhenCannotQueueToEncoder() {
        when(encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT)).thenReturn(MediaCodec.INFO_TRY_AGAIN_LATER);

        renderer.renderFrame(frame, FRAME_PRESENTATION_TIME_NS);

        verify(encoder, never()).getInputFrame(anyInt());
    }

    @Test
    public void notRenderWhenEncoderReturnsNullInputFrame() {
        when(encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT)).thenReturn(FRAME_TAG);
        when(encoder.getInputFrame(FRAME_TAG)).thenReturn(null);

        renderer.renderFrame(frame, FRAME_PRESENTATION_TIME_NS);

        verify(encoder, never()).queueInputFrame(frame);
    }

    @Test
    public void renderWhenEncoderAcceptsFrames() {
        int encoderFrameTag = 2;
        ByteBuffer encoderInputBuffer = ByteBuffer.allocate(FRAME_SIZE);
        MediaCodec.BufferInfo encoderBufferInfo = new MediaCodec.BufferInfo();
        Frame encoderInputFrame = new Frame(encoderFrameTag, encoderInputBuffer, encoderBufferInfo);

        when(encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT)).thenReturn(FRAME_TAG);
        when(encoder.getInputFrame(FRAME_TAG)).thenReturn(encoderInputFrame);

        renderer.renderFrame(frame, FRAME_PRESENTATION_TIME_NS);

        verify(encoder).queueInputFrame(encoderInputFrame);
        assertThat(encoderInputFrame.bufferInfo.flags, is(frame.bufferInfo.flags));
        assertThat(encoderInputFrame.bufferInfo.presentationTimeUs, is(frame.bufferInfo.presentationTimeUs));
        assertThat(encoderInputFrame.bufferInfo.offset, is(0));
        assertThat(encoderInputFrame.bufferInfo.size, is(frame.bufferInfo.size));

        for (int index = 0; index < FRAME_SIZE; index++) {
            assertThat(frame.buffer.get(index), is(encoderInputBuffer.get(index)));
        }
    }

    @Test
    public void renderWhenEncoderAcceptsMultiFramesWithPassThrough() {
        int inputFrameSize = FRAME_SIZE;
        int frameSize1 = 50;
        int frameSize2 = 50;
        int frameSize3 = inputFrameSize - frameSize1 - frameSize2;
        int encoderFrameTag1 = 1;
        int encoderFrameTag2 = 2;
        int encoderFrameTag3 = 3;
        ByteBuffer encoderInputBuffer1 = ByteBuffer.allocate(frameSize1);
        ByteBuffer encoderInputBuffer2 = ByteBuffer.allocate(frameSize2);
        ByteBuffer encoderInputBuffer3 = ByteBuffer.allocate(frameSize3);
        Frame encoderInputFrame1 = new Frame(encoderFrameTag1, encoderInputBuffer1, new MediaCodec.BufferInfo());
        Frame encoderInputFrame2 = new Frame(encoderFrameTag2, encoderInputBuffer2, new MediaCodec.BufferInfo());
        Frame encoderInputFrame3 = new Frame(encoderFrameTag3, encoderInputBuffer3, new MediaCodec.BufferInfo());

        when(encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT)).thenReturn(encoderFrameTag1, encoderFrameTag2, encoderFrameTag3);
        when(encoder.getInputFrame(encoderFrameTag1)).thenReturn(encoderInputFrame1);
        when(encoder.getInputFrame(encoderFrameTag2)).thenReturn(encoderInputFrame2);
        when(encoder.getInputFrame(encoderFrameTag3)).thenReturn(encoderInputFrame3);

        renderer.renderFrame(frame, FRAME_PRESENTATION_TIME_NS);

        verifyEncoderWithSpecificFrame(encoderInputFrame1, encoderInputBuffer1, frameSize1, 0);
        verifyEncoderWithSpecificFrame(encoderInputFrame2, encoderInputBuffer2, frameSize2, frameSize1);
        verifyEncoderWithSpecificFrame(encoderInputFrame3, encoderInputBuffer3, frameSize3, frameSize2);
    }

    @Test
    public void renderWhenEncoderAcceptsMultiFramesWithDownSampling() {
        int inputFrameSize = FRAME_SIZE;
        int frameSize1 = 48;
        int frameSize2 = 48;
        int frameSize3 = 28;
        int frameSize4 = 4;
        int encoderFrameTag1 = 1;
        int encoderFrameTag2 = 2;
        int encoderFrameTag3 = 3;
        int encoderFrameTag4 = 4;
        ByteBuffer encoderInputBuffer1 = ByteBuffer.allocate(frameSize1);
        ByteBuffer encoderInputBuffer2 = ByteBuffer.allocate(frameSize2);
        ByteBuffer encoderInputBuffer3 = ByteBuffer.allocate(frameSize3);
        ByteBuffer encoderInputBuffer4 = ByteBuffer.allocate(frameSize4);
        Frame encoderInputFrame1 = new Frame(encoderFrameTag1, encoderInputBuffer1, new MediaCodec.BufferInfo());
        Frame encoderInputFrame2 = new Frame(encoderFrameTag2, encoderInputBuffer2, new MediaCodec.BufferInfo());
        Frame encoderInputFrame3 = new Frame(encoderFrameTag3, encoderInputBuffer3, new MediaCodec.BufferInfo());
        Frame encoderInputFrame4 = new Frame(encoderFrameTag4, encoderInputBuffer4, new MediaCodec.BufferInfo());

        when(encoder.dequeueInputFrame(FRAME_WAIT_TIMEOUT)).thenReturn(encoderFrameTag1, encoderFrameTag2,
                encoderFrameTag3, encoderFrameTag4);
        when(encoder.getInputFrame(encoderFrameTag1)).thenReturn(encoderInputFrame1);
        when(encoder.getInputFrame(encoderFrameTag2)).thenReturn(encoderInputFrame2);
        when(encoder.getInputFrame(encoderFrameTag3)).thenReturn(encoderInputFrame3);
        when(encoder.getInputFrame(encoderFrameTag4)).thenReturn(encoderInputFrame4);

        int downSampleRate = SAMPLE_RATE / 2;
        when(targetAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(downSampleRate);
        renderer.onMediaFormatChanged(sourceMediaFormat, targetAudioFormat);

        renderer.renderFrame(frame, FRAME_PRESENTATION_TIME_NS);

        float sampleRateRatio = (float) downSampleRate / SAMPLE_RATE;
        verifyEncoderWithSpecificFrame(encoderInputFrame1, encoderInputBuffer1, (int) (frameSize1 * sampleRateRatio), 0, true);
        verifyEncoderWithSpecificFrame(encoderInputFrame2, encoderInputBuffer2, (int) (frameSize2 * sampleRateRatio), frameSize1, true);
        verifyEncoderWithSpecificFrame(encoderInputFrame3, encoderInputBuffer3, (int) (frameSize3 * sampleRateRatio), frameSize2, true);
        verifyEncoderWithSpecificFrame(encoderInputFrame4, encoderInputBuffer4, (int) (frameSize4 * sampleRateRatio), frameSize3, true);
    }

    private void verifyEncoderWithSpecificFrame(Frame encoderInputFrame, ByteBuffer encoderInputBuffer, int frameSize,
            int firstIndex) {
        verifyEncoderWithSpecificFrame(encoderInputFrame, encoderInputBuffer, frameSize, firstIndex, false);
    }

    private void verifyEncoderWithSpecificFrame(Frame encoderInputFrame, ByteBuffer encoderInputBuffer, int frameSize,
            int firstIndex, boolean skipByteByByteCheck) {
        verify(encoder).queueInputFrame(encoderInputFrame);
        assertThat(encoderInputFrame.bufferInfo.flags, is(frame.bufferInfo.flags));
        assertThat(encoderInputFrame.bufferInfo.presentationTimeUs, is(frame.bufferInfo.presentationTimeUs));
        assertThat(encoderInputFrame.bufferInfo.offset, is(0));
        assertThat(encoderInputFrame.bufferInfo.size, is(frameSize));

        if (!skipByteByByteCheck) {
            for (int index = firstIndex; index < frameSize; index++) {
                assertThat(frame.buffer.get(index), is(encoderInputBuffer.get(index - firstIndex)));
            }
        }
    }
}
