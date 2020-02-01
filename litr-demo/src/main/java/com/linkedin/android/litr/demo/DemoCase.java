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
    TRANSCODE_VIDEO_MOCK(R.string.demo_case_mock_transcode_video, "TranscodeVideoMock", new MockTranscodeFragment());

    @StringRes int displayName;
    String fragmentTag;
    Fragment fragment;

    DemoCase(@StringRes int displayName, @NonNull String fragmentTag, @NonNull Fragment fragment) {
        this.displayName = displayName;
        this.fragmentTag = fragmentTag;
        this.fragment = fragment;
    }
}
