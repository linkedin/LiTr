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
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.exception.TrackTranscoderException;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.VideoRenderer;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TrackTranscoderFactory {
    private static final String TAG = TrackTranscoderFactory.class.getSimpleName();

    /**
     * Create a proper transcoder for a given source track and target media format.
     *
     * @param sourceTrack       source track id
     * @param sourceMediaFormat source track media format
     * @param mediaSource    {@link MediaExtractor} for reading data from the source
     * @param mediaTarget        {@link MediaTarget} for writing data to the target
     * @param targetVideoFormat {@link MediaFormat} with target video track parameters, null if writing "as is"
     * @param targetAudioFormat {@link MediaFormat} with target audio track parameters, null if writing "as is"
     * @return implementation of {@link TrackTranscoder} for a given track
     */
    @NonNull
    public TrackTranscoder create(int sourceTrack,
                                  @NonNull MediaFormat sourceMediaFormat,
                                  @NonNull MediaSource mediaSource,
                                  @NonNull Decoder decoder,
                                  @NonNull VideoRenderer videoRenderer,
                                  @NonNull Encoder encoder,
                                  @NonNull MediaTarget mediaTarget,
                                  @Nullable MediaFormat targetVideoFormat,
                                  @Nullable MediaFormat targetAudioFormat) throws TrackTranscoderException {
        String trackMimeType = sourceMediaFormat.getString(MediaFormat.KEY_MIME);
        if (trackMimeType == null) {
            throw new TrackTranscoderException(TrackTranscoderException.Error.SOURCE_TRACK_MIME_TYPE_NOT_FOUND, sourceMediaFormat, null, null);
        }

        if (trackMimeType.startsWith("video")) {
            if (targetVideoFormat == null) {
                Log.d(TAG, "No target video format, using passthrough transcoder for video");
            } else {
                return new VideoTrackTranscoder(mediaSource,
                                                sourceTrack,
                                                mediaTarget,
                                                targetVideoFormat,
                                                videoRenderer,
                                                decoder,
                                                encoder);
            }
        } else if (trackMimeType.startsWith("audio")) {
            if (targetAudioFormat == null) {
                Log.d(TAG, "No target audio format, using passthrough transcoder for audio");
            } else {
                return new AudioTrackTranscoder(mediaSource,
                                                sourceTrack,
                                                mediaTarget,
                                                targetAudioFormat,
                                                new MediaCodecDecoder(),
                                                new MediaCodecEncoder());
            }
        } else {
            Log.e(TAG, "Unsupported track mime type: " + trackMimeType + ", will use passthrough transcoder");
        }

        return new PassthroughTranscoder(mediaSource, sourceTrack, mediaTarget);
    }
}
