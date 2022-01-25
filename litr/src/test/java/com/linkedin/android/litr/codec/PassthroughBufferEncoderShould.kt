/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.codec

import android.media.MediaCodec
import android.media.MediaFormat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val BUFFER_CAPACITY = 42

class PassthroughBufferEncoderShould {

    private lateinit var encoder: Encoder

    @Before
    fun setup() {
        encoder = PassthroughBufferEncoder(BUFFER_CAPACITY)

        // first output frame is always a media format change, so get it out of the way for main tests
        encoder.dequeueOutputFrame(0)
    }

    @Test
    fun start() {
        assertFalse(encoder.isRunning)

        encoder.start()

        assertTrue(encoder.isRunning)
    }

    @Test
    fun stop() {
        encoder.start()

        encoder.stop()

        assertFalse(encoder.isRunning)
    }

    @Test
    fun `dequeue input frame`() {
        val tag = encoder.dequeueInputFrame(0)

        assertThat(tag, equalTo(0))
    }

    @Test
    fun `return try later when frame pool is depleted`() {
        val tag1 = encoder.dequeueInputFrame(0)
        val tag2 = encoder.dequeueInputFrame(0)
        val tag3 = encoder.dequeueInputFrame(0)

        assertThat(tag1, equalTo(0))
        assertThat(tag2, equalTo(1))
        assertThat(tag3, equalTo(MediaCodec.INFO_TRY_AGAIN_LATER))
    }

    @Test
    fun `return media format change on first output frame request`() {
        val mediaFormat = MediaFormat()
        mediaFormat.setString(MediaFormat.KEY_MIME, "audio/raw")
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)

        val encoder = PassthroughBufferEncoder(BUFFER_CAPACITY)
        encoder.init(mediaFormat)

        val formatTag = encoder.dequeueOutputFrame(0)
        assertThat(formatTag, equalTo(MediaCodec.INFO_OUTPUT_FORMAT_CHANGED))
        assertThat(encoder.outputFormat, equalTo(mediaFormat))
    }

    @Test
    fun `return try later when no output frames are available`() {
        val tag = encoder.dequeueOutputFrame(0)

        assertThat(tag, equalTo(MediaCodec.INFO_TRY_AGAIN_LATER))
    }

    @Test
    fun `passthrough encode a frame`() {
        val inputTag = encoder.dequeueInputFrame(0)
        val inputFrame = encoder.getInputFrame(inputTag)
        assertNotNull(inputFrame)
        encoder.queueInputFrame(inputFrame)

        // subsequent requests will return frame tags, if available
        val outputTag = encoder.dequeueOutputFrame(0)
        val outputFrame = encoder.getOutputFrame(outputTag)

        assertThat(outputTag, equalTo(inputTag))
        assertThat(outputFrame, equalTo(inputFrame))
    }

    @Test
    fun `maintain frame ordering when encoding`() {
        val inputTag1 = encoder.dequeueInputFrame(0)
        val inputTag2 = encoder.dequeueInputFrame(0)
        val inputFrame1 = encoder.getInputFrame(inputTag1)
        assertNotNull(inputFrame1)
        val inputFrame2 = encoder.getInputFrame(inputTag2)
        assertNotNull(inputFrame2)
        encoder.queueInputFrame(inputFrame1)
        encoder.queueInputFrame(inputFrame2)

        val outputTag1 = encoder.dequeueOutputFrame(0)
        val outputFrame1 = encoder.getOutputFrame(outputTag1)
        val outputTag2 = encoder.dequeueOutputFrame(0)
        val outputFrame2 = encoder.getOutputFrame(outputTag2)

        assertThat(outputTag1, equalTo(inputTag1))
        assertThat(outputTag2, equalTo(inputTag2))
        assertThat(outputFrame1, equalTo(inputFrame1))
        assertThat(outputFrame2, equalTo(inputFrame2))
    }

    @Test
    fun `make output frame available when it is released`() {
        val inputTag1 = encoder.dequeueInputFrame(0)
        val inputTag2 = encoder.dequeueInputFrame(0)
        val inputFrame1 = encoder.getInputFrame(inputTag1)
        assertNotNull(inputFrame1)
        val inputFrame2 = encoder.getInputFrame(inputTag2)
        assertNotNull(inputFrame2)
        encoder.queueInputFrame(inputFrame1)
        encoder.queueInputFrame(inputFrame2)

        val outputTag1 = encoder.dequeueOutputFrame(0)
        val outputFrame1 = encoder.getOutputFrame(outputTag1)
        val outputTag2 = encoder.dequeueOutputFrame(0)
        val outputFrame2 = encoder.getOutputFrame(outputTag2)

        // no available input frames yet
        assertThat(encoder.dequeueInputFrame(0), equalTo(MediaCodec.INFO_TRY_AGAIN_LATER))

        encoder.releaseOutputFrame(outputTag1)

        // now frame becomes available after it is released
        assertThat(encoder.dequeueInputFrame(0), equalTo(inputTag1))
    }
}