/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.exception;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.util.Arrays;

public class TrackTranscoderException extends MediaTransformationException {

    private static final String TAG = TrackTranscoderException.class.getName();
    private static final String DECODER_FORMAT_NOT_FOUND_ERROR_TEXT = "Failed to create decoder codec.";
    private static final String DECODER_CONFIGURATION_ERROR_TEXT = "Failed to configure decoder codec.";
    private static final String ENCODER_FORMAT_NOT_FOUND_ERROR_TEXT = "Failed to create encoder codec.";
    private static final String ENCODER_CONFIGURATION_ERROR_TEXT = "Failed to configure encoder codec.";
    private static final String DECODER_NOT_FOUND_ERROR_TEXT = "No decoder found.";
    private static final String ENCODER_NOT_FOUND_ERROR_TEXT = "No encoder found.";
    private static final String CODEC_IN_RELEASED_STATE_ERROR_TEXT = "Codecs are in released state.";
    private static final String SOURCE_TRACK_MIME_TYPE_NOT_FOUND_ERROR_TEXT = "Mime type not found for the source track.";
    private static final String NO_TRACKS_FOUND_ERROR_TEXT = "No tracks found.";
    private static final String INTERNAL_CODEC_ERROR_TEXT = "Internal codec error occurred.";
    private static final String NO_FRAME_AVAILABLE_ERROR_TEXT = "No frame available for specified tag";
    private static final String DECODER_NOT_PROVIDED_TEXT = "Decoder is not provided";
    private static final String ENCODER_NOT_PROVIDED_TEXT = "Encoder is not provided";
    private static final String RENDERER_NOT_PROVIDED_TEXT = "Renderer is not provided";

    @NonNull private final Error error;
    @Nullable private final MediaFormat mediaFormat;
    @Nullable private final MediaCodec mediaCodec;
    @Nullable private final MediaCodecList mediaCodecList;

    // TODO Add track number to this exception to pass track info when track transcoders are in progress.

    public enum Error {
        DECODER_FORMAT_NOT_FOUND(DECODER_FORMAT_NOT_FOUND_ERROR_TEXT),
        DECODER_CONFIGURATION_ERROR(DECODER_CONFIGURATION_ERROR_TEXT),
        ENCODER_FORMAT_NOT_FOUND(ENCODER_FORMAT_NOT_FOUND_ERROR_TEXT),
        ENCODER_CONFIGURATION_ERROR(ENCODER_CONFIGURATION_ERROR_TEXT),
        DECODER_NOT_FOUND(DECODER_NOT_FOUND_ERROR_TEXT),
        ENCODER_NOT_FOUND(ENCODER_NOT_FOUND_ERROR_TEXT),
        CODEC_IN_RELEASED_STATE(CODEC_IN_RELEASED_STATE_ERROR_TEXT),
        SOURCE_TRACK_MIME_TYPE_NOT_FOUND(SOURCE_TRACK_MIME_TYPE_NOT_FOUND_ERROR_TEXT),
        NO_TRACKS_FOUND(NO_TRACKS_FOUND_ERROR_TEXT),
        INTERNAL_CODEC_ERROR(INTERNAL_CODEC_ERROR_TEXT),
        NO_FRAME_AVAILABLE(NO_FRAME_AVAILABLE_ERROR_TEXT),
        DECODER_NOT_PROVIDED(DECODER_NOT_PROVIDED_TEXT),
        ENCODER_NOT_PROVIDED(ENCODER_NOT_PROVIDED_TEXT),
        RENDERER_NOT_PROVIDED(RENDERER_NOT_PROVIDED_TEXT);

        private final String message;

        Error(String message) {
            this.message = message;
        }
    }

    public TrackTranscoderException(@NonNull Error error) {
        this(error, null, null, null);
    }

    public TrackTranscoderException(@NonNull Error error, @NonNull Throwable cause) {
        this(error, null, null, null, cause);
    }

    public TrackTranscoderException(@NonNull Error error,
                                    @Nullable MediaFormat sourceFormat,
                                    @Nullable MediaCodec mediaCodec,
                                    @Nullable MediaCodecList mediaCodecList) {
        this(error, sourceFormat, mediaCodec, mediaCodecList, null);
    }

    public TrackTranscoderException(@NonNull Error error,
                                    @Nullable MediaFormat sourceFormat,
                                    @Nullable MediaCodec mediaCodec,
                                    @Nullable MediaCodecList mediaCodecList,
                                    @Nullable Throwable cause) {
        super(cause);
        this.error = error;
        this.mediaFormat = sourceFormat;
        this.mediaCodec = mediaCodec;
        this.mediaCodecList = mediaCodecList;
    }

    @NonNull
    public Error getError() {
        return error;
    }

    @Override
    @NonNull
    public String getMessage() {
        return error.message;
    }

    @Override
    public String toString() {
        String ret = super.toString() + '\n';
        if (mediaFormat != null) {
            ret += "Media format: " + mediaFormat.toString() + '\n';
        }
        if (mediaCodec != null) {
            ret += "Selected media codec info: " + convertMediaCodecInfoToString(mediaCodec) + '\n';
        }
        if (mediaCodecList != null) {
            ret += "Available media codec info list (Name, IsEncoder, Supported Types): "+ convertMediaCodecListToString(mediaCodecList);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getCause() != null) {
            ret += "Diagnostic info: " + getExceptionDiagnosticInfo(getCause());
        }
        return ret;
    }

    @NonNull
    private String convertMediaCodecListToString(@NonNull MediaCodecList mediaCodecList) {
        StringBuilder builder = new StringBuilder();
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                // TODO filter supported codecs for the mime type
                for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                    if (mediaCodecInfo != null) {
                        builder.append('\n').append(convertMediaCodecInfoToString(mediaCodecInfo));
                    }
                }
            } else {
                Log.e(TAG, "Failed to retrieve media codec info below API level 21.");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to retrieve media codec info.", e);
        }
        return builder.toString();
    }

    @NonNull
    private String convertMediaCodecInfoToString(@NonNull MediaCodec mediaCodec) {
        try {
            return convertMediaCodecInfoToString(mediaCodec.getCodecInfo());
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to retrieve media codec info.");
        }
        return "";
    }

    @NonNull
    private String convertMediaCodecInfoToString(@NonNull MediaCodecInfo mediaCodecInfo) {
        return "MediaCodecInfo: "
            + mediaCodecInfo.getName() + ','
            + mediaCodecInfo.isEncoder() + ','
            + Arrays.asList(mediaCodecInfo.getSupportedTypes()).toString();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private String getExceptionDiagnosticInfo(@Nullable Throwable cause) {
        if (!(cause instanceof MediaCodec.CodecException)) {
            return null;
        }

        return ((MediaCodec.CodecException) cause).getDiagnosticInfo();
    }
}
