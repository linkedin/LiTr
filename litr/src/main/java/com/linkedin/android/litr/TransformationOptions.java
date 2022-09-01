/*
 * Copyright 2020 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import static com.linkedin.android.litr.MediaTransformer.GRANULARITY_DEFAULT;
import static com.linkedin.android.litr.MediaTransformer.GRANULARITY_NONE;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.BufferFilter;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.io.MediaRange;

import java.util.List;

/**
 * A data class which specifies different transformation options:
 *  - callback granularity (how frequently listener is called back during transformation)
 *  - video filters, in order they must be applied
 *  - source media range, if only part of {@link com.linkedin.android.litr.io.MediaSource} should be used
 *  - ability to mute video by removing audio track(s)
 */
public class TransformationOptions {
    @IntRange(from = GRANULARITY_NONE) public final int granularity;
    @Nullable public final List<GlFilter> videoFilters;
    @Nullable public final List<BufferFilter> audioFilters;
    @NonNull public final MediaRange sourceMediaRange;
    public final boolean removeAudio;

    private TransformationOptions(@IntRange(from = GRANULARITY_NONE) int granularity,
                                  @Nullable List<GlFilter> videoFilters,
                                  @Nullable List<BufferFilter> audioFilters,
                                  @Nullable MediaRange sourceMediaRange,
                                  boolean removeAudio) {
        this.granularity = granularity;
        this.videoFilters = videoFilters;
        this.audioFilters = audioFilters;
        this.sourceMediaRange = sourceMediaRange == null ? new MediaRange(0, Long.MAX_VALUE) : sourceMediaRange;
        this.removeAudio = removeAudio;
    }

    public static class Builder {
        private int granularity = GRANULARITY_DEFAULT;
        private List<GlFilter> videoFilters;
        private List<BufferFilter> audioFilters;
        private MediaRange sourceMediaRange;
        private boolean removeAudio;

        @NonNull
        public Builder setGranularity(@IntRange(from = GRANULARITY_NONE) int granularity) {
            this.granularity = granularity;
            return this;
        }

        @NonNull
        public Builder setVideoFilters(@Nullable List<GlFilter> videoFilters) {
            this.videoFilters = videoFilters;
            return this;
        }

        @NonNull
        public Builder setAudioFilters(@Nullable List<BufferFilter> audioFilters) {
            this.audioFilters = audioFilters;
            return this;
        }

        @NonNull
        public Builder setSourceMediaRange(@NonNull MediaRange sourceMediaRange) {
            this.sourceMediaRange = sourceMediaRange;
            return this;
        }

        @NonNull
        public Builder setRemoveAudio(boolean removeAudio) {
            this.removeAudio = removeAudio;
            return this;
        }

        @NonNull
        public TransformationOptions build() {
            return new TransformationOptions(granularity, videoFilters, audioFilters, sourceMediaRange, removeAudio);
        }
    }
}
