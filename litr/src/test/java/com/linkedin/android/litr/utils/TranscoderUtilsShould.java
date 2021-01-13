/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.media.MediaFormat;

import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TranscoderUtilsShould {
    private static final long DURATION_S = 30;
    private static final long DURATION_US = DURATION_S * 1000 * 1000;
    private static final long MISC_DURATION_S = 25;
    private static final long MISC_DURATION_US = MISC_DURATION_S * 1000 * 1000;

    private static final long TRIM_DURATION_S = DURATION_S / 2;
    private static final long TRIM_DURATION_US = TRIM_DURATION_S * 1000 * 1000;

    private static final int VIDEO_BIT_RATE = 19 * 1000 * 1000;
    private static final int AUDIO_BIT_RATE = 96 * 1000;
    private static final int MISC_BIT_RATE = 5 * 1000;

    private static final int VIDEO_WIDTH = 1920;
    private static final int VIDEO_HEIGHT = 1080;

    @Mock private MediaSource mediaSource;
    @Mock private MediaFormat targetVideoFormat;
    @Mock private MediaFormat videoMediaFormat;
    @Mock private MediaFormat audioMediaFormat;
    @Mock private MediaFormat miscMediaFormat;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        targetVideoFormat = new MediaFormat();
        targetVideoFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        targetVideoFormat.setInteger(MediaFormat.KEY_WIDTH, 1280);
        targetVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, 720);
        targetVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3300000);
        targetVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        when(videoMediaFormat.containsKey(MediaFormat.KEY_MIME)).thenReturn(true);
        when(videoMediaFormat.getString(MediaFormat.KEY_MIME)).thenReturn("video/avc");
        when(videoMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(videoMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(VIDEO_BIT_RATE);
        when(videoMediaFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(videoMediaFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(DURATION_US);
        when(videoMediaFormat.getInteger(MediaFormat.KEY_WIDTH)).thenReturn(VIDEO_WIDTH);
        when(videoMediaFormat.getInteger(MediaFormat.KEY_HEIGHT)).thenReturn(VIDEO_HEIGHT);

        when(audioMediaFormat.containsKey(MediaFormat.KEY_MIME)).thenReturn(true);
        when(audioMediaFormat.getString(MediaFormat.KEY_MIME)).thenReturn("audio/mp4a-latm");
        when(audioMediaFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(audioMediaFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(DURATION_US);

        when(miscMediaFormat.containsKey(MediaFormat.KEY_MIME)).thenReturn(true);
        when(miscMediaFormat.getString(MediaFormat.KEY_MIME)).thenReturn("text/vnd.dvb.subtitle");

        when(mediaSource.getTrackFormat(0)).thenReturn(videoMediaFormat);
        when(mediaSource.getTrackFormat(1)).thenReturn(audioMediaFormat);
        when(mediaSource.getSelection()).thenReturn(new MediaRange(0, Long.MAX_VALUE));
    }

    @Test
    public void useTargetVideoBitrateToEstimateSizeWhenSourceBitrateIsNotAvailable() {
        when(mediaSource.getTrackCount()).thenReturn(2);
        when(videoMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);

        long referenceSize = (targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE) * DURATION_S
            + AUDIO_BIT_RATE * DURATION_S) / 8;

        long estimatedSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        assertThat(estimatedSize, is(referenceSize));
    }

    @Test
    public void useAudioTrackBitrateToEstimateSizeWhenAvailable() {
        when(mediaSource.getTrackCount()).thenReturn(2);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);

        long referenceSize = (targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE) * DURATION_S
                              + AUDIO_BIT_RATE * DURATION_S) / 8;

        long estimatedSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        assertThat(estimatedSize, is(referenceSize));
    }

    @Test
    public void useDefaultAudioTrackBitrateToEstimateSizeWhenNotAvailable() {
        when(mediaSource.getTrackCount()).thenReturn(2);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(-1);

        long referenceSize = (targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE) * DURATION_S
                             + TranscoderUtils.COMMON_AUDIO_BITRATE_KBPS * 1000 * DURATION_S) / 8;

        long estimatedSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        assertThat(estimatedSize, is(referenceSize));
    }

    @Test
    public void useMiscTrackBitrateAndDurationWhenAvailable() {
        when(mediaSource.getTrackCount()).thenReturn(3);
        when(mediaSource.getTrackFormat(2)).thenReturn(miscMediaFormat);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);
        when(miscMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(miscMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(MISC_BIT_RATE);
        when(miscMediaFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(miscMediaFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(MISC_DURATION_US);

        long referenceSize = (targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE) * DURATION_S
            + AUDIO_BIT_RATE * DURATION_S
            + MISC_BIT_RATE * MISC_DURATION_S) / 8;

        long estimatedSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        assertThat(estimatedSize, is(referenceSize));
    }

    @Test
    public void useMiscTrackBitrateAndMaxDurationWhenDurationNotAvailable() {
        when(mediaSource.getTrackCount()).thenReturn(3);
        when(mediaSource.getTrackFormat(2)).thenReturn(miscMediaFormat);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);
        when(miscMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(miscMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(MISC_BIT_RATE);
        when(miscMediaFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(false);
        when(miscMediaFormat.getLong(MediaFormat.KEY_DURATION)).thenReturn(-1L);

        long referenceSize = (targetVideoFormat.getInteger(MediaFormat.KEY_BIT_RATE) * DURATION_S
            + AUDIO_BIT_RATE * DURATION_S
            + MISC_BIT_RATE * DURATION_S) / 8;

        long estimatedSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        assertThat(estimatedSize, is(referenceSize));
    }

    @Test
    public void useVideoTrackBitrateAsEstimationWhenPresent() {
        when(mediaSource.getSize()).thenReturn(VIDEO_BIT_RATE * DURATION_S / 8);
        when(mediaSource.getTrackCount()).thenReturn(1);
        when(mediaSource.getTrackFormat(0)).thenReturn(videoMediaFormat);

        int estimatedBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, 0);

        assertThat(estimatedBitrate, is(VIDEO_BIT_RATE));
    }

    @Test
    public void estimateVideoTrackBitrateWhenSingleVideoTrack() {
        when(mediaSource.getSize()).thenReturn(VIDEO_BIT_RATE * DURATION_S / 8);
        when(mediaSource.getTrackCount()).thenReturn(1);
        when(mediaSource.getTrackFormat(0)).thenReturn(videoMediaFormat);
        when(videoMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);

        int estimatedBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, 0);

        assertThat(estimatedBitrate, is(VIDEO_BIT_RATE));
    }

    @Test
    public void estimateVideoTrackBitrateWhenSingleVideoAndSingleAudioTracks() {
        long size = (VIDEO_BIT_RATE * DURATION_S + AUDIO_BIT_RATE * DURATION_S) / 8;
        when(mediaSource.getSize()).thenReturn(size);
        when(mediaSource.getTrackCount()).thenReturn(2);
        when(mediaSource.getTrackFormat(0)).thenReturn(videoMediaFormat);
        when(mediaSource.getTrackFormat(1)).thenReturn(audioMediaFormat);
        when(videoMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);

        int estimatedBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, 0);

        assertThat(estimatedBitrate, is(VIDEO_BIT_RATE));
    }

    @Test
    public void estimateVideoTrackBitrateWhenSingleVideoAndSingleAudioAndSingleMiscTracks() {
        long size = (VIDEO_BIT_RATE * DURATION_S + AUDIO_BIT_RATE * DURATION_S + MISC_BIT_RATE * DURATION_S) / 8;
        when(mediaSource.getSize()).thenReturn(size);
        when(mediaSource.getTrackCount()).thenReturn(3);
        when(mediaSource.getTrackFormat(0)).thenReturn(videoMediaFormat);
        when(mediaSource.getTrackFormat(1)).thenReturn(audioMediaFormat);
        when(mediaSource.getTrackFormat(2)).thenReturn(miscMediaFormat);
        when(videoMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);
        when(miscMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);

        int estimatedBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, 0);

        assertThat(estimatedBitrate, is(VIDEO_BIT_RATE + MISC_BIT_RATE));
    }

    @Test
    public void estimateVideoTrackBitrateWhenMultipleVideoTracks() {
        int videoWidth2 = 1280;
        int videoHeight2 = 720;
        int videoDuration2 = 25;
        long videoDurationUs2 = videoDuration2 * 1000 * 1000;

        long videoBitrate2 = ((long) videoWidth2 * videoHeight2 * VIDEO_BIT_RATE) / (VIDEO_WIDTH * VIDEO_HEIGHT);

        MediaFormat videoMediaFormat2 = mock(MediaFormat.class);
        when(videoMediaFormat2.containsKey(MediaFormat.KEY_MIME)).thenReturn(true);
        when(videoMediaFormat2.getString(MediaFormat.KEY_MIME)).thenReturn("video/avc");
        when(videoMediaFormat2.containsKey(MediaFormat.KEY_DURATION)).thenReturn(true);
        when(videoMediaFormat2.getLong(MediaFormat.KEY_DURATION)).thenReturn(videoDurationUs2);
        when(videoMediaFormat2.getInteger(MediaFormat.KEY_WIDTH)).thenReturn(videoWidth2);
        when(videoMediaFormat2.getInteger(MediaFormat.KEY_HEIGHT)).thenReturn(videoHeight2);
        when(videoMediaFormat2.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);

        when(videoMediaFormat.containsKey(MediaFormat.KEY_DURATION)).thenReturn(false);

        long size = (VIDEO_BIT_RATE * DURATION_S + videoBitrate2 * videoDuration2) / 8;
        when(mediaSource.getSize()).thenReturn(size);
        when(mediaSource.getTrackCount()).thenReturn(2);
        when(mediaSource.getTrackFormat(0)).thenReturn(videoMediaFormat);
        when(mediaSource.getTrackFormat(1)).thenReturn(videoMediaFormat2);

        int estimatedBitrate = TranscoderUtils.estimateVideoTrackBitrate(mediaSource, 0);

        // estimation is affected by integer division, so let's account for that
        assertEquals(estimatedBitrate, VIDEO_BIT_RATE, 1);
    }


    @Test
    public void useMediaRangeToEstimateSizeWhenAvailable() {
        when(mediaSource.getTrackCount()).thenReturn(2);
        when(videoMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(false);
        when(audioMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)).thenReturn(true);
        when(audioMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)).thenReturn(AUDIO_BIT_RATE);

        // Trimmer has not been set (use the default MediaRange object)
        when(mediaSource.getSelection()).thenReturn(new MediaRange(0, Long.MAX_VALUE));
        long defaultSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        // Trimmer has been set, trim duration here is half of our track length
        when(mediaSource.getSelection()).thenReturn(new MediaRange(0, TRIM_DURATION_US));
        long trimmedSize = TranscoderUtils.getEstimatedTargetVideoFileSize(mediaSource, targetVideoFormat, null);

        // Since we trim half of the video, the estimated size should be half of the untrimmed one
        assertThat(trimmedSize, is(defaultSize / 2));
    }
}
