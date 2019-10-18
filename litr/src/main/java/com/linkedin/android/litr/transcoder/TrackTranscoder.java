/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.transcoder;

import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class TrackTranscoder {
    public static final int NO_SELECTED_TRACK = -1;
    public static final int UNDEFINED_VALUE = -1;

    public static final int ERROR_TRANSCODER_NOT_RUNNING = -3;

    public static final int RESULT_OUTPUT_MEDIA_FORMAT_CHANGED = 1;
    public static final int RESULT_FRAME_PROCESSED = 2;
    public static final int RESULT_EOS_REACHED = 3;

    protected static final String KEY_ROTATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                               ? MediaFormat.KEY_ROTATION
                                               : "rotation-degrees";

    @NonNull protected final MediaSource mediaSource;
    @NonNull protected final MediaTarget mediaMuxer;
    @NonNull protected final Decoder decoder;
    @NonNull protected final Encoder encoder;

    protected int sourceTrack;
    protected int targetTrack;

    @Nullable protected MediaFormat targetFormat;

    protected float duration = UNDEFINED_VALUE;
    protected float progress;

    TrackTranscoder(@NonNull MediaSource mediaSource,
                    int sourceTrack,
                    @NonNull MediaTarget mediaTarget,
                    @Nullable MediaFormat targetFormat,
                    @NonNull Decoder decoder,
                    @NonNull Encoder encoder) {
        this.mediaSource = mediaSource;
        this.sourceTrack = sourceTrack;
        this.mediaMuxer = mediaTarget;
        this.targetFormat = targetFormat;
        this.decoder = decoder;
        this.encoder = encoder;
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

}
