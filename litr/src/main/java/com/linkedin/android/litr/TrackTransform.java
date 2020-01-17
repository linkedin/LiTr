/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import android.media.MediaFormat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.Renderer;

/**
 * Transformation instruction for a specific track. Must be constructed using a {@link Builder}.
 */
public class TrackTransform {
    private final MediaSource mediaSource;
    private final Decoder decoder;
    private final Renderer renderer;
    private final Encoder encoder;
    private final MediaTarget mediaTarget;

    private final MediaFormat targetFormat;
    private final int sourceTrack;
    private final int targetTrack;

    private TrackTransform(@NonNull MediaSource mediaSource,
                           @Nullable Decoder decoder,
                           @Nullable Renderer renderer,
                           @Nullable Encoder encoder,
                           @NonNull MediaTarget mediaTarget,
                           @Nullable MediaFormat targetFormat,
                           int sourceTrack,
                           int targetTrack) {
        this.mediaSource = mediaSource;
        this.decoder = decoder;
        this.renderer = renderer;
        this.encoder = encoder;
        this.mediaTarget = mediaTarget;
        this.targetFormat = targetFormat;
        this.sourceTrack = sourceTrack;
        this.targetTrack = targetTrack;
    }

    /**
     * Get {@link MediaSource}
     * @return {@link MediaSource} for a track
     */
    @NonNull
    public MediaSource getMediaSource() {
        return mediaSource;
    }

    /**
     * Get {@link Decoder}
     * @return {@link Decoder} for a track, null if none
     */
    @Nullable
    public Decoder getDecoder() {
        return decoder;
    }

    /**
     * Get {@link Renderer} for a track
     * @return {@link Renderer} for a track, null if none
     */
    @Nullable
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Get {@link Encoder} for a track
     * @return {@link Encoder} for a track, null if none
     */
    @Nullable
    public Encoder getEncoder() {
        return encoder;
    }

    /**
     * get {@link MediaTarget} for a track
     * @return {@link MediaTarget} for a track
     */
    @NonNull
    public MediaTarget getMediaTarget() {
        return mediaTarget;
    }

    /**
     * Get target {@link MediaFormat} for a track
     * @return target format, null if none
     */
    @Nullable
    public MediaFormat getTargetFormat() {
        return targetFormat;
    }

    /**
     * Track index of a source track in {@link MediaSource}
     * @return source track index
     */
    public int getSourceTrack() {
        return sourceTrack;
    }

    /**
     * Track index of a target track in {@link MediaTarget}
     * @return target track index
     */
    public int getTargetTrack() {
        return targetTrack;
    }

    public static class Builder {

        private final MediaSource mediaSource;
        private final int sourceTrack;
        private final MediaTarget mediaTarget;

        private Decoder decoder;
        private Renderer renderer;
        private Encoder encoder;
        private MediaFormat targetFormat;
        private int targetTrack;

        public Builder(@NonNull MediaSource mediaSource,
                       int sourceTrack,
                       @NonNull MediaTarget mediaTarget) {
            this.mediaSource = mediaSource;
            this.sourceTrack = sourceTrack;
            this.mediaTarget = mediaTarget;

            // unless specified, target track defaults to source track
            this.targetTrack = sourceTrack;
        }

        @NonNull
        public Builder setDecoder(@Nullable Decoder decoder) {
            this.decoder = decoder;
            return this;
        }

        @NonNull
        public Builder setRenderer(@Nullable Renderer renderer) {
            this.renderer = renderer;
            return this;
        }

        @NonNull
        public Builder setEncoder(@Nullable Encoder encoder) {
            this.encoder = encoder;
            return this;
        }

        @NonNull
        public Builder setTargetFormat(@Nullable MediaFormat targetFormat) {
            this.targetFormat = targetFormat;
            return this;
        }

        @NonNull
        public Builder setTargetTrack(int targetTrack) {
            this.targetTrack = targetTrack;
            return this;
        }

        @NonNull
        public TrackTransform build() {
            return new TrackTransform(mediaSource,
                                      decoder,
                                      renderer,
                                      encoder,
                                      mediaTarget,
                                      targetFormat,
                                      sourceTrack,
                                      targetTrack);
        }
    }
}
