/*
 * Copyright 2022 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr;

import androidx.annotation.Nullable;

public class MimeType {
    private static final String BASE_TYPE_AUDIO = "audio";
    private static final String BASE_TYPE_VIDEO = "video";

    public static final String AUDIO_AAC = BASE_TYPE_AUDIO + "/mp4a-latm";
    public static final String AUDIO_RAW = BASE_TYPE_AUDIO + "/raw";
    public static final String AUDIO_OPUS = BASE_TYPE_AUDIO + "/opus";
    public static final String AUDIO_VORBIS = BASE_TYPE_AUDIO + "/vorbis";

    public static final String VIDEO_AVC = BASE_TYPE_VIDEO + "/avc";
    public static final String VIDEO_HEVC = BASE_TYPE_VIDEO + "/hevc";
    public static final String VIDEO_VP8 = BASE_TYPE_VIDEO + "/x-vnd.on2.vp8";
    public static final String VIDEO_VP9 = BASE_TYPE_VIDEO + "/x-vnd.on2.vp9";
    public static final String VIDEO_RAW = BASE_TYPE_VIDEO + "/raw";

    public static boolean isVideo(@Nullable String mimeType) {
        return BASE_TYPE_VIDEO.equals(getTopLevelType(mimeType));
    }

    public static boolean isAudio(@Nullable String mimeType) {
        return BASE_TYPE_AUDIO.equals(getTopLevelType(mimeType));
    }

    private static String getTopLevelType(@Nullable String mimeType) {
        if (mimeType == null) {
            return null;
        }

        int indexOfSlash = mimeType.indexOf('/');
        if (indexOfSlash == -1) {
            return null;
        }

        return mimeType.substring(0, indexOfSlash);
    }
}
