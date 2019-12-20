/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

public enum DemoCase {
    TRANSCODE_VIDEO_GL(R.string.demo_case_transcode_video_gl, "TranscodeVideoGl", new TranscodeVideoGlFragment()),
    EXTRACT_VIDEO_TRACK(R.string.demo_case_extract_video_track, "ExtractVideoTrack", new ExtractVideoTrackFragment()),
    EXTRACT_AUDIO_TRACK(R.string.demo_case_extract_audio_track, "ExtractAudioTrack", new ExtractAudioTrackFragment());

    @StringRes int displayName;
    String fragmentTag;
    Fragment fragment;

    DemoCase(@StringRes int displayName, @NonNull String fragmentTag, @NonNull Fragment fragment) {
        this.displayName = displayName;
        this.fragmentTag = fragmentTag;
        this.fragment = fragment;
    }
}
