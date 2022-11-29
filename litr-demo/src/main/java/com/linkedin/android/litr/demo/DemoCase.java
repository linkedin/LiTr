/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.linkedin.android.litr.demo.fragment.EmptyVideoFragment;
import com.linkedin.android.litr.demo.fragment.ExtractFramesFragment;
import com.linkedin.android.litr.demo.fragment.FreeTransformVideoGlFragment;
import com.linkedin.android.litr.demo.fragment.MockTranscodeFragment;
import com.linkedin.android.litr.demo.fragment.MuxVideoAndAudioFragment;
import com.linkedin.android.litr.demo.fragment.NativeMuxerCameraFragment;
import com.linkedin.android.litr.demo.fragment.NativeMuxerTranscodeFragment;
import com.linkedin.android.litr.demo.fragment.RecordAudioFragment;
import com.linkedin.android.litr.demo.fragment.SquareCenterCropFragment;
import com.linkedin.android.litr.demo.fragment.TranscodeAudioFragment;
import com.linkedin.android.litr.demo.fragment.TranscodeToVp9Fragment;
import com.linkedin.android.litr.demo.fragment.TranscodeVideoGlFragment;
import com.linkedin.android.litr.demo.fragment.VideoFilterPreviewFragment;
import com.linkedin.android.litr.demo.fragment.VideoFiltersFragment;
import com.linkedin.android.litr.demo.fragment.VideoWatermarkFragment;

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
    TRANSCODE_AUDIO(R.string.demo_case_transcode_audio, "TranscodeAudio", new TranscodeAudioFragment()),
    EXTRACT_FRAMES(R.string.demo_case_extract_frames, "ExtractFramesFragment", new ExtractFramesFragment()),
    TRANSCODE_TO_VP9(R.string.demo_case_transcode_to_vp9, "TranscodeToVp9Fragment", new TranscodeToVp9Fragment()),
    RECORD_AUDIO(R.string.demo_case_audio_record, "RecordAudio", new RecordAudioFragment()),
    @SuppressLint("NewApi") RECORD_CAMERA(R.string.demo_case_camera_record, "RecordCamera2", new RecordCamera2Fragment()),
    NATIVE_MUXER_TRANSCODE(R.string.demo_case_native_muxer_transcode, "NativeMuxerTranscode", new NativeMuxerTranscodeFragment()),
    @SuppressLint("NewApi") NATIVE_MUXER_CAMERA(R.string.demo_case_native_muxer_camera, "NativeMuxerCamera", new NativeMuxerCameraFragment());

    @StringRes int displayName;
    String fragmentTag;
    Fragment fragment;

    DemoCase(@StringRes int displayName, @NonNull String fragmentTag, @NonNull Fragment fragment) {
        this.displayName = displayName;
        this.fragmentTag = fragmentTag;
        this.fragment = fragment;
    }
}
