/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.transcoder;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.linkedin.android.litr.codec.Decoder;
import com.linkedin.android.litr.codec.Encoder;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.PassthroughSoftwareRenderer;
import com.linkedin.android.litr.render.Renderer;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TrackTranscoderFactory {
    private static final String TAG = TrackTranscoderFactory.class.getSimpleName();

    /**
     * Create a proper transcoder for a given source track and target media format.
     *
     * @param sourceTrack  source track id
     * @param mediaSource  {@link MediaExtractor} for reading data from the source
     * @param mediaTarget  {@link MediaTarget} for writing data to the target
     * @param targetFormat {@link MediaFormat} with target video track parameters, null if writing "as is"
     * @return implementation of {@link TrackTranscoder} for a given track
     */
    @NonNull
    public TrackTranscoder create(int sourceTrack,
                                  int targetTrack,
                                  @NonNull MediaSource mediaSource,
                                  @Nullable Decoder decoder,
                                  @Nullable Renderer renderer,
                                  @Nullable Encoder encoder,
                                  @NonNull MediaTarget mediaTarget,
                                  @Nullable MediaFormat targetFormat) throws TrackTranscoderException {
        if (targetFormat == null) {
            return new PassthroughTranscoder(mediaSource, sourceTrack, mediaTarget, targetTrack);
        }

        String trackMimeType = targetFormat.getString(MediaFormat.KEY_MIME);
        if (trackMimeType == null) {
            throw new TrackTranscoderException(TrackTranscoderException.Error.SOURCE_TRACK_MIME_TYPE_NOT_FOUND, targetFormat, null, null);
        }

        if (trackMimeType.startsWith("video") || trackMimeType.startsWith("audio")) {
            if (decoder == null) {
                throw new TrackTranscoderException(TrackTranscoderException.Error.DECODER_NOT_PROVIDED,
                                                   targetFormat,
                                                   null,
                                                   null);
            } else if (encoder == null) {
                throw new TrackTranscoderException(TrackTranscoderException.Error.ENCODER_NOT_PROVIDED,
                                                   targetFormat,
                                                   null,
                                                   null);
            }
        }

        if (trackMimeType.startsWith("video")) {
            if (renderer == null) {
                throw new TrackTranscoderException(TrackTranscoderException.Error.RENDERER_NOT_PROVIDED,
                        targetFormat,
                        null,
                        null);
            }
            return new VideoTrackTranscoder(mediaSource,
                                            sourceTrack,
                                            mediaTarget,
                                            targetTrack,
                                            targetFormat,
                                            renderer,
                                            decoder,
                                            encoder);
        } else if (trackMimeType.startsWith("audio")) {
            Renderer audioRenderer = renderer == null
                    ? new PassthroughSoftwareRenderer(encoder)
                    : renderer;

            return new AudioTrackTranscoder(mediaSource,
                                            sourceTrack,
                                            mediaTarget,
                                            targetTrack,
                                            targetFormat,
                                            audioRenderer,
                                            decoder,
                                            encoder);
        } else {
            Log.i(TAG, "Unsupported track mime type: " + trackMimeType + ", will use passthrough transcoder");
            return new PassthroughTranscoder(mediaSource, sourceTrack, mediaTarget, targetTrack);
        }
    }
}
