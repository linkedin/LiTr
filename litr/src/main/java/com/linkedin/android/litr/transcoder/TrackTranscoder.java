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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaRange;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.Renderer;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class TrackTranscoder {
    public static final int NO_SELECTED_TRACK = -1;
    public static final int UNDEFINED_VALUE = -1;

    public static final int ERROR_TRANSCODER_NOT_RUNNING = -3;

    public static final int RESULT_OUTPUT_MEDIA_FORMAT_CHANGED = 1;
    public static final int RESULT_FRAME_PROCESSED = 2;
    public static final int RESULT_FRAME_SKIPPED = 3;
    public static final int RESULT_EOS_REACHED = 4;

    @NonNull protected final MediaSource mediaSource;
    @NonNull protected final MediaTarget mediaMuxer;
    @Nullable protected final Renderer renderer;
    @Nullable protected final Decoder decoder;
    @Nullable protected final Encoder encoder;
    @NonNull protected final MediaRange sourceMediaSelection;

    protected int sourceTrack;
    protected int targetTrack;

    protected boolean targetTrackAdded;

    @Nullable protected MediaFormat targetFormat;

    protected long duration = UNDEFINED_VALUE;
    protected float progress;

    TrackTranscoder(@NonNull MediaSource mediaSource,
                    int sourceTrack,
                    @NonNull MediaTarget mediaTarget,
                    int targetTrack,
                    @Nullable MediaFormat targetFormat,
                    @Nullable Renderer renderer,
                    @Nullable Decoder decoder,
                    @Nullable Encoder encoder) {
        this.mediaSource = mediaSource;
        this.sourceTrack = sourceTrack;
        this.targetTrack = targetTrack;
        this.mediaMuxer = mediaTarget;
        this.targetFormat = targetFormat;
        this.renderer = renderer;
        this.decoder = decoder;
        this.encoder = encoder;
        this.sourceMediaSelection = mediaSource.getSelection();

        MediaFormat sourceMedia = mediaSource.getTrackFormat(sourceTrack);
        if (sourceMedia.containsKey(MediaFormat.KEY_DURATION)) {
            duration = sourceMedia.getLong(MediaFormat.KEY_DURATION);
            if (targetFormat != null) {
                targetFormat.setLong(MediaFormat.KEY_DURATION, duration);
            }
        }


        if (sourceMediaSelection.getEnd() < sourceMediaSelection.getStart()) {
            throw new IllegalArgumentException("Range end should be greater than range start");
        }

        // adjust for range
        duration = Math.min(duration, sourceMediaSelection.getEnd());
        duration -= sourceMediaSelection.getStart();
    }

    public abstract void start() throws TrackTranscoderException;

    public abstract int processNextFrame() throws TrackTranscoderException;

    public abstract void stop();

    public int getSourceTrack() {
        return sourceTrack;
    }

    public int getTargetTrack() {
        return targetTrack;
    }

    public float getProgress() {
        return progress;
    }

    @NonNull
    public String getEncoderName() throws TrackTranscoderException {
        return encoder.getName();
    }

    @NonNull
    public String getDecoderName() throws TrackTranscoderException {
        return decoder.getName();
    }

    @NonNull
    public MediaFormat getTargetMediaFormat() {
        return targetFormat;
    }

    protected void advanceToNextTrack() {
        // done with this track, advance until track switches to let other track transcoders finish work
        while (mediaSource.getSampleTrackIndex() == sourceTrack) {
            mediaSource.advance();
            if ((mediaSource.getSampleFlags() & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // reached the end of container, no more tracks left
                return;
            }
        }
    }

}
