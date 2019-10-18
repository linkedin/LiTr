/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.analytics;

import android.media.MediaFormat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class which stores information about transformation for a particular track
 */
public class TrackTransformationInfo {
    public static final long UNKNOWN_VALUE = -1;

    @NonNull private MediaFormat sourceFormat;
    @Nullable private MediaFormat targetFormat;
    @Nullable private String decoderCodec;
    @Nullable private String encoderCodec;
    private long duration = UNKNOWN_VALUE;

    @NonNull
    public MediaFormat getSourceFormat() {
        return sourceFormat;
    }

    @Nullable
    public MediaFormat getTargetFormat() {
        return targetFormat;
    }

    @Nullable
    public String getDecoderCodec() {
        return decoderCodec;
    }

    @Nullable
    public String getEncoderCodec() {
        return encoderCodec;
    }

    public long getDuration() {
        return duration;
    }

    public void setSourceFormat(@NonNull MediaFormat sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public void setTargetFormat(@Nullable MediaFormat targetFormat) {
        this.targetFormat = targetFormat;
    }

    public void setDecoderCodec(@Nullable String decoderCodec) {
        this.decoderCodec = decoderCodec;
    }

    public void setEncoderCodec(@Nullable String encoderCodec) {
        this.encoderCodec = encoderCodec;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
