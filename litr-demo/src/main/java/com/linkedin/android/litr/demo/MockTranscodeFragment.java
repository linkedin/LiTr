/*
 * Copyright 2020 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.demo.data.MediaTrackFormat;
import com.linkedin.android.litr.demo.data.MediaTransformationListener;
import com.linkedin.android.litr.demo.data.SourceMedia;
import com.linkedin.android.litr.demo.data.TargetMedia;
import com.linkedin.android.litr.demo.data.TranscodingConfigPresenter;
import com.linkedin.android.litr.demo.data.TransformationPresenter;
import com.linkedin.android.litr.demo.data.TransformationState;
import com.linkedin.android.litr.demo.data.VideoTrackFormat;
import com.linkedin.android.litr.demo.databinding.FragmentMockTranscodeBinding;
import com.linkedin.android.litr.test.MockMediaTransformer;
import com.linkedin.android.litr.test.TransformationEvent;
import com.linkedin.android.litr.utils.TransformationUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MockTranscodeFragment extends BaseTransformationFragment {

    FragmentMockTranscodeBinding binding;

    private MockMediaTransformer mediaTransformer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaTransformer = new MockMediaTransformer(getContext().getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMockTranscodeBinding.inflate(inflater, container, false);

        SourceMedia sourceMedia = new SourceMedia();
        VideoTrackFormat videoTrack = new VideoTrackFormat(0, "video/avc");
        videoTrack.width = 1920;
        videoTrack.height = 1080;
        videoTrack.rotation = 0;
        videoTrack.duration = TimeUnit.SECONDS.toMicros(20);
        videoTrack.frameRate = 30;
        videoTrack.bitrate = 17000000;
        sourceMedia.tracks = Collections.singletonList((MediaTrackFormat) videoTrack);

        binding.setSourceMedia(sourceMedia);

        TransformationState transformationState = new TransformationState();
        transformationState.requestId = UUID.randomUUID().toString();
        binding.setTransformationState(transformationState);
        binding.setTransformationPresenter(new TransformationPresenter(getContext(), mediaTransformer));

        binding.getTransformationState().setState(TransformationState.STATE_IDLE);
        binding.getTransformationState().setStats(null);

        binding.tracks.setLayoutManager(new LinearLayoutManager(getContext()));

        TargetMedia targetMedia = new TargetMedia();
        targetMedia.setTracks(sourceMedia.tracks);
        File targetFile = new File(TransformationUtil.getTargetFileDirectory(requireContext().getApplicationContext()), "transcoded_mock.mp4");
        targetMedia.setTargetFile(targetFile);

        TranscodingConfigPresenter transcodingConfigPresenter = new TranscodingConfigPresenter(this, targetMedia);
        binding.setTranscodingConfigPresenter(transcodingConfigPresenter);
        binding.setTargetMedia(targetMedia);

        MediaTrackAdapter mediaTrackAdapter = new MediaTrackAdapter(binding.getTranscodingConfigPresenter(),
                                                                    sourceMedia,
                                                                    targetMedia);
        binding.tracks.setAdapter(mediaTrackAdapter);
        binding.tracks.setNestedScrollingEnabled(false);

        final MediaTransformationListener mediaTransformationListener = new MediaTransformationListener(getContext(),
                                                                                                        transformationState.requestId,
                                                                                                        transformationState,
                                                                                                        targetMedia);

        binding.buttonTranscode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    @Override
                    public void run() {
                        mediaTransformer.transform(UUID.randomUUID().toString(),
                                                   Collections.<TrackTransform>emptyList(),
                                                   mediaTransformationListener,
                                                   MediaTransformer.GRANULARITY_DEFAULT);
                    }
                }.start();
            }
        });

        setupEventSequence(transformationState.requestId);

        return binding.getRoot();
    }

    private void setupEventSequence(@NonNull String id) {
        List<TransformationEvent> events = new ArrayList<>();
        events.add(new TransformationEvent(id, TransformationEvent.TYPE_START, 0, null));
        events.add(new TransformationEvent(id, TransformationEvent.TYPE_PROGRESS, 0.5f, null));
        events.add(new TransformationEvent(id, TransformationEvent.TYPE_COMPLETED, 1, null));

        mediaTransformer.setEvents(events);
    }

}
