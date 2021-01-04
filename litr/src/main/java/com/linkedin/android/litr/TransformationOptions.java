/*
 * Copyright 2020 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.io.MediaRange;

import java.util.List;

import static com.linkedin.android.litr.MediaTransformer.GRANULARITY_DEFAULT;
import static com.linkedin.android.litr.MediaTransformer.GRANULARITY_NONE;

/**
 * A data class which specifies different transformation options:
 *  - callback granularity (how frequently listener is called back during transformation)
 *  - video filters, in order they must be applied
 *  - source media range, if only part of {@link com.linkedin.android.litr.io.MediaSource} should be used
 */
public class TransformationOptions {
    @IntRange(from = GRANULARITY_NONE) public final int granularity;
    @Nullable public final List<GlFilter> videoFilters;
    @NonNull public final MediaRange sourceMediaRange;

    private TransformationOptions(@IntRange(from = GRANULARITY_NONE) int granularity,
                                  @Nullable List<GlFilter> videoFilters,
                                  @Nullable MediaRange sourceMediaRange) {
        this.granularity = granularity;
        this.videoFilters = videoFilters;
        this.sourceMediaRange = sourceMediaRange == null ? new MediaRange(0, Long.MAX_VALUE) : sourceMediaRange;
    }

    public static class Builder {
        private int granularity = GRANULARITY_DEFAULT;
        private List<GlFilter> videoFilters;
        private MediaRange sourceMediaRange;

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
        public Builder setSourceMediaRange(@NonNull MediaRange sourceMediaRange) {
            this.sourceMediaRange = sourceMediaRange;
            return this;
        }

        @NonNull
        public TransformationOptions build() {
            return new TransformationOptions(granularity, videoFilters, sourceMediaRange);
        }
    }
}
