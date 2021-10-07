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
    VIDEO_OVERLAY_GL(R.string.demo_case_free_transform_video_gl, "FreeTransformVideoGl", new FreeTransformVideoGlFragment()),
    SQUARE_CENTER_CROP(R.string.demo_case_square_center_crop, "SquareCenterCrop", new SquareCenterCropFragment()),
    VIDEO_WATERMARK(R.string.demo_case_video_watermark, "VideoWatermark", new VideoWatermarkFragment()),
    EMPTY_VIDEO(R.string.demo_case_empty_video, "EmptyVideo", new EmptyVideoFragment()),
    MUX_VIDEO_AUDIO(R.string.demo_case_mux_video_audio, "MuxVideoAudio", new MuxVideoAndAudioFragment()),
    VIDEO_FILTERS(R.string.demo_case_video_filters, "VideoFilters", new VideoFiltersFragment()),
    VIDEO_FILTERS_PREVIEW(R.string.demo_case_video_filters_preview, "VideoFiltersPreview", new VideoFilterPreviewFragment()),
    TRANSCODE_VIDEO_MOCK(R.string.demo_case_mock_transcode_video, "TranscodeVideoMock", new MockTranscodeFragment()),
    RECORD_SURFACE(R.string.demo_case_record_surface, "RecordSurface", new RecordSurfaceFragment()),
    RECORD_CAMERA(R.string.demo_case_record_camera, "RecordCamera", new RecordCameraFragment());

    @StringRes int displayName;
    String fragmentTag;
    Fragment fragment;

    DemoCase(@StringRes int displayName, @NonNull String fragmentTag, @NonNull Fragment fragment) {
        this.displayName = displayName;
        this.fragmentTag = fragmentTag;
        this.fragment = fragment;
    }
}
