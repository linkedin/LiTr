/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val BUFFER_CAPACITY = 5

class PassthroughDecoderShould {

    @Mock private lateinit var surface: Surface

    private lateinit var mediaFormat: MediaFormat

    private lateinit var passthroughDecoder: PassthroughDecoder

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        mediaFormat = MediaFormat()
        mediaFormat.setString(MediaFormat.KEY_MIME, "video/avc")
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, 1280)
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, 720)
        mediaFormat.setLong(MediaFormat.KEY_DURATION, 5_000_000L)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3_300_000)

        passthroughDecoder = PassthroughDecoder(BUFFER_CAPACITY)
    }

    @Test
    fun start() {
        assertFalse(passthroughDecoder.isRunning)

        passthroughDecoder.start()

        assertTrue(passthroughDecoder.isRunning)
    }

    @Test
    fun stop() {
        passthroughDecoder.start()

        passthroughDecoder.stop()

        assertFalse(passthroughDecoder.isRunning)
    }

    @Test
    fun `dequeue input frame`() {
        val tag = passthroughDecoder.dequeueInputFrame(0)

        assertThat(tag, equalTo(0))
    }

    @Test
    fun `return try later when frame pool is depleted`() {
        val tag1 = passthroughDecoder.dequeueInputFrame(0)
        val tag2 = passthroughDecoder.dequeueInputFrame(0)
        val tag3 = passthroughDecoder.dequeueInputFrame(0)

        assertThat(tag1, equalTo(0))
        assertThat(tag2, equalTo(1))
        assertThat(tag3, equalTo(MediaCodec.INFO_TRY_AGAIN_LATER))
    }

    @Test
    fun `return try later when no decoded frames are available`() {
        val tag = passthroughDecoder.dequeueOutputFrame(0)

        assertThat(tag, equalTo(MediaCodec.INFO_TRY_AGAIN_LATER))
    }

    @Test
    fun `passthrough decode an encoded frame`() {
        val inputTag = passthroughDecoder.dequeueInputFrame(0)
        val inputFrame = passthroughDecoder.getInputFrame(inputTag)
        assertNotNull(inputFrame)
        passthroughDecoder.queueInputFrame(inputFrame)

        val outputTag = passthroughDecoder.dequeueOutputFrame(0)
        val outputFrame = passthroughDecoder.getOutputFrame(outputTag)

        assertThat(outputTag, equalTo(inputTag))
        assertThat(outputFrame, equalTo(inputFrame))
    }

    @Test
    fun `maintain frame ordering when decoding`() {
        val inputTag1 = passthroughDecoder.dequeueInputFrame(0)
        val inputTag2 = passthroughDecoder.dequeueInputFrame(0)
        val inputFrame1 = passthroughDecoder.getInputFrame(inputTag1)
        assertNotNull(inputFrame1)
        val inputFrame2 = passthroughDecoder.getInputFrame(inputTag2)
        assertNotNull(inputFrame2)
        passthroughDecoder.queueInputFrame(inputFrame1)
        passthroughDecoder.queueInputFrame(inputFrame2)

        val outputTag1 = passthroughDecoder.dequeueOutputFrame(0)
        val outputFrame1 = passthroughDecoder.getOutputFrame(outputTag1)
        val outputTag2 = passthroughDecoder.dequeueOutputFrame(0)
        val outputFrame2 = passthroughDecoder.getOutputFrame(outputTag2)

        assertThat(outputTag1, equalTo(inputTag1))
        assertThat(outputTag2, equalTo(inputTag2))
        assertThat(outputFrame1, equalTo(inputFrame1))
        assertThat(outputFrame2, equalTo(inputFrame2))
    }

    @Test
    fun `make output frame available when it is released`() {
        val inputTag1 = passthroughDecoder.dequeueInputFrame(0)
        val inputTag2 = passthroughDecoder.dequeueInputFrame(0)
        val inputFrame1 = passthroughDecoder.getInputFrame(inputTag1)
        assertNotNull(inputFrame1)
        val inputFrame2 = passthroughDecoder.getInputFrame(inputTag2)
        assertNotNull(inputFrame2)
        passthroughDecoder.queueInputFrame(inputFrame1)
        passthroughDecoder.queueInputFrame(inputFrame2)

        val outputTag1 = passthroughDecoder.dequeueOutputFrame(0)
        val outputFrame1 = passthroughDecoder.getOutputFrame(outputTag1)
        val outputTag2 = passthroughDecoder.dequeueOutputFrame(0)
        val outputFrame2 = passthroughDecoder.getOutputFrame(outputTag2)

        // no available input frames yet
        assertThat(passthroughDecoder.dequeueInputFrame(0), equalTo(MediaCodec.INFO_TRY_AGAIN_LATER))

        passthroughDecoder.releaseOutputFrame(outputTag1, true)

        // now frame becomes available after it is released
        assertThat(passthroughDecoder.dequeueInputFrame(0), equalTo(inputTag1))
    }

    @Test
    fun `invoke GL render call when output frame is released and render is requested`() {
        passthroughDecoder.init(mediaFormat, surface)

        passthroughDecoder.releaseOutputFrame(0, true)

        verify(surface).lockCanvas(any())
        verify(surface).unlockCanvasAndPost(any())
    }

    @Test
    fun `not invoke GL render call when output frame is released and render is not requested`() {
        passthroughDecoder.init(mediaFormat, surface)

        passthroughDecoder.releaseOutputFrame(0, false)

        verify(surface, never()).lockCanvas(any())
        verify(surface, never()).unlockCanvasAndPost(any())
    }

    @Test
    fun `not invoke GL render call when running in software rendering mode`() {
        passthroughDecoder.init(mediaFormat, null)

        passthroughDecoder.releaseOutputFrame(0, true)

        verify(surface, never()).lockCanvas(any())
        verify(surface, never()).unlockCanvasAndPost(any())
    }
}