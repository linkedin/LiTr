/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.linkedin.android.litr.exception.TrackTranscoderException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class CodecUtils {

    public static final int UNDEFINED_VALUE = -1;

    public static final String MIME_TYPE_VIDEO_AV1 = "video/av01";
    public static final String MIME_TYPE_VIDEO_AVC = "video/avc";
    public static final String MIME_TYPE_VIDEO_HEVC = "video/hevc";
    public static final String MIME_TYPE_VIDEO_VP8 = "video/x-vnd.on2.vp8";
    public static final String MIME_TYPE_VIDEO_VP9 = "video/x-vnd.on2.vp9";

    private static Map<String, int[]> CODEC_PROFILE_RANK_MAP = new HashMap<>();

    static {
        int[] avcProfileRanks = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                ?  new int[] {
                    MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileExtended,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileMain,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444}
                :  new int[] {
                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileExtended,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileMain,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444};
        CODEC_PROFILE_RANK_MAP.put(MIME_TYPE_VIDEO_AVC, avcProfileRanks);

        int[] vp8ProfileRanks = new int[] {
                MediaCodecInfo.CodecProfileLevel.VP8ProfileMain
        };
        CODEC_PROFILE_RANK_MAP.put(MIME_TYPE_VIDEO_VP8, vp8ProfileRanks);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int[] hevcProfileRanks = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new int[] {
                            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain,
                            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
                            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
                            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus}
                    : Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            ? new int[] {
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain,
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10}
                            : new int[] {
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain,
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10};
            CODEC_PROFILE_RANK_MAP.put(MIME_TYPE_VIDEO_HEVC, hevcProfileRanks);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            int[] vp9ProfileRanks = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new int[] {
                            MediaCodecInfo.CodecProfileLevel.VP9Profile0,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile1,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile2,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile3,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR,
                            MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR10Plus}
                    : new int[] {
                        MediaCodecInfo.CodecProfileLevel.VP9Profile0,
                        MediaCodecInfo.CodecProfileLevel.VP9Profile1,
                        MediaCodecInfo.CodecProfileLevel.VP9Profile2,
                        MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR,
                        MediaCodecInfo.CodecProfileLevel.VP9Profile3,
                        MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR};
            CODEC_PROFILE_RANK_MAP.put(MIME_TYPE_VIDEO_VP9, vp9ProfileRanks);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int[] av1ProfileRanks = new int[] {
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8,
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10,
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10,
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus
            };
            CODEC_PROFILE_RANK_MAP.put(MIME_TYPE_VIDEO_AV1, av1ProfileRanks);
        }
    }

    private CodecUtils() {}

    /**
     * Attempts to find highest supported codec profile for a given MIME type. Iterates through all codecs available,
     * filters for codecs that support provided MIME type and picks highest profile available among them.
     * Works only on API level 21 and above (Lollipop +)
     * @param mimeType media MIME type
     * @param isEncoder search through encoder codecs if true, decoder codecs if false
     * @return a {@link android.media.MediaCodecInfo.CodecProfileLevel} profile constant of successful, UNDEFINED_VALUE otherwise
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static int getHighestSupportedProfile(@NonNull String mimeType, boolean isEncoder) {
        return getHighestSupportedProfile(mimeType, isEncoder, UNDEFINED_VALUE);
    }

    /**
     * Attempts to find highest supported codec profile for a given MIME type. Iterates through all codecs available,
     * filters for codecs that support provided MIME type and picks highest profile available among them.
     * Works only on API level 21 and above (Lollipop +)
     * @param mimeType media MIME type
     * @param isEncoder search through encoder codecs if true, decoder codecs if false
     * @param targetProfile optional upper limit for codec profile, one of {@link android.media.MediaCodecInfo.CodecProfileLevel} profile constants,
     *                      or UNDEFINED_VALUE for highest supported codec profile
     * @return a {@link android.media.MediaCodecInfo.CodecProfileLevel} profile constant of successful, UNDEFINED_VALUE otherwise
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static int getHighestSupportedProfile(@NonNull String mimeType, boolean isEncoder, int targetProfile) {
        int highestSupportedProfile = UNDEFINED_VALUE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int maxProfileRank = targetProfile == UNDEFINED_VALUE ? Integer.MAX_VALUE : getProfileRank(mimeType, targetProfile);

            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                if (supportsType(mediaCodecInfo, mimeType) && mediaCodecInfo.isEncoder() == isEncoder) {
                    MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(mimeType);
                    for (MediaCodecInfo.CodecProfileLevel codecProfileLevel : codecCapabilities.profileLevels) {
                        if (getProfileRank(mimeType, codecProfileLevel.profile) > getProfileRank(mimeType, highestSupportedProfile)
                                && getProfileRank(mimeType, codecProfileLevel.profile) <= maxProfileRank) {
                            highestSupportedProfile = codecProfileLevel.profile;
                        }
                    }
                }
            }
        }

        return highestSupportedProfile;
    }

    /**
     * Get and configure {@link MediaCodec} for provided parameters
     * @param mediaFormat {@link MediaFormat} for which to get the codec
     * @param surface optional {@link Surface} which codec with work will
     * @param isEncoder flag indicating if encoder codec is requested
     * @param codecNotFoundError message to provide in {@link TrackTranscoderException} if codec could not be found
     * @param codecFormatNotFoundError message to provide in {@link TrackTranscoderException} if codec could not be found by format
     * @param codecConfigurationError message to provide in {@link TrackTranscoderException} if codec could not configured
     * @return configured instance of {@link MediaCodec}, or a {@link TrackTranscoderException} will be thrown
     */
    @NonNull
    public static MediaCodec getAndConfigureCodec(@NonNull MediaFormat mediaFormat,
                                                  @Nullable Surface surface,
                                                  boolean isEncoder,
                                                  @NonNull TrackTranscoderException.Error codecNotFoundError,
                                                  @NonNull TrackTranscoderException.Error codecFormatNotFoundError,
                                                  @NonNull TrackTranscoderException.Error codecConfigurationError) throws TrackTranscoderException {
        MediaCodec mediaCodec;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaCodec = getAndConfigureCodecByConfig(mediaFormat, surface, isEncoder);
            } else {
                mediaCodec = getAndConfigureCodecByType(mediaFormat, surface, isEncoder);
            }
            if (mediaCodec == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    throw new IllegalStateException("Try fallbackToGetCodecByType");
                } else {
                    throw new TrackTranscoderException(codecNotFoundError, mediaFormat, null, null);
                }
            }
            return mediaCodec;
        } catch (IOException | IllegalStateException e) {
            Exception exception = e;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                try {
                    mediaCodec = getAndConfigureCodecByType(mediaFormat, surface, isEncoder);
                    if (mediaCodec == null) {
                        throw new TrackTranscoderException(codecNotFoundError, mediaFormat, null, null);
                    }
                    return mediaCodec;
                } catch (IOException | IllegalStateException ex) {
                    exception = ex;
                }
            }
            if (exception instanceof IOException) {
                throw new TrackTranscoderException(codecFormatNotFoundError, mediaFormat, null, null, exception);
            } else {
                throw new TrackTranscoderException(codecConfigurationError, mediaFormat, null, null, exception);
            }
        }
    }

    @Nullable
    private static MediaCodec getAndConfigureCodecByType(@NonNull MediaFormat mediaFormat,
                                                         @Nullable Surface surface,
                                                         boolean isEncoder) throws IOException, IllegalStateException {
        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec mediaCodec = null;
        List<Callable<MediaCodec>> supportedMediaCodecs = findCodecForFormatOrType(isEncoder, mimeType, null);
        if (!supportedMediaCodecs.isEmpty()) {
            mediaCodec = createAndConfigureCodec(mediaFormat, surface, isEncoder, supportedMediaCodecs);
        }

        return mediaCodec;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private static MediaCodec getAndConfigureCodecByConfig(@NonNull MediaFormat mediaFormat,
                                                           @Nullable Surface surface,
                                                           boolean isEncoder) throws IOException, IllegalStateException {
        MediaCodec mediaCodec = null;
        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        List<Callable<MediaCodec>> supportedMediaCodecs = findCodecForFormatOrType(isEncoder, mimeType,
                mediaFormat);
        if (!supportedMediaCodecs.isEmpty()) {
            mediaCodec = createAndConfigureCodec(mediaFormat, surface, isEncoder, supportedMediaCodecs);
        }

        return mediaCodec;
    }

    @NonNull
    private static MediaCodec createAndConfigureCodec(
            @NonNull MediaFormat mediaFormat,
            @Nullable Surface surface,
            boolean isEncoder,
            @NonNull List<Callable<MediaCodec>> supportedMediaCodecs) throws IllegalStateException, IOException {

        MediaCodec mediaCodec = null;
        IOException error = null;
        for (Callable<MediaCodec> callable : supportedMediaCodecs) {
            try {
                mediaCodec = callable.call();
                if (mediaCodec != null) {
                    configureMediaFormat(mediaCodec, mediaFormat, surface, isEncoder);
                    break;
                }
            } catch (Exception e) {
                if (mediaCodec != null) {
                    mediaCodec.release();
                    mediaCodec = null;
                }
                if (e instanceof IOException) {
                    error = (IOException) e;
                }
            }
        }

        if (mediaCodec == null) {
            if (error != null) {
                throw error;
            } else {
                throw new IllegalStateException();
            }
        }
        return mediaCodec;
    }

    private static void configureMediaFormat(@NonNull MediaCodec mediaCodec,
                                             @NonNull MediaFormat mediaFormat,
                                             @Nullable Surface surface,
                                             boolean isEncoder) throws IllegalStateException {
        mediaCodec.configure(mediaFormat, surface, null, isEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0);
    }

    /**
     * This will iterate over all available codecs that support the mimeType param, and return them as a list of
     * {@link Callable Callable#MediaCodec}, when will create the codec once calling {@link Callable#call()}
     * @param encoder flag indicating if encoder codec is requested
     * @param mimeType the mime type for which to get the codecs that support it
     * @param mediaFormat {@link MediaFormat} if non-null, then only get the codecs that support the mediaFormat
     */
    @NonNull
    private static List<Callable<MediaCodec>> findCodecForFormatOrType(boolean encoder,
                                                                       @NonNull String mimeType,
                                                                       @Nullable MediaFormat mediaFormat) {
        List<Callable<MediaCodec>> supportedMediaCodecs = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo info : mediaCodecList.getCodecInfos()) {
                if (info.isEncoder() != encoder) {
                    continue;
                }
                try {
                    MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mimeType);
                    if (caps != null && (mediaFormat == null || caps.isFormatSupported(mediaFormat))) {
                        supportedMediaCodecs.add(() -> MediaCodec.createByCodecName(info.getName()));
                    }
                } catch (IllegalArgumentException e) {
                    // type is not supported
                }
            }
        } else {
            supportedMediaCodecs.add(() -> encoder ? MediaCodec.createEncoderByType(mimeType) :
                    MediaCodec.createDecoderByType(mimeType));
        }
        return supportedMediaCodecs;
    }

    private static boolean supportsType(@NonNull MediaCodecInfo mediaCodecInfo, @NonNull String mimeType) {
        String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
        for (String supportedType : supportedTypes) {
            if (TextUtils.equals(mimeType, supportedType)) {
                return true;
            }
        }
        return false;
    }

    private static int getProfileRank(@NonNull String mimeType, int profile) {
        if (profile == UNDEFINED_VALUE) {
            return UNDEFINED_VALUE;
        }

        int[] rankedProfiles = CODEC_PROFILE_RANK_MAP.get(mimeType);
        if (rankedProfiles == null) {
            return UNDEFINED_VALUE;
        }

        for (int ranking = 0; ranking < rankedProfiles.length; ranking++) {
            if (profile == rankedProfiles[ranking]) {
                return ranking;
            }
        }
        return UNDEFINED_VALUE;
    }
}
