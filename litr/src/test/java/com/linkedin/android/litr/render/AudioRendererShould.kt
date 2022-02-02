package com.linkedin.android.litr.render

import android.media.MediaCodec
import android.media.MediaFormat
import com.linkedin.android.litr.codec.Encoder
import com.linkedin.android.litr.codec.Frame
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import kotlin.test.assertNotNull

private const val INPUT_FRAME_TAG = 0
private const val OUTPUT_FRAME_TAG_1 = 1
private const val OUTPUT_FRAME_TAG_2 = 2
private const val INPUT_BUFFER_SIZE = 40
private const val PRESENTATION_TIME = 100L
private const val ENCODER_BUFFER_CAPACITY = 42
private const val EXTRA_INPUT_BYTES = 10
private const val SOURCE_SAMPLE_RATE = 44100
private const val TARGET_SAMPLE_RATE = 44100

class AudioRendererShould {

    @Mock private lateinit var encoder: Encoder

    private val encoderInputFrame1 =
        Frame(OUTPUT_FRAME_TAG_1, ByteBuffer.allocate(ENCODER_BUFFER_CAPACITY), MediaCodec.BufferInfo())

    private val encoderInputFrame2 =
        Frame(OUTPUT_FRAME_TAG_2, ByteBuffer.allocate(ENCODER_BUFFER_CAPACITY), MediaCodec.BufferInfo())

    private lateinit var audioRenderer: AudioRenderer

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(encoder.dequeueInputFrame(any())).thenReturn(OUTPUT_FRAME_TAG_1).thenReturn(OUTPUT_FRAME_TAG_2)
        whenever(encoder.getInputFrame(OUTPUT_FRAME_TAG_1)).thenReturn(encoderInputFrame1)
        whenever(encoder.getInputFrame(OUTPUT_FRAME_TAG_2)).thenReturn(encoderInputFrame2)

        audioRenderer = AudioRenderer(encoder)
    }

    @Test
    fun `send a copy of stereo input frame to encoder when no resampling is needed`() {
        val sourceMediaFormat = createMediaFormat(2, SOURCE_SAMPLE_RATE)
        val targetMediaFormat = createMediaFormat(2, TARGET_SAMPLE_RATE)
        audioRenderer.init(null, sourceMediaFormat, targetMediaFormat)

        val frameCollector = encoder.collectOutputFrames(1)
        val inputFrame = createFrame(INPUT_BUFFER_SIZE, PRESENTATION_TIME, 0)
        audioRenderer.renderFrame(inputFrame, PRESENTATION_TIME)

        val outputFrames = frameCollector.call()
        verify(encoder).getInputFrame(OUTPUT_FRAME_TAG_1)
        assertThat(outputFrames.size, equalTo(1))
        verifyOutputFrame(inputFrame, outputFrames[0])
    }

    @Test
    fun `send a copy of mono input frame to encoder when no resampling is needed`() {
        val sourceMediaFormat = createMediaFormat(1, SOURCE_SAMPLE_RATE)
        val targetMediaFormat = createMediaFormat(1, TARGET_SAMPLE_RATE)
        audioRenderer.init(null, sourceMediaFormat, targetMediaFormat)

        val frameCollector = encoder.collectOutputFrames(1)
        val inputFrame = createFrame(INPUT_BUFFER_SIZE, PRESENTATION_TIME, 0)
        audioRenderer.renderFrame(inputFrame, PRESENTATION_TIME)

        val outputFrames = frameCollector.call()
        verify(encoder).getInputFrame(OUTPUT_FRAME_TAG_1)
        assertThat(outputFrames.size, equalTo(1))
        verifyOutputFrame(inputFrame, outputFrames[0])
    }

    @Test
    fun `use multiple output buffers to encode large input buffer`() {
        val channelCount = 2
        val sourceMediaFormat = createMediaFormat(channelCount, SOURCE_SAMPLE_RATE)
        val targetMediaFormat = createMediaFormat(channelCount, TARGET_SAMPLE_RATE)
        audioRenderer.init(null, sourceMediaFormat, targetMediaFormat)

        val frameCollector = encoder.collectOutputFrames(2)
        val inputFrame = createFrame(
            ENCODER_BUFFER_CAPACITY + EXTRA_INPUT_BYTES,
            PRESENTATION_TIME,
            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        audioRenderer.renderFrame(inputFrame, PRESENTATION_TIME)

        val outputFrames = frameCollector.call()
        verify(encoder).getInputFrame(OUTPUT_FRAME_TAG_1)
        verify(encoder).getInputFrame(OUTPUT_FRAME_TAG_2)
        assertThat(outputFrames.size, equalTo(2))

        // first encoder input frame should be filled and start at input presentation time
        val expectedByteBuffer1 = ByteBuffer.allocate(ENCODER_BUFFER_CAPACITY)
        repeat(ENCODER_BUFFER_CAPACITY) {
            expectedByteBuffer1.put(it.toByte())
        }
        val expectedBufferInfo1 = MediaCodec.BufferInfo()
        expectedBufferInfo1.offset = 0
        expectedBufferInfo1.size = ENCODER_BUFFER_CAPACITY
        expectedBufferInfo1.presentationTimeUs = PRESENTATION_TIME
        expectedBufferInfo1.flags = 0 // EOS flag should be cleared from the first buffer
        val expectedOutputFrame1 = Frame(OUTPUT_FRAME_TAG_1, expectedByteBuffer1, expectedBufferInfo1)

        // second encoder frame should contain remaining bytes and start at adjusted presentation time
        val expectedByteBuffer2 = ByteBuffer.allocate(ENCODER_BUFFER_CAPACITY)
        repeat(EXTRA_INPUT_BYTES) {
            expectedByteBuffer2.put((it + ENCODER_BUFFER_CAPACITY).toByte())
        }
        val expectedBufferInfo2 = MediaCodec.BufferInfo()
        expectedBufferInfo2.offset = 0
        expectedBufferInfo2.size = EXTRA_INPUT_BYTES
        expectedBufferInfo2.presentationTimeUs = inputFrame.bufferInfo.presentationTimeUs +
            ((ENCODER_BUFFER_CAPACITY / (channelCount * 2)) * (1_000_000f / TARGET_SAMPLE_RATE)).toLong()
        expectedBufferInfo2.flags = inputFrame.bufferInfo.flags // EOS flag should not be cleared from the second buffer
        val expectedOutputFrame2 = Frame(OUTPUT_FRAME_TAG_2, expectedByteBuffer2, expectedBufferInfo2)

        verifyOutputFrame(expectedOutputFrame1, outputFrames[0])
        verifyOutputFrame(expectedOutputFrame2, outputFrames[1])
    }

    private fun verifyOutputFrame(expectedFrame: Frame, actualFrame: Frame) {
        // compare buffer info
        assertThat(actualFrame.bufferInfo.size, equalTo(expectedFrame.bufferInfo.size))
        assertThat(actualFrame.bufferInfo.presentationTimeUs, equalTo(expectedFrame.bufferInfo.presentationTimeUs))
        assertThat(actualFrame.bufferInfo.flags, equalTo(expectedFrame.bufferInfo.flags))

        // compare buffer contents
        assertNotNull(expectedFrame.buffer)
        assertNotNull(actualFrame.buffer)
        expectedFrame.buffer?.let { expectedBuffer ->
            actualFrame.buffer?.let { actualBuffer ->
                expectedBuffer.rewind()
                actualBuffer.rewind()
                repeat(expectedBuffer.limit()) {
                    assertThat(expectedBuffer.get(), equalTo(actualBuffer.get()))
                }
            }
        }
    }

    private fun createFrame(size: Int, presentationTime: Long, flags: Int): Frame {
        val buffer = ByteBuffer.allocate(size)
        repeat(size) {
            buffer.put(it.toByte())
        }
        buffer.flip()

        val bufferInfo = MediaCodec.BufferInfo().apply {
            this.offset = 0
            this.size = size
            this.presentationTimeUs = presentationTime
            this.flags = flags
        }

        return Frame(INPUT_FRAME_TAG, buffer, bufferInfo)
    }

    private fun createMediaFormat(channelCount: Int, sampleRate: Int): MediaFormat {
        val mediaFormat = mock<MediaFormat>()
        whenever(mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(true)
        whenever(mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(channelCount)
        whenever(mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(true)
        whenever(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(sampleRate)

        return mediaFormat
    }

    private fun Encoder.collectOutputFrames(count: Int) : Callable<List<Frame>> {
        val latch = CountDownLatch(count)
        val frames = mutableListOf<Frame>()
        whenever(this.queueInputFrame(any())).thenAnswer {
            frames.add(it.arguments[0] as Frame)
            latch.countDown()
        }
        return Callable {
            latch.await()
            frames
        }
    }
}
